package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.RemindPulseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.d("BootReceiver", "Device rebooted, rescheduling active reminder alarms")
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val app = context.applicationContext as? RemindPulseApp
                    val repository = app?.repository ?: com.example.data.TaskRepository(
                        com.example.data.AppDatabase.getInstance(context).taskDao(),
                        context
                    )
                    repository.rescheduleAllActiveAlarms()
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Failed to reschedule alarms after boot: ${e.message}", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
