package com.example.ui.components

import android.text.format.DateUtils
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AlertTone
import com.example.model.Priority
import com.example.model.RecurrenceType
import com.example.model.TaskReminder
import com.example.ui.theme.HighPriorityOrange
import com.example.ui.theme.IndigoLight
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.UrgentCrimson
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskCard(
    task: TaskReminder,
    isPlayingThisTone: Boolean,
    onToggleComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSnooze: (minutes: Int) -> Unit,
    onPreviewTone: (AlertTone) -> Unit,
    onTestAlarm: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val priorityColor = when (task.priority) {
        Priority.URGENT -> UrgentCrimson
        Priority.HIGH -> HighPriorityOrange
        Priority.MEDIUM -> IndigoPrimary
        Priority.LOW -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    }

    val cardBorderColor by animateColorAsState(
        targetValue = when {
            task.isCompleted -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            task.isOverdue -> UrgentCrimson.copy(alpha = 0.6f)
            isPlayingThisTone -> IndigoLight
            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        },
        label = "cardBorderColor"
    )

    val cardBgColor = when {
        task.isCompleted -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        task.isOverdue -> UrgentCrimson.copy(alpha = 0.05f)
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("task_card_${task.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        border = BorderStroke(if (isPlayingThisTone || task.isOverdue) 2.dp else 1.dp, cardBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (task.isCompleted) 0.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Category emoji, Title, Checkbox, Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Interactive Checkbox / Completion Circle
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            if (task.isCompleted) SuccessEmerald else Color.Transparent
                        )
                        .border(
                            width = 2.dp,
                            color = if (task.isCompleted) SuccessEmerald else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            shape = CircleShape
                        )
                        .clickable(onClick = onToggleComplete)
                        .testTag("checkbox_task_${task.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    if (task.isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completed",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title and Notes
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = task.category.emoji,
                            fontSize = 16.sp
                        )
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                            ),
                            color = if (task.isCompleted) {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (task.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = task.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // More Menu Button
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(32.dp).testTag("task_menu_button_${task.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Task options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Reminder") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Snooze +15 mins") },
                            leadingIcon = { Icon(Icons.Default.Snooze, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onSnooze(15)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Snooze +1 hour") },
                            leadingIcon = { Icon(Icons.Default.Snooze, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onSnooze(60)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Trigger Notification Now") },
                            leadingIcon = { Icon(Icons.Default.NotificationsActive, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onTestAlarm()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = UrgentCrimson) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = UrgentCrimson) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Deadline countdown and status pill
            val deadlineText = formatDeadline(task)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (task.isSnoozed) Icons.Default.Snooze else Icons.Outlined.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = if (task.isOverdue) UrgentCrimson else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = deadlineText,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (task.isOverdue) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (task.isOverdue) UrgentCrimson else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Badges row: Priority, Recurrence, Custom Tone with live audio preview
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Priority Pill
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = priorityColor.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, priorityColor.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(priorityColor)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = task.priority.displayName,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = priorityColor
                        )
                    }
                }

                // Recurrence Badge if repeating
                if (task.recurrenceType != RecurrenceType.NONE) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Repeat,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = task.recurrenceType.displayName,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Custom Alert Tone Pill with Interactive Preview Button
                val tonePillBg = if (isPlayingThisTone) IndigoPrimary else MaterialTheme.colorScheme.surfaceVariant
                val tonePillText = if (isPlayingThisTone) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = tonePillBg,
                    border = BorderStroke(1.dp, if (isPlayingThisTone) IndigoLight else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    modifier = Modifier.clickable { onPreviewTone(task.alertTone) }
                ) {
                    Row(
                        modifier = Modifier.padding(start = 8.dp, end = 6.dp, top = 3.dp, bottom = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isPlayingThisTone) {
                            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                            val scale by infiniteTransition.animateFloat(
                                initialValue = 0.8f,
                                targetValue = 1.2f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(400, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "scale"
                            )
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = "Playing sound preview",
                                modifier = Modifier.size(13.dp).scale(scale),
                                tint = Color.White
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Alert Tone",
                                modifier = Modifier.size(13.dp),
                                tint = tonePillText
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = task.alertTone.title,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = tonePillText
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (isPlayingThisTone) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = if (isPlayingThisTone) "Stop" else "Preview",
                            modifier = Modifier.size(14.dp),
                            tint = tonePillText
                        )
                    }
                }
            }
        }
    }
}

private fun formatDeadline(task: TaskReminder): String {
    val time = task.activeAlarmTime
    val now = System.currentTimeMillis()
    val diff = time - now

    val cal = Calendar.getInstance().apply { timeInMillis = time }
    val nowCal = Calendar.getInstance()

    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val dateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(time))

    if (task.isCompleted) {
        val completedDate = task.completedAt?.let { Date(it) } ?: Date()
        return "Completed ${timeFormat.format(completedDate)}"
    }

    if (task.isSnoozed) {
        val mins = (diff / 60000).toInt()
        return "Snoozed until $formattedTime (in ${mins}m)"
    }

    if (diff < 0) {
        // Overdue
        val pastMins = (-diff / 60000).toInt()
        return if (pastMins < 60) {
            "⚠️ Overdue by $pastMins mins"
        } else {
            val hours = pastMins / 60
            "⚠️ Overdue by $hours hours ($formattedTime)"
        }
    }

    val isToday = cal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
            cal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)

    val isTomorrow = cal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
            cal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR) + 1

    return when {
        diff < 60 * 60 * 1000L -> {
            val mins = (diff / 60000).toInt().coerceAtLeast(1)
            "Due in $mins mins ($formattedTime)"
        }
        isToday -> "Today at $formattedTime"
        isTomorrow -> "Tomorrow at $formattedTime"
        else -> dateFormat.format(Date(time))
    }
}
