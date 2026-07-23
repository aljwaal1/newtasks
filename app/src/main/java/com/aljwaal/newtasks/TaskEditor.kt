package com.aljwaal.newtasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.util.Calendar

@Composable
fun TaskEditorDialog(
    existing: TaskItem?,
    categories: List<String>,
    onDismiss: () -> Unit,
    onSave: (TaskItem) -> Unit
) {
    val initial = remember(existing) {
        existing?.dueAtMillis ?: Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, 1)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    var title by remember(existing) { mutableStateOf(existing?.title.orEmpty()) }
    var notes by remember(existing) { mutableStateOf(existing?.notes.orEmpty()) }
    var category by remember(existing) { mutableStateOf(existing?.category ?: categories.firstOrNull() ?: "عام") }
    var priority by remember(existing) { mutableStateOf(existing?.priority ?: TaskPriority.NORMAL) }
    var repeatRule by remember(existing) { mutableStateOf(existing?.repeatRule ?: RepeatRule.NONE) }
    var reminderEnabled by remember(existing) { mutableStateOf(existing?.reminderEnabled ?: true) }
    var dueAt by remember(existing) { mutableStateOf(initial) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 24.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFFF8FAFC)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(13.dp)
            ) {
                Text(
                    if (existing == null) "إضافة مهمة جديدة" else "تعديل المهمة",
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Text("اختر التاريخ والوقت من القوائم لتجنب أي خطأ.", color = Color(0xFF64748B), fontSize = 13.sp)

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; error = null },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("عنوان المهمة *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    shape = RoundedCornerShape(16.dp)
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("ملاحظات") },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(16.dp)
                )

                Text("اختيار سريع للتاريخ", fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickDateButton("اليوم") { dueAt = setRelativeDate(dueAt, 0) }
                    QuickDateButton("غدًا") { dueAt = setRelativeDate(dueAt, 1) }
                    QuickDateButton("بعد أسبوع") { dueAt = setRelativeDate(dueAt, 7) }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DateTimeField(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.CalendarMonth,
                        label = "التاريخ",
                        value = NumberFormatUtils.formatDate(dueAt),
                        onClick = { showDatePicker = true }
                    )
                    DateTimeField(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Schedule,
                        label = "الوقت",
                        value = NumberFormatUtils.formatTime(dueAt),
                        onClick = { showTimePicker = true }
                    )
                }

                Text("أوقات سريعة", fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(9 to 0, 14 to 0, 18 to 0, 21 to 0).forEach { (hour, minute) ->
                        QuickDateButton("${NumberFormatUtils.twoDigits(hour)}:${NumberFormatUtils.twoDigits(minute)}") {
                            dueAt = setTime(dueAt, hour, minute)
                        }
                    }
                }

                SelectorField("التصنيف", category, categories) { category = it }
                SelectorField("الأولوية", priority.label, TaskPriority.entries.map { it.label }) { label ->
                    priority = TaskPriority.entries.first { it.label == label }
                }
                SelectorField("التكرار", repeatRule.label, RepeatRule.entries.map { it.label }) { label ->
                    repeatRule = RepeatRule.entries.first { it.label == label }
                }

                Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("تفعيل التنبيه", fontWeight = FontWeight.Bold)
                            Text("إظهار شاشة تنبيه مع صوت واهتزاز", color = Color(0xFF64748B), fontSize = 12.sp)
                        }
                        Switch(checked = reminderEnabled, onCheckedChange = { reminderEnabled = it })
                    }
                }

                error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold) }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(16.dp)) {
                        Text("إلغاء")
                    }
                    Button(
                        onClick = {
                            when {
                                title.trim().isEmpty() -> error = "اكتب عنوان المهمة."
                                reminderEnabled && dueAt <= System.currentTimeMillis() && existing == null -> error = "اختر موعدًا قادمًا للتنبيه."
                                else -> onSave(
                                    (existing ?: TaskItem(title = title.trim(), dueAtMillis = dueAt)).copy(
                                        title = title.trim(),
                                        notes = notes.trim(),
                                        category = category.ifBlank { "عام" },
                                        dueAtMillis = dueAt,
                                        priority = priority,
                                        repeatRule = repeatRule,
                                        reminderEnabled = reminderEnabled,
                                        status = existing?.status ?: TaskStatus.PENDING
                                    )
                                )
                            }
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) { Text("حفظ المهمة", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }

    if (showDatePicker) {
        SafeDatePickerDialog(
            initialMillis = dueAt,
            onDismiss = { showDatePicker = false },
            onConfirm = { year, month, day ->
                val old = Calendar.getInstance().apply { timeInMillis = dueAt }
                dueAt = NumberFormatUtils.withDateAndTime(
                    year, month, day,
                    old.get(Calendar.HOUR_OF_DAY), old.get(Calendar.MINUTE)
                )
                showDatePicker = false
            }
        )
    }
    if (showTimePicker) {
        SafeTimePickerDialog(
            initialMillis = dueAt,
            onDismiss = { showTimePicker = false },
            onConfirm = { hour, minute ->
                dueAt = setTime(dueAt, hour, minute)
                showTimePicker = false
            }
        )
    }
}

@Composable
private fun DateTimeField(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = Color(0xFF4F46E5), modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(6.dp))
                Text(label, color = Color(0xFF64748B), fontSize = 12.sp)
            }
            Spacer(Modifier.height(5.dp))
            Text(value, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 15.sp)
        }
    }
}

@Composable
private fun QuickDateButton(text: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clip(RoundedCornerShape(50)).clickable(onClick = onClick),
        shape = RoundedCornerShape(50),
        color = Color(0xFFEEF2FF)
    ) {
        Text(text, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = Color(0xFF4338CA), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SelectorField(label: String, value: String, options: List<String>, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Card(
            modifier = Modifier.fillMaxWidth().clickable { expanded = true },
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(label, color = Color(0xFF64748B), fontSize = 12.sp)
                    Text(value, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                }
                Text("▼", color = Color(0xFF94A3B8))
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.distinct().forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = { onSelected(option); expanded = false })
            }
        }
    }
}
