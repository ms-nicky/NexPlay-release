package com.nexplay

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The analytics wrapper only sends when the user has answered the consent prompt and has not opted
 * out. These cases pin the gate so a future default flip cannot quietly start reporting.
 */
class NexPlayAnalyticsTest {
    @Test
    fun telemetryIsOffUntilConsentIsAnswered() {
        val settings = AppSettings(analyticsConsentAsked = false, analyticsOptOut = false)

        assertFalse(settings.analyticsSharingEnabled)
    }

    @Test
    fun telemetryIsOffWhenTheUserOptsOut() {
        val settings = AppSettings(analyticsConsentAsked = true, analyticsOptOut = true)

        assertFalse(settings.analyticsSharingEnabled)
    }

    @Test
    fun telemetryIsOnOnlyAfterAnExplicitOptIn() {
        val settings = AppSettings(analyticsConsentAsked = true, analyticsOptOut = false)

        assertTrue(settings.analyticsSharingEnabled)
    }

    @Test
    fun sensitivePropertyNamesAreDroppedFromAnalyticsPayloads() {
        val properties = sanitizedAnalyticsProperties(
            mapOf(
                "email" to "player@example.com",
                "accessToken" to "secret-token",
                "userId" to "abc123",
                "authorization" to "Bearer abc",
                "game_id" to "42",
            ),
        )

        assertFalse(properties.containsKey("email"))
        assertFalse(properties.containsKey("accessToken"))
        assertFalse(properties.containsKey("userId"))
        assertFalse(properties.containsKey("authorization"))
        assertTrue(properties.containsKey("game_id"))
    }

    @Test
    fun exceptionTextIsRedactedOnlyForExceptionEvents() {
        val redacted = sanitizedAnalyticsProperties(
            mapOf("message" to "user@example.com"),
            redactExceptionText = true,
        )
        val kept = sanitizedAnalyticsProperties(
            mapOf("message" to "user@example.com"),
            redactExceptionText = false,
        )

        assertFalse(redacted.containsKey("message"))
        assertTrue(kept.containsKey("message"))
    }

    @Test
    fun geolocationIsDisabledOnTheServer() {
        val properties = sanitizedAnalyticsProperties(emptyMap())

        assertTrue(properties["\$geoip_disable"] == true)
    }
}
