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

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        StreamFileLogger.initialize(applicationContext)
        StreamFileLogger.log(TAG, "Service created")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        StreamFileLogger.log(TAG, "onStartCommand action=${intent?.action}")
        when (intent?.action) {
            ACTION_START -> startBroadcast(intent)
            ACTION_STOP -> stopBroadcast()
            else -> stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun startBroadcast(intent: Intent) {
        StreamFileLogger.log(TAG, "startBroadcast called")
        if (genericStream != null) {
            StreamFileLogger.log(TAG, "Stream already active, ignoring start request")
            return
        }
        startForeground(
            NOTIFICATION_ID,
            buildNotification(getString(R.string.youtube_live_notification_connecting)),
        )
        val url = intent.getStringExtra(EXTRA_RTMP_URL).orEmpty()
        if (url.isBlank()) {
            StreamFileLogger.logError(TAG, "RTMP URL is blank")
            fail(getString(R.string.youtube_live_error_missing_url))
            return
        }
        val projection = pendingProjection
        pendingProjection = null
        if (projection == null) {
            StreamFileLogger.logError(TAG, "MediaProjection is null")
            fail(getString(R.string.youtube_live_error_missing_permission))
            return
        }
        mediaProjection = projection

        val width = intent.getIntExtra(EXTRA_WIDTH, 1280)
        val height = intent.getIntExtra(EXTRA_HEIGHT, 720)
        val fps = intent.getIntExtra(EXTRA_FPS, 30)
        val bitrate = intent.getIntExtra(EXTRA_BITRATE, 4_500_000)
        val rotation = intent.getIntExtra(EXTRA_ROTATION, 0)
        StreamFileLogger.log(TAG, "Stream params: ${width}x${height} fps=$fps bitrate=$bitrate rotation=$rotation")

        setState { it.copy(active = true, connected = false, status = "connecting", message = "", startedAtMs = 0L) }

        scope.launch {
            setupAndStartStream(url, projection, width, height, fps, bitrate, rotation)
        }
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
            StreamFileLogger.logError(TAG, "Failed to create GenericStream", e)
            fail(getString(R.string.youtube_live_error_prepare, e.message ?: e.javaClass.simpleName))
            return
        }
        genericStream = stream

        val prepared = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                StreamFileLogger.log(TAG, "Setting up InternalAudioSource")
                stream.changeAudioSource(InternalAudioSource(projection))
            }
            StreamFileLogger.log(TAG, "Preparing video encoder...")
            val videoReady = stream.prepareVideo(width, height, bitrate, fps, rotation = rotation)
            StreamFileLogger.log(TAG, "Video encoder ready=$videoReady")
            StreamFileLogger.log(TAG, "Preparing audio encoder...")
            val audioReady = stream.prepareAudio(32000, true, 128 * 1024, echoCanceler = false, noiseSuppressor = false)
            StreamFileLogger.log(TAG, "Audio encoder ready=$audioReady")
            videoReady && audioReady
        } catch (e: Exception) {
            StreamFileLogger.logError(TAG, "Failed to prepare encoders", e)
            fail(getString(R.string.youtube_live_error_prepare, e.message ?: e.javaClass.simpleName))
            return
        }
        if (!prepared) {
            StreamFileLogger.logError(TAG, "Encoders not supported")
            fail(getString(R.string.youtube_live_error_unsupported))
            return
        }

        try {
            StreamFileLogger.log(TAG, "Setting up ScreenSource")
            stream.changeVideoSource(ScreenSource(applicationContext, projection))
            stream.setVideoCodec(VideoCodec.H264)
            stream.setAudioCodec(AudioCodec.AAC)
            StreamFileLogger.log(TAG, "Starting RTMP stream to $url")
            stream.startStream(url)
        } catch (e: Exception) {
            StreamFileLogger.logError(TAG, "Failed to start broadcast", e)
            fail(getString(R.string.youtube_live_error_start, e.message ?: e.javaClass.simpleName))
        }
    }

    private fun fail(message: String) {
        StreamFileLogger.logError(TAG, "Broadcast failed: $message")
        setState {
            it.copy(active = false, connected = false, status = "failed", message = message, startedAtMs = 0L)
        }
        scope.launch {
            stopStreamInternal()
            stopSelf()
        }
    }

    private fun stopBroadcast() {
        StreamFileLogger.log(TAG, "stopBroadcast called")
        setState {
            it.copy(active = false, connected = false, status = "stopped", message = "", startedAtMs = 0L)
        }
        scope.launch {
            stopStreamInternal()
            StreamFileLogger.flushToFile(applicationContext)
            stopSelf()
        }
    }

    private fun stopStreamInternal() {
        StreamFileLogger.log(TAG, "stopStreamInternal: cleaning up")
        runCatching { genericStream?.stopStream() }
        runCatching { genericStream?.release() }
        genericStream = null
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
        StreamFileLogger.log(TAG, "Service destroyed")
        StreamFileLogger.flushToFile(applicationContext)
        scope.cancel()
        runCatching { genericStream?.stopStream() }
        runCatching { genericStream?.release() }
        genericStream = null
        runCatching { mediaProjection?.stop() }
        mediaProjection = null
        super.onDestroy()
    }

    override fun onConnectionStarted(url: String) {
        StreamFileLogger.log(TAG, "onConnectionStarted")
        setState { it.copy(active = true, connected = false, status = "connecting", message = "") }
        notificationManager.notify(
            NOTIFICATION_ID,
            buildNotification(getString(R.string.youtube_live_notification_connecting)),
        )
    }

    override fun onConnectionSuccess() {
        StreamFileLogger.log(TAG, "onConnectionSuccess - LIVE")
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
        StreamFileLogger.logError(TAG, "onConnectionFailed: $reason")
        fail(reason.ifBlank { getString(R.string.youtube_live_failed_connection) })
    }

    override fun onDisconnect() {
        StreamFileLogger.log(TAG, "onDisconnect")
        if (state.value.active) {
            setState {
                it.copy(active = false, connected = false, status = "ended", startedAtMs = 0L)
            }
            scope.launch {
                stopStreamInternal()
                stopSelf()
            }
        }
    }

    override fun onAuthError() {
        StreamFileLogger.logError(TAG, "onAuthError - stream key rejected")
        fail(getString(R.string.youtube_live_auth_error))
    }

    override fun onAuthSuccess() {
        StreamFileLogger.log(TAG, "onAuthSuccess")
    }

    override fun onNewBitrate(bitrate: Long) {
        StreamFileLogger.log(TAG, "onNewBitrate: $bitrate bps")
    }

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
