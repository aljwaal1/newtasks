package com.aljwaal.newtasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

private enum class UnifiedStatusFilter(val label: String) {
    ALL("الكل"),
    PENDING("قيد التنفيذ"),
    COMPLETED("مكتملة"),
    OVERDUE("متأخرة")
}

private enum class UnifiedDateFilter(val label: String) {
    ALL("كل التواريخ"),
    TODAY("اليوم"),
    TOMORROW("غدًا"),
    UPCOMING("قادمة")
}

@Composable
internal fun UnifiedTasksScreen(
    tasks: List<TaskItem>,
    priorities: List<TaskPriority>,
    onAdd: () -> Unit,
    onSettings: () -> Unit,
    onEdit: (TaskItem) -> Unit,
    onDelete: (TaskItem) -> Unit,
    onToggle: (TaskItem) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(UnifiedStatusFilter.ALL) }
    var dateFilter by remember { mutableStateOf(UnifiedDateFilter.ALL) }
    var priorityId by remember { mutableStateOf<String?>(null) }

    val now = System.currentTimeMillis()
    val tomorrow = remember(now / 60_000L) {
        Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
    }
    val filtered = remember(tasks, query, status, dateFilter, priorityId, now / 60_000L) {
        tasks.filter { task ->
            val textMatches = query.isBlank() ||
                task.title.contains(query, ignoreCase = true) ||
                task.notes.contains(query, ignoreCase = true) ||
                task.category.contains(query, ignoreCase = true)
            val statusMatches = when (status) {
                UnifiedStatusFilter.ALL -> true
                UnifiedStatusFilter.PENDING -> task.status == TaskStatus.PENDING
                UnifiedStatusFilter.COMPLETED -> task.status == TaskStatus.COMPLETED
                UnifiedStatusFilter.OVERDUE ->
                    task.status == TaskStatus.PENDING && task.dueAtMillis < now
            }
            val dateMatches = when (dateFilter) {
                UnifiedDateFilter.ALL -> true
                UnifiedDateFilter.TODAY -> NumberFormatUtils.sameDay(task.dueAtMillis, now)
                UnifiedDateFilter.TOMORROW -> NumberFormatUtils.sameDay(task.dueAtMillis, tomorrow)
                UnifiedDateFilter.UPCOMING -> task.dueAtMillis > NumberFormatUtils.endOfDay(now)
            }
            textMatches && statusMatches && dateMatches &&
                (priorityId == null || task.priority.id == priorityId)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F7FB))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        UnifiedHeader(
            pendingCount = tasks.count { it.status == TaskStatus.PENDING },
            onAdd = onAdd,
            onSettings = onSettings
        )
        StatsRow(tasks, now)
        OutlinedTextField(
            value = query,
            onValueChange = { query = NumberFormatUtils.latinDigits(it) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("بحث") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            shape = RoundedCornerShape(15.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CompactSelector(
                modifier = Modifier.weight(1f),
                value = status.label,
                options = UnifiedStatusFilter.entries.map { it.label },
                onSelected = { label ->
                    status = UnifiedStatusFilter.entries.first { it.label == label }
                }
            )
            CompactSelector(
                modifier = Modifier.weight(1f),
                value = dateFilter.label,
                options = UnifiedDateFilter.entries.map { it.label },
                onSelected = { label ->
                    dateFilter = UnifiedDateFilter.entries.first { it.label == label }
                }
            )
            CompactSelector(
                modifier = Modifier.weight(1f),
                value = priorityId?.let { id -> priorities.firstOrNull { it.id == id }?.label }
                    ?: "كل الأولويات",
                options = listOf("كل الأولويات") + priorities.map { it.label },
                onSelected = { label ->
                    priorityId = priorities.firstOrNull { it.label == label }?.id
                }
            )
        }
        Text(
            "${NumberFormatUtils.number(filtered.size)} مهمة",
            color = Color(0xFF64748B),
            fontSize = 12.sp
        )
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            if (filtered.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.EventNote,
                        null,
                        tint = Color(0xFFCBD5E1),
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("لا توجد مهام مطابقة", fontWeight = FontWeight.Bold)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    items(filtered, key = { it.id }) { task ->
                        UnifiedTaskCard(task, onEdit, onDelete, onToggle)
                    }
                }
            }
        }
    }
}

