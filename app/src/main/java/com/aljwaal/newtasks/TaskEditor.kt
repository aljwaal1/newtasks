package com.aljwaal.newtasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.util.Calendar

@Composable
fun TaskEditorDialog(
    existing: TaskItem?,
    categories: List<String>,
    priorities: List<TaskPriority>,
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
    var title by remember(existing) {
        mutableStateOf(NumberFormatUtils.latinDigits(existing?.title.orEmpty()))
    }
    var notes by remember(existing) {
        mutableStateOf(NumberFormatUtils.latinDigits(existing?.notes.orEmpty()))
    }
    var category by remember(existing) {
        mutableStateOf(existing?.category ?: categories.firstOrNull() ?: "عام")
    }
    var priority by remember(existing, priorities) {
        mutableStateOf(
            existing?.priority
                ?: priorities.firstOrNull { it.id == TaskPriority.NORMAL.id }
                ?: TaskPriority.NORMAL
        )
    }
    var repeatRule by remember(existing) {
        mutableStateOf(existing?.repeatRule ?: RepeatRule.NONE)
    }
    var reminderEnabled by remember(existing) {
        mutableStateOf(existing?.reminderEnabled ?: true)
    }
    var dueAt by remember(existing) { mutableStateOf(initial) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFFF8FAFC)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (existing == null) "إضافة مهمة" else "تعديل المهمة",
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            "كل الحقول في نافذة واحدة",
                            color = Color(0xFF64748B),
                            fontSize = 10.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "إغلاق")
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = NumberFormatUtils.latinDigits(it)
                        error = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("عنوان المهمة *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                    shape = RoundedCornerShape(14.dp)
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = NumberFormatUtils.latinDigits(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("ملاحظات") },
                    minLines = 1,
                    maxLines = 2,
                    shape = RoundedCornerShape(14.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    QuickDateButton("اليوم", Modifier.weight(1f)) {
                        dueAt = setRelativeDate(dueAt, 0)
                    }
                    QuickDateButton("غدًا", Modifier.weight(1f)) {
                        dueAt = setRelativeDate(dueAt, 1)
                    }
                    QuickDateButton("بعد أسبوع", Modifier.weight(1f)) {
                        dueAt = setRelativeDate(dueAt, 7)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
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

                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    SelectorField(
                        modifier = Modifier.weight(1f),
                        label = "التصنيف",
                        value = category,
                        options = categories,
                        onSelected = { category = NumberFormatUtils.latinDigits(it) }
                    )
                    SelectorField(
                        modifier = Modifier.weight(1f),
                        label = "الأولوية",
                        value = priority.label,
                        options = priorities.map { it.label },
                        onSelected = { label ->
                            priority = priorities.firstOrNull { it.label == label }
                                ?: TaskPriority.NORMAL
                        }
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    SelectorField(
                        modifier = Modifier.weight(1f),
                        label = "التكرار",
                        value = repeatRule.label,
                        options = RepeatRule.entries.map { it.label },
                        onSelected = { label ->
                            repeatRule = RepeatRule.entries.first { it.label == label }
                        }
                    )
                    Card(
                        modifier = Modifier.weight(1f).height(58.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("التنبيه", fontSize = 10.sp, color = Color(0xFF64748B))
                                Text(
                                    if (reminderEnabled) "مفعّل" else "متوقف",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            Switch(
                                checked = reminderEnabled,
                                onCheckedChange = {
                                    reminderEnabled = it
                                    error = null
                                }
                            )
                        }
                    }
                }

                error?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("إلغاء")
                    }
                    Button(
                        onClick = {
                            val cleanTitle = NumberFormatUtils.latinDigits(title).trim()
                            val cleanNotes = NumberFormatUtils.latinDigits(notes).trim()
                            val cleanCategory = NumberFormatUtils.latinDigits(category).trim()
                                .ifBlank { "عام" }
                            when {
                                cleanTitle.isEmpty() -> error = "اكتب عنوان المهمة."
                                reminderEnabled && dueAt <= System.currentTimeMillis() + 1_000L ->
                                    error = "اختر موعدًا قادمًا للتنبيه."
                                else -> onSave(
                                    (existing ?: TaskItem(
                                        title = cleanTitle,
                                        dueAtMillis = dueAt
                                    )).copy(
                                        title = cleanTitle,
                                        notes = cleanNotes,
                                        category = cleanCategory,
                                        dueAtMillis = dueAt,
                                        priority = priority,
                                        repeatRule = repeatRule,
                                        reminderEnabled = reminderEnabled,
                                        status = existing?.status ?: TaskStatus.PENDING
                                    )
                                )
                            }
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("حفظ المهمة", fontWeight = FontWeight.Bold)
                    }
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
                    year,
                    month,
                    day,
                    old.get(Calendar.HOUR_OF_DAY),
                    old.get(Calendar.MINUTE)
                )
                showDatePicker = false
                error = null
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
                error = null
            }
        )
    }
}

@Composable
private fun DateTimeField(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.height(58.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = Color(0xFF4F46E5), modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, color = Color(0xFF64748B), fontSize = 10.sp)
                Text(
                    value,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun QuickDateButton(
    text: String,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.height(34.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFEEF2FF)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text,
                color = Color(0xFF4338CA),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun SelectorField(
    modifier: Modifier,
    label: String,
    value: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Card(
            modifier = Modifier.fillMaxWidth().height(58.dp).clickable { expanded = true },
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(label, color = Color(0xFF64748B), fontSize = 10.sp)
                    Text(
                        value,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text("▼", color = Color(0xFF94A3B8), fontSize = 10.sp)
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.distinct().forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
