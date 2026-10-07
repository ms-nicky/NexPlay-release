package com.nexplay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.pedro.common.AudioCodec
import com.pedro.common.ConnectChecker
import com.pedro.common.VideoCodec
import com.pedro.encoder.input.sources.audio.InternalAudioSource
import com.pedro.encoder.input.sources.audio.MicrophoneSource
import com.pedro.encoder.input.sources.video.NoVideoSource
import com.pedro.encoder.input.sources.video.ScreenSource
import com.pedro.library.generic.GenericStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

data class YouTubeLiveBroadcastState(
    val active: Boolean = false,
    val connected: Boolean = false,
    val status: String = "",
    val message: String = "",
    val startedAtMs: Long = 0L,
)

/**
 * Optional live broadcast of the device screen to an RTMP ingest endpoint, used for YouTube Live.
 *
 * The RTMP URL carries the broadcast stream key, so it is treated as a credential throughout:
 * it never reaches the stream log (only the redacted host does) and never enters a diagnostic
 * export, because every log line passes through [NexPlayHttpDiagnostics]-backed sanitisation on
 * the way to a bug report.
 *
 * Capture runs as a `mediaProjection` foreground service, which Android requires to start before
 * the projection token is consumed.
 */
class YouTubeLiveBroadcastService : Service(), ConnectChecker {

    companion object {
        private const val TAG = "YouTubeLive"
        const val ACTION_START = "com.nexplay.broadcast.START"
        const val ACTION_STOP = "com.nexplay.broadcast.STOP"
        const val EXTRA_RTMP_URL = "com.nexplay.broadcast.RTMP_URL"
        const val EXTRA_WIDTH = "com.nexplay.broadcast.WIDTH"
        const val EXTRA_HEIGHT = "com.nexplay.broadcast.HEIGHT"
        const val EXTRA_FPS = "com.nexplay.broadcast.FPS"
        const val EXTRA_BITRATE = "com.nexplay.broadcast.BITRATE"
        const val EXTRA_ROTATION = "com.nexplay.broadcast.ROTATION"

        private const val CHANNEL_ID = "nexplay_youtube_live"
        private const val NOTIFICATION_ID = 23971

        @Volatile
        var pendingProjection: MediaProjection? = null

        val state = MutableStateFlow(YouTubeLiveBroadcastState())

        fun setState(transform: (YouTubeLiveBroadcastState) -> YouTubeLiveBroadcastState) {
            state.value = transform(state.value)
        }
    }

    private var genericStream: GenericStream? = null
    private var mediaProjection: MediaProjection? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val notificationManager: NotificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    /** Stops capture when the user revokes it from the system UI, which otherwise leaves it running. */
    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            fail(getString(R.string.youtube_live_error_missing_permission))
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startBroadcast(intent)
            ACTION_STOP -> stopBroadcast()
            else -> stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun startBroadcast(intent: Intent) {
        if (genericStream != null) return

        // Android requires a foreground notification before the capture token is consumed, so this
        // runs ahead of any validation that could bail out.
        startForeground(
            NOTIFICATION_ID,
            buildNotification(getString(R.string.youtube_live_notification_connecting)),
        )

        val url = intent.getStringExtra(EXTRA_RTMP_URL).orEmpty()
        if (url.isBlank()) {
            fail(getString(R.string.youtube_live_error_missing_url))
            return
        }
        val projection = pendingProjection
        // Consumed exactly once. Android 14+ invalidates a reused token, and holding it would keep
        // the system screen-recording indicator lit after teardown.
        pendingProjection = null
        if (projection == null) {
            fail(getString(R.string.youtube_live_error_missing_permission))
            return
        }
        mediaProjection = projection
        runCatching { projection.registerCallback(projectionCallback, null) }

        val width = intent.getIntExtra(EXTRA_WIDTH, 1280)
        val height = intent.getIntExtra(EXTRA_HEIGHT, 720)
        val fps = intent.getIntExtra(EXTRA_FPS, 30)
        val bitrate = intent.getIntExtra(EXTRA_BITRATE, 4_500_000)
        val rotation = intent.getIntExtra(EXTRA_ROTATION, 0)

        setState {
            it.copy(active = true, connected = false, status = "connecting", message = "", startedAtMs = 0L)
        }

        scope.launch { setupAndStartStream(url, projection, width, height, fps, bitrate, rotation) }
    }

