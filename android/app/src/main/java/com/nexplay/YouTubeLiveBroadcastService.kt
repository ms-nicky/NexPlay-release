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
import androidx.core.app.NotificationCompat
import com.pedro.common.AudioCodec
import com.pedro.common.ConnectChecker
import com.pedro.common.VideoCodec
import com.pedro.encoder.input.sources.audio.InternalAudioSource
import com.pedro.encoder.input.sources.audio.MicrophoneSource
import com.pedro.encoder.input.sources.video.NoVideoSource
import com.pedro.encoder.input.sources.video.ScreenSource
import com.pedro.library.generic.GenericStream
import kotlinx.coroutines.flow.MutableStateFlow

data class YouTubeLiveBroadcastState(
    val active: Boolean = false,
    val connected: Boolean = false,
    val status: String = "",
    val message: String = "",
    val startedAtMs: Long = 0L,
)

class YouTubeLiveBroadcastService : Service(), ConnectChecker {

    companion object {
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

        /**
         * Primed by the ViewModel with a fresh MediaProjection while the app is foreground,
         * right before the service is started. Consumed once by [YouTubeLiveBroadcastService]
         * to satisfy the Android 14 mediaProjection foreground-service gate without ever
         * calling getMediaProjection() twice.
         */
        @Volatile
        var pendingProjection: MediaProjection? = null

        val state = MutableStateFlow(YouTubeLiveBroadcastState())

        fun setState(transform: (YouTubeLiveBroadcastState) -> YouTubeLiveBroadcastState) {
            state.value = transform(state.value)
        }
    }

    private var genericStream: GenericStream? = null
    private var mediaProjection: MediaProjection? = null
    private val notificationManager: NotificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
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
        // Must be called promptly after startForegroundService().
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
        pendingProjection = null
        if (projection == null) {
            fail(getString(R.string.youtube_live_error_missing_permission))
            return
        }
        mediaProjection = projection

        val width = intent.getIntExtra(EXTRA_WIDTH, 1280)
        val height = intent.getIntExtra(EXTRA_HEIGHT, 720)
        val fps = intent.getIntExtra(EXTRA_FPS, 30)
        val bitrate = intent.getIntExtra(EXTRA_BITRATE, 4_500_000)
        val rotation = intent.getIntExtra(EXTRA_ROTATION, 0)

        val stream = GenericStream(applicationContext, this, NoVideoSource(), MicrophoneSource()).apply {
            // MediaProjection only produces frames when the screen changes, so force a constant fps.
            getGlInterface().setForceRender(true, 15)
        }
        genericStream = stream

        val prepared = try {
            // API 29+: capture the app's own audio (the GFN game sound) via AudioPlaybackCapture,
            // which needs no microphone permission. Older devices fall back to the microphone.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                stream.changeAudioSource(InternalAudioSource(projection))
            }
            stream.prepareVideo(width, height, bitrate, fps, rotation = rotation) &&
                stream.prepareAudio(32000, true, 128 * 1024, echoCanceler = false, noiseSuppressor = false)
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
            setState { it.copy(active = true, connected = false, status = "connecting", message = "", startedAtMs = 0L) }
            stream.startStream(url)
        } catch (e: Exception) {
            fail(getString(R.string.youtube_live_error_start, e.message ?: e.javaClass.simpleName))
        }
    }

    private fun fail(message: String) {
        setState {
            it.copy(active = false, connected = false, status = "failed", message = message, startedAtMs = 0L)
        }
        stopStreamInternal()
        stopSelf()
    }

    private fun stopBroadcast() {
        setState {
            it.copy(active = false, connected = false, status = "stopped", message = "", startedAtMs = 0L)
        }
        stopStreamInternal()
        stopSelf()
    }

    private fun stopStreamInternal() {
        runCatching { genericStream?.stopStream() }
        genericStream?.release()
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
        stopStreamInternal()
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
            setState {
                it.copy(active = false, connected = false, status = "ended", startedAtMs = 0L)
            }
            stopStreamInternal()
            stopSelf()
        }
    }

    override fun onAuthError() {
        fail(getString(R.string.youtube_live_auth_error))
    }

    override fun onAuthSuccess() {
        // Nothing to do; onConnectionSuccess follows.
    }

    override fun onNewBitrate(bitrate: Long) {
        // Optional future use: surface the live bitrate in the overlay.
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
