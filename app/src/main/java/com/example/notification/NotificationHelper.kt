package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.model.AlertTone
import com.example.model.TaskReminder
import com.example.receiver.NotificationActionReceiver

object NotificationHelper {

    const val EXTRA_TASK_ID = "extra_task_id"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    const val ACTION_MARK_COMPLETE = "com.example.remindpulse.ACTION_MARK_COMPLETE"
    const val ACTION_SNOOZE = "com.example.remindpulse.ACTION_SNOOZE"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create a channel for each AlertTone
        for (tone in AlertTone.entries) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(tone.channelId, "Reminders: ${tone.title}", importance).apply {
                description = tone.description
                enableVibration(true)
                enableLights(true)
                lightColor = 0xFF6366F1.toInt()

                // Vibration pattern
                when (tone) {
                    AlertTone.URGENT_ALARM -> vibrationPattern = longArrayOf(0, 400, 150, 400, 150, 600)
                    AlertTone.GENTLE_CHIME -> vibrationPattern = longArrayOf(0, 200, 200, 200)
                    AlertTone.CRYSTAL_BELL -> vibrationPattern = longArrayOf(0, 150, 100, 150)
                    AlertTone.DIGITAL_PULSE -> vibrationPattern = longArrayOf(0, 100, 80, 100, 80, 100)
                    AlertTone.COSMIC_HARP -> vibrationPattern = longArrayOf(0, 300, 200, 300)
                    AlertTone.SYSTEM_DEFAULT -> vibrationPattern = longArrayOf(0, 250, 250, 250)
                }

                // Custom sound if raw resource exists
                if (tone.rawResId != null) {
                    val soundUri = Uri.parse(
                        "${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/${tone.rawResId}"
                    )
                    val audioAttributes = AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .build()
                    setSound(soundUri, audioAttributes)
                }
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun sendReminderNotification(context: Context, task: TaskReminder) {
        val notificationManager = NotificationManagerCompat.from(context)

        // Open MainActivity when tapping notification
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TASK_ID, task.id)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            task.notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Mark Complete action
        val completeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_MARK_COMPLETE
            putExtra(EXTRA_TASK_ID, task.id)
            putExtra(EXTRA_NOTIFICATION_ID, task.notificationId)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            task.notificationId * 10 + 1,
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze 15 minutes action
        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_TASK_ID, task.id)
            putExtra(EXTRA_NOTIFICATION_ID, task.notificationId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            task.notificationId * 10 + 2,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = task.alertTone.channelId
        val priorityText = task.priority.displayName
        val categoryEmoji = task.category.emoji
        val subtitle = if (task.notes.isNotBlank()) task.notes else "Priority: $priorityText • Tone: ${task.alertTone.title}"

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_icon)
            .setContentTitle("$categoryEmoji ${task.title}")
            .setContentText(subtitle)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("${task.title}\n${if (task.notes.isNotBlank()) "${task.notes}\n" else ""}Priority: $priorityText\nCustom Tone: ${task.alertTone.title}${if (task.recurrenceType != com.example.model.RecurrenceType.NONE) "\nRepeats: ${task.recurrenceType.displayName}" else ""}")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .addAction(android.R.drawable.checkbox_on_background, "Mark Complete", completePendingIntent)
            .addAction(android.R.drawable.ic_popup_sync, "Snooze 15m", snoozePendingIntent)

        try {
            notificationManager.notify(task.notificationId, builder.build())
        } catch (_: SecurityException) {
            // Notification permission might not be granted
        }
    }

    fun sendTestNotification(
        context: Context,
        tone: AlertTone,
        title: String = "Test Reminder Notification",
        message: String = "Custom alert tone: "
    ) {
        val notificationManager = NotificationManagerCompat.from(context)
        val notificationId = 99999

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, tone.channelId)
            .setSmallIcon(R.drawable.ic_launcher_icon)
            .setContentTitle("🔔 $title")
            .setContentText("$message ${tone.title}")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$message ${tone.title}\n${tone.description}\nPush notifications and alert channels are fully active!")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            notificationManager.notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // Notification permission not granted
        }
    }

    fun cancelNotification(context: Context, notificationId: Int) {
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(notificationId)
    }
}
