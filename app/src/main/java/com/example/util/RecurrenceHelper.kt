package com.example.util

import com.example.model.RecurrenceType
import java.util.Calendar

object RecurrenceHelper {

    fun calculateNextDueDate(
        currentDueDate: Long,
        recurrenceType: RecurrenceType,
        recurrenceDays: String = "",
        fromTime: Long = System.currentTimeMillis()
    ): Long? {
        if (recurrenceType == RecurrenceType.NONE) return null

        val cal = Calendar.getInstance().apply {
            timeInMillis = currentDueDate
        }
        val targetHour = cal.get(Calendar.HOUR_OF_DAY)
        val targetMinute = cal.get(Calendar.MINUTE)
        val targetSecond = cal.get(Calendar.SECOND)

        // Base time from which we calculate next
        val nowCal = Calendar.getInstance().apply {
            timeInMillis = maxOf(currentDueDate, fromTime)
        }

        when (recurrenceType) {
            RecurrenceType.NONE -> return null

            RecurrenceType.DAILY -> {
                nowCal.add(Calendar.DAY_OF_YEAR, 1)
                nowCal.set(Calendar.HOUR_OF_DAY, targetHour)
                nowCal.set(Calendar.MINUTE, targetMinute)
                nowCal.set(Calendar.SECOND, targetSecond)
                nowCal.set(Calendar.MILLISECOND, 0)
                return nowCal.timeInMillis
            }

            RecurrenceType.WEEKDAYS -> {
                do {
                    nowCal.add(Calendar.DAY_OF_YEAR, 1)
                } while (nowCal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ||
                    nowCal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
                )
                nowCal.set(Calendar.HOUR_OF_DAY, targetHour)
                nowCal.set(Calendar.MINUTE, targetMinute)
                nowCal.set(Calendar.SECOND, targetSecond)
                nowCal.set(Calendar.MILLISECOND, 0)
                return nowCal.timeInMillis
            }

            RecurrenceType.WEEKLY -> {
                nowCal.add(Calendar.WEEK_OF_YEAR, 1)
                nowCal.set(Calendar.HOUR_OF_DAY, targetHour)
                nowCal.set(Calendar.MINUTE, targetMinute)
                nowCal.set(Calendar.SECOND, targetSecond)
                nowCal.set(Calendar.MILLISECOND, 0)
                return nowCal.timeInMillis
            }

            RecurrenceType.MONTHLY -> {
                nowCal.add(Calendar.MONTH, 1)
                nowCal.set(Calendar.HOUR_OF_DAY, targetHour)
                nowCal.set(Calendar.MINUTE, targetMinute)
                nowCal.set(Calendar.SECOND, targetSecond)
                nowCal.set(Calendar.MILLISECOND, 0)
                return nowCal.timeInMillis
            }
        }
    }
}
