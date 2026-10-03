package com.example

import android.app.Application
import com.example.data.AppDatabase
import com.example.data.TaskRepository
import com.example.notification.NotificationHelper

class RemindPulseApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: TaskRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getInstance(this)
        repository = TaskRepository(database.taskDao(), this)

        // Initialize notification channels with custom tones
        NotificationHelper.createNotificationChannels(this)
    }

    companion object {
        lateinit var instance: RemindPulseApp
            private set
    }
}