    private fun setupAndStartStream(
        url: String,
        projection: MediaProjection,
        width: Int,
        height: Int,
        fps: Int,
        bitrate: Int,
        rotation: Int,
    ) {
        val stream = try {
            GenericStream(applicationContext, this@YouTubeLiveBroadcastService, NoVideoSource(), MicrophoneSource()).apply {
                getGlInterface().setForceRender(true, 15)
            }
        } catch (e: Exception) {
            fail(getString(R.string.youtube_live_error_prepare, e.message ?: e.javaClass.simpleName))
            return
        }
        genericStream = stream

        val prepared = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                stream.changeAudioSource(InternalAudioSource(projection))
            }
            val videoReady = stream.prepareVideo(width, height, bitrate, fps, rotation = rotation)
            val audioReady = stream.prepareAudio(32000, true, 128 * 1024, echoCanceler = false, noiseSuppressor = false)
            videoReady && audioReady
        } catch (e: Exception) {
            fail(getString(R.string.youtube_live_error_prepare, e.message ?: e.javaClass.simpleName))
            return
        }
        if (!prepared) {
            fail(getString(R.string.youtube_live_error_unsupported))
            return
        }

        try {
            stream.changeVideoSource(ScreenSource(applicationContext, projection))
            stream.setVideoCodec(VideoCodec.H264)
            stream.setAudioCodec(AudioCodec.AAC)
            // Log the ingest host only. The URL's last path segment is the stream key, and these
            // lines are attached to bug reports.
            // The URL's last path segment is the stream key, so log the ingest host only. Debug
            // output here is picked up by the diagnostic export.
            Log.d(TAG, "Starting RTMP stream to ${redactRtmpStreamKey(url)}")
            stream.startStream(url)
        } catch (e: Exception) {
            fail(getString(R.string.youtube_live_error_start, e.message ?: e.javaClass.simpleName))
        }
    }

    private fun fail(message: String) {
        setState {
            it.copy(active = false, connected = false, status = "failed", message = message, startedAtMs = 0L)
        }
        scope.launch {
            stopStreamInternal()
            stopSelf()
        }
    }

    private fun stopBroadcast() {
        setState {
            it.copy(active = false, connected = false, status = "stopped", message = "", startedAtMs = 0L)
        }
        scope.launch {
            stopStreamInternal()
            stopSelf()
        }
    }

    private fun stopStreamInternal() {
        runCatching { genericStream?.stopStream() }
        runCatching { genericStream?.release() }
        genericStream = null
        runCatching { mediaProjection?.unregisterCallback(projectionCallback) }
        runCatching { mediaProjection?.stop() }
        mediaProjection = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        runCatching { genericStream?.stopStream() }
        runCatching { genericStream?.release() }
        genericStream = null
        runCatching { mediaProjection?.unregisterCallback(projectionCallback) }
        runCatching { mediaProjection?.stop() }
        mediaProjection = null
        super.onDestroy()
    }

    override fun onConnectionStarted(url: String) {
        setState { it.copy(active = true, connected = false, status = "connecting", message = "") }
        notificationManager.notify(
            NOTIFICATION_ID,
            buildNotification(getString(R.string.youtube_live_notification_connecting)),
        )
    }

    override fun onConnectionSuccess() {
        setState {
            it.copy(
                active = true,
                connected = true,
                status = "live",
                message = "",
                startedAtMs = System.currentTimeMillis(),
            )
        }
        notificationManager.notify(
            NOTIFICATION_ID,
            buildNotification(getString(R.string.youtube_live_notification_live)),
        )
    }

    override fun onConnectionFailed(reason: String) {
        fail(reason.ifBlank { getString(R.string.youtube_live_failed_connection) })
    }

    override fun onDisconnect() {
        if (state.value.active) {
            setState { it.copy(active = false, connected = false, status = "ended", startedAtMs = 0L) }
            scope.launch {
                stopStreamInternal()
                stopSelf()
            }
        }
    }

    override fun onAuthError() {
        fail(getString(R.string.youtube_live_auth_error))
    }

    override fun onAuthSuccess() = Unit

    override fun onNewBitrate(bitrate: Long) = Unit

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.youtube_live_notification_channel),
                NotificationManager.IMPORTANCE_LOW,
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        val stopIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, YouTubeLiveBroadcastService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_go_live)
            .setContentTitle(getString(R.string.youtube_live_notification_title))
            .setContentText(text)
            .setOngoing(true)
            .setSilent(true)
            .addAction(
                R.drawable.ic_stat_go_live,
                getString(R.string.youtube_live_notification_stop),
                stopIntent,
            )
            .build()
    }
}
