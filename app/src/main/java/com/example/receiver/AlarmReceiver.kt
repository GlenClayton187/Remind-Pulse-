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

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(NotificationHelper.EXTRA_TASK_ID, -1L)
        if (taskId == -1L) return

        Log.d("AlarmReceiver", "Received alarm for task $taskId")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as? RemindPulseApp
                val repository = app?.repository ?: com.example.data.TaskRepository(
                    com.example.data.AppDatabase.getInstance(context).taskDao(),
                    context
                )

                val task = repository.getTaskById(taskId)
                if (task != null && !task.isCompleted && task.isNotificationEnabled) {
                    NotificationHelper.sendReminderNotification(context, task)
                }
            } catch (e: Exception) {
                Log.e("AlarmReceiver", "Error processing alarm: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
