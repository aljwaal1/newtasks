package com.aljwaal.newtasks

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

enum class AppDestination(val label: String, val icon: ImageVector) {
    HOME("الرئيسية", Icons.Default.Home),
    TASKS("المهام", Icons.Default.TaskAlt),
    CALENDAR("التقويم", Icons.Default.CalendarMonth),
    SETTINGS("الإعدادات", Icons.Default.Settings)
}

enum class TaskListFilter(val label: String) {
    ALL("الكل"), PENDING("قيد التنفيذ"), OVERDUE("متأخرة"), COMPLETED("مكتملة")
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
    var destination by remember { mutableStateOf(AppDestination.HOME) }
    var tasks by remember { mutableStateOf(TaskRepository.list(context)) }
    var editingTask by remember { mutableStateOf<TaskItem?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var deleteCandidate by remember { mutableStateOf<TaskItem?>(null) }

    LaunchedEffect(refreshTick) { tasks = TaskRepository.list(context) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar(destination.label, tasks.count { it.status == TaskStatus.PENDING }) },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                AppDestination.entries.forEach { item ->
                    NavigationBarItem(
                        selected = destination == item,
                        onClick = { destination = item },
                        icon = { Icon(item.icon, null) },
                        label = { Text(item.label) }
                    )
                }
            }
        },
        floatingActionButton = {
            if (destination != AppDestination.SETTINGS) {
                FloatingActionButton(
                    onClick = { editingTask = null; showEditor = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) { Icon(Icons.Default.Add, "إضافة مهمة") }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (destination) {
                AppDestination.HOME -> HomeScreen(
                    tasks = tasks,
                    onOpenTasks = { destination = AppDestination.TASKS },
                    onEdit = { editingTask = it; showEditor = true },
                    onToggle = onToggleTask
                )
                AppDestination.TASKS -> TasksScreen(
                    tasks = tasks,
                    onEdit = { editingTask = it; showEditor = true },
                    onDelete = { deleteCandidate = it },
                    onToggle = onToggleTask
                )
                AppDestination.CALENDAR -> CalendarScreen(
                    tasks = tasks,
                    onEdit = { editingTask = it; showEditor = true },
                    onToggle = onToggleTask
                )
                AppDestination.SETTINGS -> SettingsScreen(
                    refreshTick = refreshTick,
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
        }
    }

    if (showEditor) {
        TaskEditorDialog(
            existing = editingTask,
            categories = TaskRepository.categories(context),
            onDismiss = { showEditor = false },
            onSave = { onSaveTask(it); showEditor = false }
        )
    }

    deleteCandidate?.let { task ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("حذف المهمة") },
            text = { Text("هل تريد حذف «${task.title}» نهائيًا؟") },
            confirmButton = {
                Button(onClick = { onDeleteTask(task); deleteCandidate = null }) { Text("حذف") }
            },
            dismissButton = { TextButton(onClick = { deleteCandidate = null }) { Text("إلغاء") } }
        )
    }
}

@Composable
private fun AppTopBar(title: String, pendingCount: Int) {
    Surface(color = Color.White, shadowElevation = 2.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(42.dp).background(Color(0xFFE0E7FF), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.EventNote, null, tint = Color(0xFF4338CA)) }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                Text("Smart Tasks • ${pendingCount} قيد التنفيذ", color = Color(0xFF64748B), fontSize = 12.sp)
            }
            Surface(shape = CircleShape, color = Color(0xFFEEF2FF)) {
                Icon(Icons.Default.NotificationsActive, null, tint = Color(0xFF4F46E5), modifier = Modifier.padding(10.dp))
            }
        }
    }
}
