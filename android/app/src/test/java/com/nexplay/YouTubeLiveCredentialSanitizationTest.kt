package com.nexplay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the two places a YouTube broadcast key could leak: the stream key stored in settings, and
 * the RTMP URL that embeds it. Both feed the diagnostic export and the bug report, so redaction has
 * to hold even though the key never appears under an obviously sensitive name.
 */
class YouTubeLiveCredentialSanitizationTest {
    @Test
    fun redactsStreamKeyFromRtmpUrl() {
        val sanitized = sanitizeDiagnosticExport(
            "Starting RTMP stream to rtmp://a.rtmp.youtube.com/live2/AB0cDE1fGhIjKlMnOpQr",
        )

        assertFalse(sanitized.contains("AB0cDE1fGhIjKlMnOpQr"))
        assertTrue(sanitized.contains("rtmp://a.rtmp.youtube.com/live2/[redacted]"))
    }

    @Test
    fun redactsStreamKeyWhenNamedInSettings() {
        val sanitized = sanitizeDiagnosticExport("streamKey=AB0cDE1fGhIjKlMnOpQr")

        assertFalse(sanitized.contains("AB0cDE1fGhIjKlMnOpQr"))
        assertTrue(sanitized.contains("[redacted]"))
    }

    @Test
    fun redactsCamelCaseStreamKeyProperty() {
        val sanitized = sanitizeDiagnosticExport("stream_key: AB0cDE1fGhIjKlMnOpQr")

        assertFalse(sanitized.contains("AB0cDE1fGhIjKlMnOpQr"))
    }

    @Test
    fun keepsTheIngestHostVisibleForTroubleshooting() {
        val redacted = redactRtmpStreamKey("rtmp://a.rtmp.youtube.com/live2/AB0cDE1fGhIjKlMnOpQr")

        assertEquals("rtmp://a.rtmp.youtube.com/live2/[redacted]", redacted)
    }

    @Test
    fun leavesAnRtmpUrlWithoutAKeyIntact() {
        val url = "rtmp://a.rtmp.youtube.com/live2/"

        assertEquals(url, redactRtmpStreamKey(url))
    }

    @Test
    fun analyticsNeverCarriesTheBroadcastKeyOrRtmpUrl() {
        val properties = sanitizedAnalyticsProperties(
            mapOf(
                "youtubeLiveStreamKey" to "AB0cDE1fGhIjKlMnOpQr",
                "youtubeLiveRtmpUrl" to "rtmp://a.rtmp.youtube.com/live2/AB0cDE1fGhIjKlMnOpQr",
                "game_id" to "1234",
            ),
        )

        assertFalse(properties.containsKey("youtubeLiveStreamKey"))
        assertFalse(properties.containsKey("youtubeLiveRtmpUrl"))
        assertEquals("1234", properties["game_id"])
    }
}
