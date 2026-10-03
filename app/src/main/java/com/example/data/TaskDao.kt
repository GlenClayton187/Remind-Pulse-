package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.TaskReminder
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM task_reminders ORDER BY isCompleted ASC, dueDate ASC")
    fun getAllTasks(): Flow<List<TaskReminder>>

    @Query("SELECT * FROM task_reminders WHERE isCompleted = 0 ORDER BY dueDate ASC")
    fun getActiveTasks(): Flow<List<TaskReminder>>

    @Query("SELECT * FROM task_reminders WHERE isCompleted = 1 ORDER BY completedAt DESC")
    fun getCompletedTasks(): Flow<List<TaskReminder>>

    @Query("SELECT * FROM task_reminders WHERE isCompleted = 0")
    suspend fun getActiveTasksSync(): List<TaskReminder>

    @Query("SELECT * FROM task_reminders WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: Long): TaskReminder?

    @Query("SELECT * FROM task_reminders WHERE id = :id LIMIT 1")
    fun getTaskByIdFlow(id: Long): Flow<TaskReminder?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskReminder): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<TaskReminder>)

    @Update
    suspend fun updateTask(task: TaskReminder)

    @Delete
    suspend fun deleteTask(task: TaskReminder)

    @Query("DELETE FROM task_reminders WHERE id = :id")
    suspend fun deleteTaskById(id: Long)

    @Query("UPDATE task_reminders SET isCompleted = :completed, completedAt = :completedAt WHERE id = :id")
    suspend fun setCompleted(id: Long, completed: Boolean, completedAt: Long?)

    @Query("UPDATE task_reminders SET snoozeUntil = :snoozeUntil WHERE id = :id")
    suspend fun snoozeTask(id: Long, snoozeUntil: Long?)

    @Query("SELECT COUNT(*) FROM task_reminders")
    suspend fun getTaskCount(): Int
}
