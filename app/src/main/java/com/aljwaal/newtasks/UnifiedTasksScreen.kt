package com.aljwaal.newtasks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

@Suppress("UNUSED_PARAMETER")
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
    val now = System.currentTimeMillis()
    val orderedTasks = remember(tasks) {
        tasks.sortedWith(
            compareBy<TaskItem> { it.status == TaskStatus.COMPLETED }
                .thenBy { it.dueAtMillis }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F4FF))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .padding(bottom = 82.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            SimpleHeader(
                pendingCount = tasks.count { it.status == TaskStatus.PENDING },
                onSettings = onSettings
            )
            SummaryRow(tasks = tasks, now = now)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "المهام",
                    modifier = Modifier.weight(1f),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    NumberFormatUtils.number(tasks.size),
                    color = Color(0xFF64748B),
                    fontSize = 12.sp
                )
            }
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                if (orderedTasks.isEmpty()) {
                    EmptyTasks(onAdd)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        items(orderedTasks, key = { it.id }) { task ->
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
}

@Composable
private fun SimpleHeader(pendingCount: Int, onSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(listOf(Color(0xFF4338CA), Color(0xFF7C3AED))),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "مهامي",
                fontSize = 25.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Text(
                "${NumberFormatUtils.number(pendingCount)} قيد التنفيذ",
                color = Color(0xFFE0E7FF),
                fontSize = 12.sp
            )
        }
        Surface(
            modifier = Modifier.size(46.dp).clickable(onClick = onSettings),
            shape = RoundedCornerShape(15.dp),
            color = Color.White.copy(alpha = 0.18f),
            shadowElevation = 0.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Settings, "الإعدادات", tint = Color.White)
            }
        }
    }
}

@Composable
private fun SummaryRow(tasks: List<TaskItem>, now: Long) {
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
        SummaryCard("اليوم", today, Color(0xFFE7EEFF), Color(0xFF2554D9), Modifier.weight(1f))
        SummaryCard("متأخرة", overdue, Color(0xFFFFE9E7), Color(0xFFD92D20), Modifier.weight(1f))
        SummaryCard("مكتملة", completed, Color(0xFFE1F8EC), Color(0xFF07883F), Modifier.weight(1f))
    }
}

@Composable
private fun SummaryCard(
    label: String,
    value: Int,
    background: Color,
    foreground: Color,
    modifier: Modifier
) {
    Surface(
        modifier = modifier.height(72.dp).border(1.dp, foreground.copy(alpha = 0.12f), RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(17.dp),
        color = background
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                NumberFormatUtils.number(value),
                color = foreground,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black
            )
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
        Icon(
            Icons.Default.EventNote,
            null,
            tint = Color(0xFFCBD5E1),
            modifier = Modifier.size(54.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text("لا توجد مهام", fontWeight = FontWeight.Bold, color = Color(0xFF334155))
        Text("ابدأ بإضافة مهمة جديدة", color = Color(0xFF94A3B8), fontSize = 12.sp)
        TextButton(onClick = onAdd) { Text("إضافة الآن") }
    }
}

@Composable
private fun BottomAddBar(onAdd: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth().navigationBarsPadding(),
        color = Color(0xFFF2F4FF),
        shadowElevation = 10.dp
    ) {
        Button(
            onClick = onAdd,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 11.dp)
                .height(54.dp),
            shape = RoundedCornerShape(18.dp),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = Color(0xFF5138EE),
                contentColor = Color.White
            )
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
    val now = System.currentTimeMillis()
    val overdue = !completed && task.dueAtMillis < now
    val overdueText = if (overdue) overdueDaysLabel(task.dueAtMillis, now) else null
    val priorityColor = when (task.priority.id) {
        TaskPriority.URGENT.id -> Color(0xFFDC2626)
        TaskPriority.MEDIUM.id -> Color(0xFFEA580C)
        else -> Color(0xFF64748B)
    }
    var menuOpen by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().border(
            1.dp,
            if (overdue) Color(0xFFFCA5A5) else priorityColor.copy(alpha = 0.18f),
            RoundedCornerShape(20.dp)
        ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(46.dp).clickable { onToggle(task) },
                shape = RoundedCornerShape(15.dp),
                color = when {
                    completed -> Color(0xFFDDF8E9)
                    overdue -> Color(0xFFFFECEA)
                    else -> Color(0xFFE9EDFF)
                }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.CheckCircle,
                        if (completed) "إعادة فتح" else "إنجاز",
                        tint = when {
                            completed -> Color(0xFF07883F)
                            overdue -> Color(0xFFD92D20)
                            else -> Color(0xFF5B4BE8)
                        }
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
                overdueText?.let { label ->
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = Color(0xFFFEE2E2)
                    ) {
                        Text(
                            label,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            color = Color(0xFFB91C1C),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
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
                        leadingIcon = {
                            Icon(Icons.Default.Delete, null, tint = Color(0xFFDC2626))
                        },
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

private fun overdueDaysLabel(dueAtMillis: Long, nowMillis: Long): String? {
    if (dueAtMillis >= nowMillis) return null

    val dueDay = Calendar.getInstance().apply {
        timeInMillis = dueAtMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val today = Calendar.getInstance().apply {
        timeInMillis = nowMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    var days = 0
    while (dueDay.before(today)) {
        dueDay.add(Calendar.DAY_OF_YEAR, 1)
        days++
    }

    return when (days) {
        0 -> "متأخرة اليوم"
        1 -> "متأخرة يومًا واحدًا"
        2 -> "متأخرة يومين"
        in 3..10 -> "متأخرة ${NumberFormatUtils.number(days)} أيام"
        else -> "متأخرة ${NumberFormatUtils.number(days)} يومًا"
    }
}
