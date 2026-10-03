package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.RemindPulseApp
import com.example.data.TaskRepository
import com.example.model.AlertTone
import com.example.model.Category
import com.example.model.Priority
import com.example.model.RecurrenceType
import com.example.model.TaskReminder
import com.example.notification.NotificationHelper
import com.example.util.TonePlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class TaskFilter(val displayName: String) {
    ALL("All"),
    TODAY("Today"),
    OVERDUE("Overdue"),
    RECURRING("Recurring"),
    COMPLETED("Completed")
}

data class TaskStats(
    val totalActive: Int = 0,
    val dueToday: Int = 0,
    val overdue: Int = 0,
    val recurring: Int = 0,
    val completed: Int = 0
)

class TaskViewModel(
    private val repository: TaskRepository = RemindPulseApp.instance.repository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(TaskFilter.ALL)
    val selectedFilter: StateFlow<TaskFilter> = _selectedFilter.asStateFlow()

    private val _selectedCategory = MutableStateFlow<Category?>(null)
    val selectedCategory: StateFlow<Category?> = _selectedCategory.asStateFlow()

    private val _editingTask = MutableStateFlow<TaskReminder?>(null)
    val editingTask: StateFlow<TaskReminder?> = _editingTask.asStateFlow()

    private val _isCreateEditDialogOpen = MutableStateFlow(false)
    val isCreateEditDialogOpen: StateFlow<Boolean> = _isCreateEditDialogOpen.asStateFlow()

    private val _isToneSettingsOpen = MutableStateFlow(false)
    val isToneSettingsOpen: StateFlow<Boolean> = _isToneSettingsOpen.asStateFlow()

    private val _playingToneId = MutableStateFlow<String?>(null)
    val playingToneId: StateFlow<String?> = _playingToneId.asStateFlow()

    private val _lastActionSnackbar = MutableStateFlow<String?>(null)
    val lastActionSnackbar: StateFlow<String?> = _lastActionSnackbar.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }
    }

    // Raw tasks flow
    val allTasks: StateFlow<List<TaskReminder>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Statistics computed reactively
    val stats: StateFlow<TaskStats> = allTasks.combine(_searchQuery) { tasks, _ ->
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        val todayStart = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val todayEnd = todayStart + (24 * 60 * 60 * 1000L)

        var activeCount = 0
        var todayCount = 0
        var overdueCount = 0
        var recurringCount = 0
        var completedCount = 0

        for (task in tasks) {
            if (task.isCompleted) {
                completedCount++
            } else {
                activeCount++
                if (task.activeAlarmTime < now) {
                    overdueCount++
                } else if (task.activeAlarmTime in todayStart until todayEnd) {
                    todayCount++
                }
                if (task.recurrenceType != RecurrenceType.NONE) {
                    recurringCount++
                }
            }
        }

        TaskStats(
            totalActive = activeCount,
            dueToday = todayCount,
            overdue = overdueCount,
            recurring = recurringCount,
            completed = completedCount
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TaskStats())

    // Filtered tasks flow
    val filteredTasks: StateFlow<List<TaskReminder>> = combine(
        allTasks,
        _searchQuery,
        _selectedFilter,
        _selectedCategory
    ) { tasks, query, filter, category ->
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        val todayStart = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val todayEnd = todayStart + (24 * 60 * 60 * 1000L)

        tasks.filter { task ->
            // Search query filter
            val matchesQuery = query.isBlank() ||
                    task.title.contains(query, ignoreCase = true) ||
                    task.notes.contains(query, ignoreCase = true) ||
                    task.priority.displayName.contains(query, ignoreCase = true) ||
                    task.alertTone.title.contains(query, ignoreCase = true)

            // Category filter
            val matchesCategory = category == null || task.category == category

            // Tab filter
            val matchesFilter = when (filter) {
                TaskFilter.ALL -> true
                TaskFilter.TODAY -> !task.isCompleted && task.activeAlarmTime in todayStart until todayEnd
                TaskFilter.OVERDUE -> !task.isCompleted && task.activeAlarmTime < now
                TaskFilter.RECURRING -> task.recurrenceType != RecurrenceType.NONE
                TaskFilter.COMPLETED -> task.isCompleted
            }

            matchesQuery && matchesCategory && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: TaskFilter) {
        _selectedFilter.value = filter
    }

    fun setCategory(category: Category?) {
        _selectedCategory.value = if (_selectedCategory.value == category) null else category
    }

    fun openCreateDialog() {
        _editingTask.value = null
        _isCreateEditDialogOpen.value = true
    }

    fun openEditDialog(task: TaskReminder) {
        _editingTask.value = task
        _isCreateEditDialogOpen.value = true
    }

    fun closeCreateEditDialog() {
        _isCreateEditDialogOpen.value = false
        _editingTask.value = null
        stopTonePreview()
    }

    fun openToneSettings() {
        _isToneSettingsOpen.value = true
    }

    fun closeToneSettings() {
        _isToneSettingsOpen.value = false
        stopTonePreview()
    }

    fun saveTask(
        title: String,
        notes: String,
        dueDate: Long,
        priority: Priority,
        category: Category,
        alertTone: AlertTone,
        recurrenceType: RecurrenceType,
        recurrenceDays: String = ""
    ) {
        viewModelScope.launch {
            val existing = _editingTask.value
            if (existing != null) {
                val updated = existing.copy(
                    title = title.trim(),
                    notes = notes.trim(),
                    dueDate = dueDate,
                    priority = priority,
                    category = category,
                    alertTone = alertTone,
                    recurrenceType = recurrenceType,
                    recurrenceDays = recurrenceDays,
                    snoozeUntil = null // reset snooze on edit
                )
                repository.updateTask(updated)
                _lastActionSnackbar.value = "Updated: \"$title\" with ${alertTone.title} tone"
            } else {
                val newTask = TaskReminder(
                    title = title.trim(),
                    notes = notes.trim(),
                    dueDate = dueDate,
                    priority = priority,
                    category = category,
                    alertTone = alertTone,
                    recurrenceType = recurrenceType,
                    recurrenceDays = recurrenceDays
                )
                repository.insertTask(newTask)
                _lastActionSnackbar.value = "Reminder scheduled with ${alertTone.title}"
            }
            closeCreateEditDialog()
        }
    }

    fun toggleComplete(task: TaskReminder) {
        viewModelScope.launch {
            repository.toggleComplete(task)
            if (!task.isCompleted && task.recurrenceType != RecurrenceType.NONE) {
                _lastActionSnackbar.value = "Completed! Next occurrence scheduled for ${task.recurrenceType.displayName}"
            } else if (!task.isCompleted) {
                _lastActionSnackbar.value = "Task marked completed"
            } else {
                _lastActionSnackbar.value = "Task reopened"
            }
        }
    }

    fun deleteTask(task: TaskReminder) {
        viewModelScope.launch {
            repository.deleteTask(task)
            _lastActionSnackbar.value = "Task deleted"
        }
    }

    fun snoozeTask(task: TaskReminder, minutes: Int) {
        viewModelScope.launch {
            repository.snoozeTask(task.id, minutes)
            _lastActionSnackbar.value = "Snoozed for $minutes minutes"
        }
    }

    fun previewTone(context: Context, tone: AlertTone) {
        if (_playingToneId.value == tone.id) {
            stopTonePreview()
        } else {
            _playingToneId.value = tone.id
            TonePlayer.playTone(context, tone) {
                _playingToneId.value = null
            }
        }
    }

    fun stopTonePreview() {
        TonePlayer.stopTone()
        _playingToneId.value = null
    }

    fun triggerTestPushNotification(context: Context, tone: AlertTone) {
        NotificationHelper.sendTestNotification(
            context = context,
            tone = tone,
            title = "Test Reminder Alarm",
            message = "Alert tone activated:"
        )
        _lastActionSnackbar.value = "Push notification triggered with ${tone.title}!"
    }

    fun clearSnackbar() {
        _lastActionSnackbar.value = null
    }

    override fun onCleared() {
        super.onCleared()
        TonePlayer.stopTone()
    }
}
