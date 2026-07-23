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
internal fun CalendarScreen(
    tasks: List<TaskItem>,
    onEdit: (TaskItem) -> Unit,
    onToggle: (TaskItem) -> Unit
) {
    val now = Calendar.getInstance()
    var year by remember { mutableIntStateOf(now.get(Calendar.YEAR)) }
    var month by remember { mutableIntStateOf(now.get(Calendar.MONTH)) }
    var selectedDay by remember { mutableIntStateOf(now.get(Calendar.DAY_OF_MONTH)) }
    val selectedMillis = remember(year, month, selectedDay) {
        NumberFormatUtils.withDateAndTime(year, month, selectedDay, 12, 0)
    }
    val monthCalendar = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val daysInMonth = monthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val offset = monthCalendar.get(Calendar.DAY_OF_WEEK) % 7
    val cells = List(offset) { 0 } + (1..daysInMonth).toList()
    val selectedTasks = tasks.filter { NumberFormatUtils.sameDay(it.dueAtMillis, selectedMillis) }
        .sortedBy { it.dueAtMillis }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            month--
                            if (month < 0) { month = 11; year-- }
                            selectedDay = 1
                        }) { Icon(Icons.Default.ArrowForward, "الشهر السابق") }
                        Text(
                            NumberFormatUtils.monthTitle(year, month),
                            modifier = Modifier.weight(1f),
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        IconButton(onClick = {
                            month++
                            if (month > 11) { month = 0; year++ }
                            selectedDay = 1
                        }) { Icon(Icons.Default.ArrowBack, "الشهر التالي") }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf("س", "ح", "ن", "ث", "ر", "خ", "ج").forEach {
                            Text(it, modifier = Modifier.weight(1f), color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    cells.chunked(7).forEach { week ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            repeat(7) { index ->
                                val day = week.getOrNull(index) ?: 0
                                val dayMillis = if (day > 0) NumberFormatUtils.withDateAndTime(year, month, day, 12, 0) else 0L
                                val hasTasks = day > 0 && tasks.any { NumberFormatUtils.sameDay(it.dueAtMillis, dayMillis) }
                                val selected = day == selectedDay
                                Box(
                                    modifier = Modifier.weight(1f).padding(vertical = 3.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (day > 0) {
                                        Column(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(CircleShape)
                                                .background(if (selected) Color(0xFF4F46E5) else Color.Transparent)
                                                .clickable { selectedDay = day },
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                day.toString(),
                                                color = if (selected) Color.White else Color(0xFF0F172A),
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                            )
                                            if (hasTasks) Box(Modifier.size(4.dp).clip(CircleShape).background(if (selected) Color.White else Color(0xFF4F46E5)))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        item { SectionHeader(NumberFormatUtils.formatWeekdayDate(selectedMillis), "${selectedTasks.size} مهمة") }
        if (selectedTasks.isEmpty()) {
            item { EmptyState("لا توجد مهام في هذا اليوم", "اختر يومًا آخر أو أضف مهمة جديدة") }
        } else {
            items(selectedTasks, key = { it.id }) { TaskCard(it, onEdit, {}, onToggle, compact = true) }
        }
    }
}
