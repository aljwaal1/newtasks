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

@Composable
internal fun HomeScreen(
    tasks: List<TaskItem>,
    onOpenTasks: () -> Unit,
    onEdit: (TaskItem) -> Unit,
    onToggle: (TaskItem) -> Unit
) {
    val now = System.currentTimeMillis()
    val stats = remember(tasks) {
        val todayStart = NumberFormatUtils.startOfDay(now)
        val todayEnd = NumberFormatUtils.endOfDay(now)
        TaskStats(
            today = tasks.count { it.status == TaskStatus.PENDING && it.dueAtMillis in todayStart..todayEnd },
            upcoming = tasks.count { it.status == TaskStatus.PENDING && it.dueAtMillis > todayEnd },
            overdue = tasks.count { it.status == TaskStatus.PENDING && it.dueAtMillis < now },
            completed = tasks.count { it.status == TaskStatus.COMPLETED },
            total = tasks.size
        )
    }
    val todayTasks = tasks.filter {
        it.status == TaskStatus.PENDING && NumberFormatUtils.sameDay(it.dueAtMillis, now)
    }.sortedBy { it.dueAtMillis }
    val nextTasks = tasks.filter { it.status == TaskStatus.PENDING && it.dueAtMillis > now }
        .sortedBy { it.dueAtMillis }.take(4)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF312E81))
            ) {
                Column(modifier = Modifier.padding(22.dp)) {
                    Text("مرحبًا بك", color = Color(0xFFC7D2FE), fontSize = 15.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        NumberFormatUtils.formatWeekdayDate(now),
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (stats.today == 0) "لا توجد مهام اليوم، استمتع بوقتك." else "لديك ${stats.today} مهمة تحتاج انتباهك اليوم.",
                        color = Color(0xFFE0E7FF),
                        lineHeight = 23.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onOpenTasks) { Text("عرض جميع المهام") }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("اليوم", stats.today, Color(0xFFEEF2FF), Color(0xFF4338CA), Modifier.weight(1f))
                    StatCard("قادمة", stats.upcoming, Color(0xFFECFDF5), Color(0xFF047857), Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("متأخرة", stats.overdue, Color(0xFFFEF2F2), Color(0xFFB91C1C), Modifier.weight(1f))
                    StatCard("مكتملة", stats.completed, Color(0xFFFFF7ED), Color(0xFFC2410C), Modifier.weight(1f))
                }
            }
        }
        item { SectionHeader("مهام اليوم", "الأهم أولًا") }
        if (todayTasks.isEmpty()) {
            item { EmptyState("لا توجد مهام اليوم", "اضغط زر + لإضافة مهمة جديدة") }
        } else {
            items(todayTasks, key = { it.id }) { TaskCard(it, onEdit, {}, onToggle, compact = true) }
        }
        item { SectionHeader("التالي", "أقرب المواعيد القادمة") }
        if (nextTasks.isEmpty()) {
            item { EmptyState("لا توجد مواعيد قادمة", "يمكنك إضافة موعد من زر +") }
        } else {
            items(nextTasks, key = { it.id }) { TaskCard(it, onEdit, {}, onToggle, compact = true) }
        }
    }
}

@Composable
private fun StatCard(title: String, value: Int, background: Color, foreground: Color, modifier: Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = background)) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(value.toString(), fontSize = 30.sp, fontWeight = FontWeight.Black, color = foreground)
            Text(title, color = foreground, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
internal fun TasksScreen(
    tasks: List<TaskItem>,
    onEdit: (TaskItem) -> Unit,
    onDelete: (TaskItem) -> Unit,
    onToggle: (TaskItem) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(TaskListFilter.ALL) }
    var priority by remember { mutableStateOf<TaskPriority?>(null) }
    val now = System.currentTimeMillis()
    val filtered = tasks.filter { task ->
        val matchesText = query.isBlank() || task.title.contains(query, true) || task.notes.contains(query, true) || task.category.contains(query, true)
        val matchesFilter = when (filter) {
            TaskListFilter.ALL -> true
            TaskListFilter.PENDING -> task.status == TaskStatus.PENDING
            TaskListFilter.COMPLETED -> task.status == TaskStatus.COMPLETED
            TaskListFilter.OVERDUE -> task.status == TaskStatus.PENDING && task.dueAtMillis < now
        }
        matchesText && matchesFilter && (priority == null || task.priority == priority)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("بحث في المهام") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                shape = RoundedCornerShape(18.dp)
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TaskListFilter.entries.forEach { item ->
                    ChoicePill(item.label, filter == item) { filter = item }
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChoicePill("كل الأولويات", priority == null) { priority = null }
                TaskPriority.entries.forEach { item -> ChoicePill(item.label, priority == item) { priority = item } }
            }
        }
        item {
            Text("${filtered.size} نتيجة", color = Color(0xFF64748B), fontSize = 13.sp)
        }
        if (filtered.isEmpty()) {
            item { EmptyState("لا توجد نتائج", "غيّر البحث أو عوامل التصفية") }
        } else {
            items(filtered, key = { it.id }) { task -> TaskCard(task, onEdit, onDelete, onToggle) }
        }
    }
}

@Composable
internal fun TaskCard(
    task: TaskItem,
    onEdit: (TaskItem) -> Unit,
    onDelete: (TaskItem) -> Unit,
    onToggle: (TaskItem) -> Unit,
    compact: Boolean = false
) {
    val overdue = task.status == TaskStatus.PENDING && task.dueAtMillis < System.currentTimeMillis()
    val priorityColor = when (task.priority) {
        TaskPriority.LOW -> Color(0xFF0284C7)
        TaskPriority.NORMAL -> Color(0xFF64748B)
        TaskPriority.HIGH -> Color(0xFFEA580C)
        TaskPriority.URGENT -> Color(0xFFDC2626)
    }
    var menuOpen by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onEdit(task) },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier.size(10.dp).clip(CircleShape).background(priorityColor).padding(top = 4.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    task.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = if (task.status == TaskStatus.COMPLETED) Color(0xFF94A3B8) else Color(0xFF0F172A),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!compact && task.notes.isNotBlank()) {
                    Spacer(Modifier.height(5.dp))
                    Text(task.notes, color = Color(0xFF64748B), maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(9.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    InfoPill(task.category, Color(0xFFEEF2FF), Color(0xFF4338CA))
                    InfoPill(task.priority.label, Color(0xFFFFF7ED), priorityColor)
                    if (task.repeatRule != RepeatRule.NONE) InfoPill(task.repeatRule.label, Color(0xFFF0FDFA), Color(0xFF0F766E))
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    NumberFormatUtils.formatDateTime(task.dueAtMillis),
                    color = if (overdue) Color(0xFFDC2626) else Color(0xFF475569),
                    fontWeight = if (overdue) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 13.sp
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = { onToggle(task) }) {
                    Icon(
                        if (task.status == TaskStatus.COMPLETED) Icons.Default.CheckCircle else Icons.Default.Schedule,
                        null,
                        tint = if (task.status == TaskStatus.COMPLETED) Color(0xFF0F766E) else Color(0xFF94A3B8)
                    )
                }
                if (!compact) {
                    Box {
                        IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, null) }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("تعديل") },
                                leadingIcon = { Icon(Icons.Default.Edit, null) },
                                onClick = { menuOpen = false; onEdit(task) }
                            )
                            DropdownMenuItem(
                                text = { Text("حذف") },
                                leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color(0xFFDC2626)) },
                                onClick = { menuOpen = false; onDelete(task) }
                            )
                        }
                    }
                }
            }
        }
    }
}
