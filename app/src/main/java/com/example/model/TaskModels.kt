package com.example.model

import androidx.annotation.RawRes
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.R

enum class Priority(val displayName: String, val level: Int) {
    LOW("Low", 1),
    MEDIUM("Medium", 2),
    HIGH("High", 3),
    URGENT("Urgent Deadline", 4)
}

enum class Category(val displayName: String, val emoji: String) {
    WORK("Work", "💼"),
    PERSONAL("Personal", "🏠"),
    HEALTH("Health", "💊"),
    STUDY("Study", "📚"),
    FINANCE("Finance", "💳"),
    GENERAL("General", "🔔")
}

enum class RecurrenceType(val displayName: String) {
    NONE("Does not repeat"),
    DAILY("Every day"),
    WEEKDAYS("Weekdays (Mon-Fri)"),
    WEEKLY("Every week"),
    MONTHLY("Every month")
}

enum class AlertTone(
    val id: String,
    val title: String,
    val description: String,
    @RawRes val rawResId: Int?,
    val channelId: String
) {
    URGENT_ALARM(
        id = "urgent_alarm",
        title = "Urgent Alarm",
        description = "Punchy high-pitch alert for critical deadlines",
        rawResId = R.raw.tone_urgent,
        channelId = "channel_remindpulse_urgent"
    ),
    GENTLE_CHIME(
        id = "gentle_chime",
        title = "Gentle Chime",
        description = "Harmonic bell chime for daily reminders",
        rawResId = R.raw.tone_chime,
        channelId = "channel_remindpulse_gentle"
    ),
    CRYSTAL_BELL(
        id = "crystal_bell",
        title = "Crystal Bell",
        description = "Crisp resonant tone for appointments",
        rawResId = R.raw.tone_bell,
        channelId = "channel_remindpulse_bell"
    ),
    DIGITAL_PULSE(
        id = "digital_pulse",
        title = "Digital Pulse",
        description = "Modern futuristic triple-beep",
        rawResId = R.raw.tone_digital,
        channelId = "channel_remindpulse_digital"
    ),
    COSMIC_HARP(
        id = "cosmic_harp",
        title = "Cosmic Harp",
        description = "Soothing acoustic string strum",
        rawResId = R.raw.tone_harp,
        channelId = "channel_remindpulse_harp"
    ),
    SYSTEM_DEFAULT(
        id = "system_default",
        title = "System Default",
        description = "Standard device notification sound",
        rawResId = null,
        channelId = "channel_remindpulse_default"
    );

    companion object {
        fun fromId(id: String?): AlertTone {
            return entries.firstOrNull { it.id == id } ?: GENTLE_CHIME
        }
    }
}

@Entity(tableName = "task_reminders")
data class TaskReminder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val notes: String = "",
    val dueDate: Long, // timestamp in ms
    val priority: Priority = Priority.MEDIUM,
    val category: Category = Category.GENERAL,
    val alertTone: AlertTone = AlertTone.GENTLE_CHIME,
    val recurrenceType: RecurrenceType = RecurrenceType.NONE,
    val recurrenceDays: String = "", // e.g., "1,3,5" for Mon, Wed, Fri
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val notificationId: Int = (System.currentTimeMillis() % 100000).toInt(),
    val isNotificationEnabled: Boolean = true,
    val lastNotifiedAt: Long? = null,
    val snoozeUntil: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val activeAlarmTime: Long
        get() = snoozeUntil ?: dueDate

    val isOverdue: Boolean
        get() = !isCompleted && activeAlarmTime < System.currentTimeMillis()

    val isSnoozed: Boolean
        get() = snoozeUntil != null && snoozeUntil > System.currentTimeMillis()
}
