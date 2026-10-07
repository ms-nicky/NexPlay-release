package com.nexplay

import org.junit.Assert.assertEquals
import org.junit.Test

class LaunchErrorsSessionRecoveryTest {
    @Test
    fun terminalSessionDoesNotPromiseAnAutomaticReplacementQueue() {
        val message = normalizeLaunchErrorMessage(
            TerminalSessionStatusException(status = 7, latestSession = null),
        )

        assertEquals(
            "The cloud provider ended this session (status 7). " +
                "NexPlay did not stop it or start a replacement queue.",
            message,
        )
    }
}
