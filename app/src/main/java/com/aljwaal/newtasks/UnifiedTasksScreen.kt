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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.util.Calendar

private enum class UnifiedStatusFilter(val label: String) {
    ALL("كل الحالات"),
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
    var showFilters by remember { mutableStateOf(false) }

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
    val activeFilterCount = listOf(
        status != UnifiedStatusFilter.ALL,
        dateFilter != UnifiedDateFilter.ALL,
        priorityId != null
    ).count { it }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F7FB))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .padding(bottom = 72.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SimpleHeader(
                pendingCount = tasks.count { it.status == TaskStatus.PENDING },
                onSettings = onSettings
            )
            SummaryRow(
                tasks = tasks,
                now = now,
                onToday = {
                    status = UnifiedStatusFilter.ALL
                    dateFilter = UnifiedDateFilter.TODAY
                    priorityId = null
                },
                onOverdue = {
                    status = UnifiedStatusFilter.OVERDUE
                    dateFilter = UnifiedDateFilter.ALL
                    priorityId = null
                },
                onCompleted = {
                    status = UnifiedStatusFilter.COMPLETED
                    dateFilter = UnifiedDateFilter.ALL
                    priorityId = null
                }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = NumberFormatUtils.latinDigits(it) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("ابحث في المهام") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    shape = RoundedCornerShape(16.dp)
                )
                Surface(
                    modifier = Modifier
                        .size(56.dp)
                        .clickable { showFilters = true },
                    shape = RoundedCornerShape(16.dp),
                    color = if (activeFilterCount > 0) Color(0xFF4F46E5) else Color.White,
                    contentColor = if (activeFilterCount > 0) Color.White else Color(0xFF475569),
                    shadowElevation = 1.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.FilterList, "التصفية")
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${NumberFormatUtils.number(filtered.size)} مهمة",
                    modifier = Modifier.weight(1f),
                    color = Color(0xFF64748B),
                    fontSize = 12.sp
                )
                if (activeFilterCount > 0) {
                    TextButton(
                        onClick = {
                            status = UnifiedStatusFilter.ALL
                            dateFilter = UnifiedDateFilter.ALL
                            priorityId = null
                        }
                    ) {
                        Text("إلغاء التصفية", fontSize = 11.sp)
                    }
                }
            }
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                if (filtered.isEmpty()) {
                    EmptyTasks(onAdd)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        items(filtered, key = { it.id }) { task ->
                            CleanTaskCard(task, onEdit, onDelete, onToggle)
                        }
                    }
                }
            }
        }

        BottomAddBar(
            onAdd = onAdd,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (showFilters) {
        FilterDialog(
            priorities = priorities,
            status = status,
            dateFilter = dateFilter,
            priorityId = priorityId,
            onStatus = { status = it },
            onDate = { dateFilter = it },
            onPriority = { priorityId = it },
            onClear = {
                status = UnifiedStatusFilter.ALL
                dateFilter = UnifiedDateFilter.ALL
                priorityId = null
            },
            onDismiss = { showFilters = false }
        )
    }
}

@Composable
private fun SimpleHeader(pendingCount: Int, onSettings: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("مهامي", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
            Text(
                "${NumberFormatUtils.number(pendingCount)} قيد التنفيذ",
                color = Color(0xFF64748B),
                fontSize = 12.sp
            )
        }
        Surface(
            modifier = Modifier.size(46.dp).clickable(onClick = onSettings),
            shape = RoundedCornerShape(15.dp),
            color = Color.White,
            shadowElevation = 1.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Settings, "الإعدادات", tint = Color(0xFF475569))
            }
        }
    }
}