@Composable
private fun UnifiedHeader(
    pendingCount: Int,
    onAdd: () -> Unit,
    onSettings: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFE0E7FF), RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.EventNote, null, tint = Color(0xFF4338CA))
            }
            Spacer(Modifier.width(9.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("مهامي", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text(
                    "${NumberFormatUtils.number(pendingCount)} قيد التنفيذ",
                    color = Color(0xFF64748B),
                    fontSize = 11.sp
                )
            }
            IconButton(onClick = onSettings) {
                Icon(Icons.Default.Settings, "الإعدادات", tint = Color(0xFF475569))
            }
            Surface(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onAdd),
                color = Color(0xFF4F46E5),
                contentColor = Color.White,
                shape = RoundedCornerShape(14.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Add, "إضافة مهمة")
                }
            }
        }
    }
}

@Composable
private fun StatsRow(tasks: List<TaskItem>, now: Long) {
    val today = tasks.count {
        it.status == TaskStatus.PENDING && NumberFormatUtils.sameDay(it.dueAtMillis, now)
    }
    val overdue = tasks.count {
        it.status == TaskStatus.PENDING && it.dueAtMillis < now
    }
    val completed = tasks.count { it.status == TaskStatus.COMPLETED }
    val upcoming = tasks.count {
        it.status == TaskStatus.PENDING && it.dueAtMillis > NumberFormatUtils.endOfDay(now)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        MiniStat("اليوم", today, Color(0xFFEFF6FF), Color(0xFF1D4ED8), Modifier.weight(1f))
        MiniStat("قادمة", upcoming, Color(0xFFECFDF5), Color(0xFF047857), Modifier.weight(1f))
        MiniStat("متأخرة", overdue, Color(0xFFFEF2F2), Color(0xFFB91C1C), Modifier.weight(1f))
        MiniStat("مكتملة", completed, Color(0xFFFFF7ED), Color(0xFFC2410C), Modifier.weight(1f))
    }
}

@Composable
private fun MiniStat(
    label: String,
    value: Int,
    background: Color,
    foreground: Color,
    modifier: Modifier
) {
    Surface(modifier = modifier, shape = RoundedCornerShape(14.dp), color = background) {
        Column(
            modifier = Modifier.padding(vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                NumberFormatUtils.number(value),
                color = foreground,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black
            )
            Text(label, color = foreground, fontSize = 10.sp)
        }
    }
}

@Composable
private fun CompactSelector(
    modifier: Modifier,
    value: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { open = true },
            modifier = Modifier.fillMaxWidth().height(42.dp),
            contentPadding = PaddingValues(horizontal = 6.dp),
            shape = RoundedCornerShape(13.dp)
        ) {
            Text(value, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.distinct().forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        open = false
                    }
                )
            }
        }
    }
}

@Composable
private fun UnifiedTaskCard(
    task: TaskItem,
    onEdit: (TaskItem) -> Unit,
    onDelete: (TaskItem) -> Unit,
    onToggle: (TaskItem) -> Unit
) {
    val completed = task.status == TaskStatus.COMPLETED
    val overdue = !completed && task.dueAtMillis < System.currentTimeMillis()
    val priorityColor = when (task.priority.id) {
        TaskPriority.URGENT.id -> Color(0xFFDC2626)
        TaskPriority.MEDIUM.id -> Color(0xFFEA580C)
        else -> Color(0xFF64748B)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .size(45.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onToggle(task) },
                shape = RoundedCornerShape(14.dp),
                color = if (completed) Color(0xFFDCFCE7) else Color(0xFFF1F5F9)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.CheckCircle,
                        if (completed) "إعادة فتح" else "إنجاز",
                        tint = if (completed) Color(0xFF0F766E) else Color(0xFF94A3B8)
                    )
                }
            }
            Spacer(Modifier.width(9.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onEdit(task) }
            ) {
                Text(
                    task.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (completed) Color(0xFF94A3B8) else Color(0xFF0F172A),
                    textDecoration = if (completed) TextDecoration.LineThrough else null,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (task.notes.isNotBlank()) {
                    Text(
                        task.notes,
                        color = Color(0xFF64748B),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    "${task.category} • ${task.priority.label} • ${NumberFormatUtils.formatDateTime(task.dueAtMillis)}",
                    color = if (overdue) Color(0xFFDC2626) else priorityColor,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = { onEdit(task) }, modifier = Modifier.size(38.dp)) {
                Icon(Icons.Default.Edit, "تعديل", tint = Color(0xFF4F46E5))
            }
            IconButton(onClick = { onDelete(task) }, modifier = Modifier.size(38.dp)) {
                Icon(Icons.Default.Delete, "حذف", tint = Color(0xFFDC2626))
            }
        }
    }
}
