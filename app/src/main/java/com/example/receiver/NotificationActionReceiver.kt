package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.RemindPulseApp
import com.example.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(NotificationHelper.EXTRA_TASK_ID, -1L)
        val notificationId = intent.getIntExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, -1)
        val action = intent.action

        Log.d("NotificationActionReceiver", "Action $action received for taskId=$taskId, notificationId=$notificationId")

        if (notificationId != -1) {
            NotificationHelper.cancelNotification(context, notificationId)
        }

        if (taskId == -1L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as? RemindPulseApp
                val repository = app?.repository ?: com.example.data.TaskRepository(
                    com.example.data.AppDatabase.getInstance(context).taskDao(),
                    context
                )

                val task = repository.getTaskById(taskId)
                if (task != null) {
                    when (action) {
                        NotificationHelper.ACTION_MARK_COMPLETE -> {
                            repository.toggleComplete(task)
                        }
                        NotificationHelper.ACTION_SNOOZE -> {
                            repository.snoozeTask(taskId, 15) // Snooze 15 minutes
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("NotificationActionReceiver", "Error processing notification action: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
