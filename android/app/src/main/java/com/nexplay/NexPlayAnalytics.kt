package com.nexplay

import android.app.Application
import android.util.Log
import com.posthog.PostHog
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig

private const val ANALYTICS_LOG_TAG = "NexPlayAnalytics"
private const val ANALYTICS_FLUSH_INTERVAL_SECONDS = 10

/**
 * Optional, opt-in usage analytics.
 *
 * Three properties this wrapper exists to guarantee:
 *  - Nothing is sent until the user answers the consent prompt. [applyNexPlaySettings] sets
 *    PostHog's own `optOut`, so an unanswered prompt leaves the SDK inert rather than relying on
 *    the absence of calls.
 *  - Every payload passes [sanitizedAnalyticsProperties] on the way out, including properties the
 *    SDK adds itself via `beforeSend`.
 *  - An empty project token disables the SDK entirely, so a build without telemetry configured
 *    does no network work at all.
 */
internal object NexPlayAnalytics {
    fun setup(application: Application, settings: AppSettings) {
        val token = BuildConfig.POSTHOG_PROJECT_TOKEN.trim()
        if (token.isEmpty()) {
            Log.w(ANALYTICS_LOG_TAG, "PostHog disabled because no project token is configured.")
            return
        }

        val config = PostHogAndroidConfig(
            apiKey = token,
            host = BuildConfig.POSTHOG_HOST,
        ).apply { applyNexPlaySettings(settings) }

        runCatching {
            PostHogAndroid.setup(application, config)
            applyOptOut(!settings.analyticsSharingEnabled)
        }.onFailure { error ->
            Log.w(ANALYTICS_LOG_TAG, "PostHog setup failed.", error)
        }
    }

    fun applyOptOut(optedOut: Boolean) {
        runPostHogOperation("opt-out update") {
            if (optedOut) {
                PostHog.optOut()
            } else {
                PostHog.optIn()
            }
        }
    }

    fun capture(event: String, properties: Map<String, Any>? = null) {
        runPostHogOperation("capture") {
            PostHog.capture(
                event = event,
                properties = sanitizedAnalyticsProperties(properties),
            )
            if (!BuildConfig.DEBUG) PostHog.flush()
        }
    }

    fun reset() {
        runPostHogOperation("reset") {
            val optedOut = PostHog.isOptOut()
            PostHog.reset()
            applyOptOut(optedOut)
        }
    }

    private inline fun runPostHogOperation(operation: String, block: () -> Unit) {
        runCatching(block).onFailure { error ->
            Log.w(ANALYTICS_LOG_TAG, "PostHog $operation failed.", error)
        }
    }
}

internal fun PostHogAndroidConfig.applyNexPlaySettings(settings: AppSettings) {
    optOut = !settings.analyticsSharingEnabled
    captureApplicationLifecycleEvents = true
    captureDeepLinks = false
    captureScreenViews = true
    flushIntervalSeconds = ANALYTICS_FLUSH_INTERVAL_SECONDS
    // Screen replay would capture gameplay and any on-screen account details. Off, and masked even
    // if a future SDK default flips it.
    sessionReplay = false
    sessionReplayConfig.apply {
        maskAllTextInputs = true
        maskAllImages = true
        screenshot = false
        captureLogcat = false
    }
    errorTrackingConfig.autoCapture = true
    addBeforeSend { event ->
        event.copy(
            properties = sanitizedAnalyticsProperties(
                properties = event.properties,
                redactExceptionText = event.event == "\$exception",
            ).toMutableMap(),
        )
    }
}

internal fun sanitizedAnalyticsProperties(
    properties: Map<String, Any>?,
    redactExceptionText: Boolean = false,
): Map<String, Any> =
    buildMap {
        // Keeps the ingest server from deriving an approximate location from the source IP.
        put("\$geoip_disable", true)
        properties.orEmpty().forEach { (key, value) ->
            if (!isSensitiveAnalyticsProperty(key, redactExceptionText)) {
                put(key, sanitizeAnalyticsValue(value, redactExceptionText))
            }
        }
    }

private fun sanitizeAnalyticsValue(value: Any, redactExceptionText: Boolean): Any =
    when (value) {
        is String -> sanitizeDiagnosticExport(value).take(ANALYTICS_STRING_LIMIT)
        is Map<*, *> -> value.entries
            .mapNotNull { (key, nestedValue) ->
                val stringKey = key as? String ?: return@mapNotNull null
                val presentValue = nestedValue ?: return@mapNotNull null
                stringKey.takeUnless { isSensitiveAnalyticsProperty(it, redactExceptionText) }
                    ?.let { it to sanitizeAnalyticsValue(presentValue, redactExceptionText) }
            }
            .toMap()
        is Iterable<*> -> value.mapNotNull { it?.let { item -> sanitizeAnalyticsValue(item, redactExceptionText) } }
        is Array<*> -> value.mapNotNull { it?.let { item -> sanitizeAnalyticsValue(item, redactExceptionText) } }
        else -> value
    }

/** Longest free-text value kept in an event, so a pasted token cannot ride along in a field. */
private const val ANALYTICS_STRING_LIMIT = 500

private fun isSensitiveAnalyticsProperty(key: String, redactExceptionText: Boolean): Boolean {
    val normalized = key.lowercase().filter(Char::isLetterOrDigit)
    return (redactExceptionText && normalized in setOf("message", "value", "exceptionmessage")) || normalized in setOf(
        "authorization",
        "cookie",
        "credential",
        "displayname",
        "email",
        "errormessage",
        "password",
        "query",
        "searchquery",
        "secret",
        // The YouTube broadcast key, and the RTMP URL that embeds it.
        "streamkey",
        "youtubelivertmpurl",
        "youtubelivestreamkey",
        "token",
        "userid",
        "username",
    ) || normalized.endsWith("token") || normalized.endsWith("credential")
}
