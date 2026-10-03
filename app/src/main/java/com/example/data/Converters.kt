package com.example.data

import androidx.room.TypeConverter
import com.example.model.AlertTone
import com.example.model.Category
import com.example.model.Priority
import com.example.model.RecurrenceType

class Converters {
    @TypeConverter
    fun fromPriority(priority: Priority): String = priority.name

    @TypeConverter
    fun toPriority(value: String): Priority = try {
        Priority.valueOf(value)
    } catch (_: Exception) {
        Priority.MEDIUM
    }

    @TypeConverter
    fun fromCategory(category: Category): String = category.name

    @TypeConverter
    fun toCategory(value: String): Category = try {
        Category.valueOf(value)
    } catch (_: Exception) {
        Category.GENERAL
    }

    @TypeConverter
    fun fromAlertTone(tone: AlertTone): String = tone.id

    @TypeConverter
    fun toAlertTone(value: String): AlertTone = AlertTone.fromId(value)

    @TypeConverter
    fun fromRecurrenceType(type: RecurrenceType): String = type.name

    @TypeConverter
    fun toRecurrenceType(value: String): RecurrenceType = try {
        RecurrenceType.valueOf(value)
    } catch (_: Exception) {
        RecurrenceType.NONE
    }
}
