package com.aljwaal.newtasks

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class TaskListFilter(val label: String) {
    ALL("الكل"),
    PENDING("قيد التنفيذ"),
    OVERDUE("متأخرة"),
    COMPLETED("مكتملة")
}

@Composable
fun SmartTasksRoot(
    refreshTick: Int,
    onRefresh: () -> Unit,
    onSaveTask: (TaskItem) -> Unit,
    onDeleteTask: (TaskItem) -> Unit,
    onToggleTask: (TaskItem) -> Unit,
    onRequestNotifications: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onOpenFullScreenSettings: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onTestNow: () -> Unit,
    onTestAfter30: () -> Unit,
    onShareBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onCreateLocalBackup: () -> Unit,
    onRestoreLocalBackup: () -> Unit,
    onShareLog: () -> Unit,
    onClearLog: () -> Unit
) {
    val context = LocalContext.current
    var tasks by remember { mutableStateOf<List<TaskItem>>(emptyList()) }
    var categories by remember { mutableStateOf<List<String>>(emptyList()) }
    var priorities by remember { mutableStateOf(TaskPriority.defaults) }
    var isLoading by remember { mutableStateOf(true) }
    var editingTask by remember { mutableStateOf<TaskItem?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var deleteCandidate by remember { mutableStateOf<TaskItem?>(null) }

    LaunchedEffect(refreshTick) {
        isLoading = true
        val snapshot = withContext(Dispatchers.IO) {
            Triple(
                TaskRepository.list(context),
                TaskRepository.categories(context),
                TaskRepository.priorities(context)
            )
        }
        tasks = snapshot.first
        categories = snapshot.second
        priorities = snapshot.third
        isLoading = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading && tasks.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            UnifiedTasksScreen(
                tasks = tasks,
                priorities = priorities,
                onAdd = {
                    editingTask = null
                    showEditor = true
                },
                onSettings = { showSettings = true },
                onEdit = {
                    editingTask = it
                    showEditor = true
                },
                onDelete = { deleteCandidate = it },
                onToggle = onToggleTask
            )
        }
    }

    if (showEditor) {
        TaskEditorDialog(
            existing = editingTask,
            categories = categories.ifEmpty { listOf("عام") },
            priorities = priorities.ifEmpty { TaskPriority.defaults },
            onDismiss = { showEditor = false },
            onSave = {
                onSaveTask(it)
                showEditor = false
            }
        )
    }

    if (showSettings) {
        CompactSettingsDialog(
            refreshTick = refreshTick,
            onDismiss = { showSettings = false },
            onRefresh = onRefresh,
            onRequestNotifications = onRequestNotifications,
            onOpenExactAlarmSettings = onOpenExactAlarmSettings,
            onOpenFullScreenSettings = onOpenFullScreenSettings,
            onOpenNotificationSettings = onOpenNotificationSettings,
            onOpenBatterySettings = onOpenBatterySettings,
            onTestNow = onTestNow,
            onTestAfter30 = onTestAfter30,
            onShareBackup = onShareBackup,
            onImportBackup = onImportBackup,
            onCreateLocalBackup = onCreateLocalBackup,
            onRestoreLocalBackup = onRestoreLocalBackup,
            onShareLog = onShareLog,
            onClearLog = onClearLog
        )
    }

    deleteCandidate?.let { task ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("حذف المهمة") },
            text = { Text("هل تريد حذف «${task.title}» نهائيًا؟") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteTask(task)
                        deleteCandidate = null
                    }
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