@Composable
private fun SummaryRow(
    tasks: List<TaskItem>,
    now: Long,
    onToday: () -> Unit,
    onOverdue: () -> Unit,
    onCompleted: () -> Unit
) {
    val today = tasks.count {
        it.status == TaskStatus.PENDING && NumberFormatUtils.sameDay(it.dueAtMillis, now)
    }
    val overdue = tasks.count {
        it.status == TaskStatus.PENDING && it.dueAtMillis < now
    }
    val completed = tasks.count { it.status == TaskStatus.COMPLETED }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        SummaryCard("اليوم", today, Color(0xFFEFF6FF), Color(0xFF1D4ED8), onToday, Modifier.weight(1f))
        SummaryCard("متأخرة", overdue, Color(0xFFFEF2F2), Color(0xFFB91C1C), onOverdue, Modifier.weight(1f))
        SummaryCard("مكتملة", completed, Color(0xFFECFDF5), Color(0xFF047857), onCompleted, Modifier.weight(1f))
    }
}

@Composable
private fun SummaryCard(
    label: String,
    value: Int,
    background: Color,
    foreground: Color,
    onClick: () -> Unit,
    modifier: Modifier
) {
    Surface(
        modifier = modifier.height(66.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(17.dp),
        color = background
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(NumberFormatUtils.number(value), color = foreground, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Text(label, color = foreground, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun EmptyTasks(onAdd: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.EventNote, null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(54.dp))
        Spacer(Modifier.height(8.dp))
        Text("لا توجد مهام هنا", fontWeight = FontWeight.Bold, color = Color(0xFF334155))
        TextButton(onClick = onAdd) { Text("أضف أول مهمة") }
    }
}

@Composable
private fun BottomAddBar(onAdd: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 10.dp
    ) {
        Button(
            onClick = onAdd,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp).height(54.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Icon(Icons.Default.Add, null)
            Spacer(Modifier.width(8.dp))
            Text("إضافة مهمة", fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CleanTaskCard(
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
    var menuOpen by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(46.dp).clickable { onToggle(task) },
                shape = RoundedCornerShape(15.dp),
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
            Spacer(Modifier.width(10.dp))
            Column(
                modifier = Modifier.weight(1f).clickable { onEdit(task) }
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
                Text(
                    "${NumberFormatUtils.formatDateTime(task.dueAtMillis)} • ${task.priority.label}",
                    color = if (overdue) Color(0xFFDC2626) else priorityColor,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (task.category.isNotBlank() && task.category != "عام") {
                    Text(task.category, color = Color(0xFF94A3B8), fontSize = 10.sp)
                }
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Default.MoreVert, "خيارات المهمة", tint = Color(0xFF64748B))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("تعديل") },
                        leadingIcon = { Icon(Icons.Default.Edit, null) },
                        onClick = {
                            menuOpen = false
                            onEdit(task)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("حذف", color = Color(0xFFDC2626)) },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color(0xFFDC2626)) },
                        onClick = {
                            menuOpen = false
                            onDelete(task)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterDialog(
    priorities: List<TaskPriority>,
    status: UnifiedStatusFilter,
    dateFilter: UnifiedDateFilter,
    priorityId: String?,
    onStatus: (UnifiedStatusFilter) -> Unit,
    onDate: (UnifiedDateFilter) -> Unit,
    onPriority: (String?) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = Color.White) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("تصفية المهام", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                CompactSelector(
                    value = status.label,
                    options = UnifiedStatusFilter.entries.map { it.label },
                    onSelected = { label -> onStatus(UnifiedStatusFilter.entries.first { it.label == label }) }
                )
                CompactSelector(
                    value = dateFilter.label,
                    options = UnifiedDateFilter.entries.map { it.label },
                    onSelected = { label -> onDate(UnifiedDateFilter.entries.first { it.label == label }) }
                )
                CompactSelector(
                    value = priorityId?.let { id -> priorities.firstOrNull { it.id == id }?.label }
                        ?: "كل الأولويات",
                    options = listOf("كل الأولويات") + priorities.map { it.label },
                    onSelected = { label -> onPriority(priorities.firstOrNull { it.label == label }?.id) }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onClear, modifier = Modifier.weight(1f)) {
                        Text("مسح")
                    }
                    Button(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("تطبيق")
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactSelector(
    value: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { open = true },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            contentPadding = PaddingValues(horizontal = 12.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(value, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
