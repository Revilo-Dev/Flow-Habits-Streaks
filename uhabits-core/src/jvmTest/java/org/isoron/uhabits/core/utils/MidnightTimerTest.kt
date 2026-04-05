package org.isoron.uhabits.core.utils

import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.isoron.uhabits.core.BaseUnitTest
import org.isoron.uhabits.core.io.StandardLogging
import org.isoron.uhabits.core.preferences.Preferences
import org.junit.After
import org.junit.Test
import org.mockito.Mockito.mock
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.test.assertEquals

class MidnightTimerTest : BaseUnitTest() {

    @After
    override fun tearDown() {
        super.tearDown()
        DateUtils.setFixedLocalTime(null)
        DateUtils.setFixedTimeZone(null)
    }

    @Test
    fun testMidnightTimer_notifyListener_atMidnight() = runBlocking {
        // Given
        val executor = Executors.newSingleThreadScheduledExecutor()
        val dispatcher = executor.asCoroutineDispatcher()

        withContext(dispatcher) {
            DateUtils.setFixedTimeZone(TimeZone.getTimeZone("GMT"))
            DateUtils.setFixedLocalTime(
                unixTime(
                    2017,
                    Calendar.JANUARY,
                    1,
                    23,
                    59,
                    DateUtils.MINUTE_LENGTH - 1
                )
            )

            val suspendedListener = suspendCoroutine<Boolean> { continuation ->
                MidnightTimer(StandardLogging(), mock(Preferences::class.java)).apply {
                    addListener { continuation.resume(true) }
                    // When
                    onResume(1, executor)
                }
            }

            // Then
            assertEquals(true, suspendedListener)
        }
    }
}
