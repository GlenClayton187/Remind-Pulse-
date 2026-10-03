package com.example.data

import android.content.Context
import com.example.model.AlertTone
import com.example.model.Category
import com.example.model.Priority
import com.example.model.RecurrenceType
import com.example.model.TaskReminder
import com.example.notification.AlarmScheduler
import com.example.notification.NotificationHelper
import com.example.util.RecurrenceHelper
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class TaskRepository(
    private val taskDao: TaskDao,
    private val context: Context
) {
    val allTasks: Flow<List<TaskReminder>> = taskDao.getAllTasks()
    val activeTasks: Flow<List<TaskReminder>> = taskDao.getActiveTasks()
    val completedTasks: Flow<List<TaskReminder>> = taskDao.getCompletedTasks()

    suspend fun getTaskById(id: Long): TaskReminder? {
        return taskDao.getTaskById(id)
    }

    suspend fun insertTask(task: TaskReminder): Long {
        val newId = taskDao.insertTask(task)
        val createdTask = task.copy(id = newId)
        AlarmScheduler.scheduleTaskReminder(context, createdTask)
        return newId
    }

    suspend fun updateTask(task: TaskReminder) {
        taskDao.updateTask(task)
        if (task.isCompleted) {
            AlarmScheduler.cancelTaskReminder(context, task)
            NotificationHelper.cancelNotification(context, task.notificationId)
        } else {
            AlarmScheduler.scheduleTaskReminder(context, task)
        }
    }

    suspend fun deleteTask(task: TaskReminder) {
        AlarmScheduler.cancelTaskReminder(context, task)
        NotificationHelper.cancelNotification(context, task.notificationId)
        taskDao.deleteTask(task)
    }

    suspend fun deleteTaskById(id: Long) {
        val task = taskDao.getTaskById(id)
        if (task != null) {
            AlarmScheduler.cancelTaskReminder(context, task)
            NotificationHelper.cancelNotification(context, task.notificationId)
            taskDao.deleteTask(task)
        }
    }

    suspend fun toggleComplete(task: TaskReminder) {
        val isNowCompleted = !task.isCompleted
        NotificationHelper.cancelNotification(context, task.notificationId)

        if (isNowCompleted) {
            // Task completed
            AlarmScheduler.cancelTaskReminder(context, task)

            if (task.recurrenceType != RecurrenceType.NONE) {
                // If it is recurring, advance to next recurrence cycle
                val nextDueDate = RecurrenceHelper.calculateNextDueDate(
                    currentDueDate = task.dueDate,
                    recurrenceType = task.recurrenceType,
                    recurrenceDays = task.recurrenceDays
                )
                if (nextDueDate != null) {
                    val nextTask = task.copy(
                        dueDate = nextDueDate,
                        isCompleted = false,
                        completedAt = null,
                        snoozeUntil = null,
                        lastNotifiedAt = System.currentTimeMillis()
                    )
                    taskDao.updateTask(nextTask)
                    AlarmScheduler.scheduleTaskReminder(context, nextTask)
                    return
                }
            }

            // Normal non-recurring completion
            taskDao.setCompleted(task.id, true, System.currentTimeMillis())
        } else {
            // Un-complete task
            val uncompletedTask = task.copy(isCompleted = false, completedAt = null, snoozeUntil = null)
            taskDao.updateTask(uncompletedTask)
            AlarmScheduler.scheduleTaskReminder(context, uncompletedTask)
        }
    }

    suspend fun snoozeTask(taskId: Long, minutes: Int = 15) {
        val task = taskDao.getTaskById(taskId) ?: return
        val snoozeTime = System.currentTimeMillis() + (minutes * 60_000L)
        val snoozedTask = task.copy(snoozeUntil = snoozeTime)

        NotificationHelper.cancelNotification(context, task.notificationId)
        taskDao.snoozeTask(taskId, snoozeTime)
        AlarmScheduler.scheduleTaskReminder(context, snoozedTask)
    }

    suspend fun rescheduleAllActiveAlarms() {
        val active = taskDao.getActiveTasksSync()
        for (task in active) {
            AlarmScheduler.scheduleTaskReminder(context, task)
        }
    }

    suspend fun seedInitialDataIfEmpty() {
        val count = taskDao.getTaskCount()
        if (count > 0) return

        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()

        // 1. Critical Deadline due in 2 hours
        calendar.timeInMillis = now
        calendar.add(Calendar.HOUR_OF_DAY, 2)
        val urgentDue = calendar.timeInMillis

        // 2. Daily recurring event tomorrow morning at 9:00 AM
        calendar.timeInMillis = now
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 9)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val dailyDue = calendar.timeInMillis

        // 3. Weekly recurring event
        calendar.timeInMillis = now
        calendar.add(Calendar.DAY_OF_YEAR, 3)
        calendar.set(Calendar.HOUR_OF_DAY, 17)
        calendar.set(Calendar.MINUTE, 30)
        val weeklyDue = calendar.timeInMillis

        val sampleTasks = listOf(
            TaskReminder(
                title = "Quarterly Project Review & Slides",
                notes = "Submit finalized executive deck and review metrics with team.",
                dueDate = urgentDue,
                priority = Priority.URGENT,
                category = Category.WORK,
                alertTone = AlertTone.URGENT_ALARM,
                recurrenceType = RecurrenceType.NONE,
                isCompleted = false
            ),
            TaskReminder(
                title = "Morning Standup & Daily Goals",
                notes = "Synchronize sprint backlog and outline today's primary milestones.",
                dueDate = dailyDue,
                priority = Priority.MEDIUM,
                category = Category.WORK,
                alertTone = AlertTone.GENTLE_CHIME,
                recurrenceType = RecurrenceType.DAILY,
                isCompleted = false
            ),
            TaskReminder(
                title = "Hydration & Posture Reset",
                notes = "Drink a glass of water, do a 5-minute stretch, and rest eyes.",
                dueDate = now + 45 * 60 * 1000L, // 45 mins from now
                priority = Priority.LOW,
                category = Category.HEALTH,
                alertTone = AlertTone.CRYSTAL_BELL,
                recurrenceType = RecurrenceType.DAILY,
                isCompleted = false
            ),
            TaskReminder(
                title = "Weekly Budget & Subscription Audit",
                notes = "Review personal finance transactions and update savings tracker.",
                dueDate = weeklyDue,
                priority = Priority.HIGH,
                category = Category.FINANCE,
                alertTone = AlertTone.DIGITAL_PULSE,
                recurrenceType = RecurrenceType.WEEKLY,
                isCompleted = false
            )
        )

        for (t in sampleTasks) {
            insertTask(t)
        }
    }
}
