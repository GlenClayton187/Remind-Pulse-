package com.example.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.model.TaskReminder
import com.example.receiver.AlarmReceiver

object AlarmScheduler {

    private const val TAG = "AlarmScheduler"
    const val ACTION_REMINDER_ALARM = "com.example.remindpulse.ACTION_REMINDER_ALARM"

    fun scheduleTaskReminder(context: Context, task: TaskReminder) {
        if (!task.isNotificationEnabled || task.isCompleted) {
            cancelTaskReminder(context, task)
            return
        }

        val triggerTime = task.activeAlarmTime
        // If trigger time is in the past by more than 1 minute, don't schedule old alarm
        if (triggerTime <= System.currentTimeMillis() - 60_000) {
            Log.d(TAG, "Task ${task.id} is in the past, skipping alarm schedule")
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_REMINDER_ALARM
            putExtra(NotificationHelper.EXTRA_TASK_ID, task.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
            Log.d(TAG, "Scheduled alarm for task ${task.id} at $triggerTime")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while scheduling alarm: ${e.message}")
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } catch (err: Exception) {
                Log.e(TAG, "Fallback alarm failed: ${err.message}")
            }
        }
    }

    fun cancelTaskReminder(context: Context, task: TaskReminder) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_REMINDER_ALARM
            putExtra(NotificationHelper.EXTRA_TASK_ID, task.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.notificationId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}
