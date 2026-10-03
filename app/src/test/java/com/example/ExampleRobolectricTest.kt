package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.AlertTone
import com.example.model.Priority
import com.example.model.RecurrenceType
import com.example.model.TaskReminder
import com.example.util.RecurrenceHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("RemindPulse", appName)
    }

    @Test
    fun `verify alert tones channel IDs`() {
        for (tone in AlertTone.entries) {
            assertTrue(tone.channelId.startsWith("channel_remindpulse_"))
            assertNotNull(tone.title)
        }
        assertEquals(AlertTone.URGENT_ALARM, AlertTone.fromId("urgent_alarm"))
    }

    @Test
    fun `verify daily recurrence calculates tomorrow`() {
        val now = System.currentTimeMillis()
        val nextDue = RecurrenceHelper.calculateNextDueDate(
            currentDueDate = now,
            recurrenceType = RecurrenceType.DAILY,
            fromTime = now
        )
        assertNotNull(nextDue)
        assertTrue(nextDue!! > now)
        // Difference should be ~24 hours
        val diffHours = (nextDue - now) / (1000 * 60 * 60)
        assertTrue(diffHours in 23..25)
    }

    @Test
    fun `verify task overdue status`() {
        val pastTime = System.currentTimeMillis() - 100000L
        val task = TaskReminder(
            id = 1L,
            title = "Overdue Task",
            dueDate = pastTime,
            priority = Priority.URGENT
        )
        assertTrue(task.isOverdue)
    }
}
