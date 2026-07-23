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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.util.Calendar

@Composable
internal fun SafeDatePickerDialog(
    initialMillis: Long,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int, Int) -> Unit
) {
    val initial = remember(initialMillis) { Calendar.getInstance().apply { timeInMillis = initialMillis } }
    var year by remember { mutableIntStateOf(initial.get(Calendar.YEAR)) }
    var month by remember { mutableIntStateOf(initial.get(Calendar.MONTH)) }
    var selectedDay by remember { mutableIntStateOf(initial.get(Calendar.DAY_OF_MONTH)) }
    val monthCalendar = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val maxDay = monthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val safeSelectedDay = selectedDay.coerceAtMost(maxDay)
    val offset = monthCalendar.get(Calendar.DAY_OF_WEEK) % 7
    val cells = List(offset) { 0 } + (1..maxDay).toList()

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(26.dp), color = Color.White) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text("اختر التاريخ", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("الأرقام تظهر دائمًا بصيغة 0 1 2 3", color = Color(0xFF64748B), fontSize = 12.sp)
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = {
                        month--
                        if (month < 0) { month = 11; year-- }
                    }) { Icon(Icons.Default.ArrowForward, null) }
                    Text(
                        NumberFormatUtils.monthTitle(year, month),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    TextButton(onClick = {
                        month++
                        if (month > 11) { month = 0; year++ }
                    }) { Icon(Icons.Default.ArrowBack, null) }
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("س", "ح", "ن", "ث", "ر", "خ", "ج").forEach {
                        Text(it, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(5.dp))
                cells.chunked(7).forEach { week ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        repeat(7) { index ->
                            val day = week.getOrNull(index) ?: 0
                            Box(modifier = Modifier.weight(1f).padding(2.dp), contentAlignment = Alignment.Center) {
                                if (day > 0) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(if (day == safeSelectedDay) Color(0xFF4F46E5) else Color.Transparent)
                                            .clickable { selectedDay = day },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(day.toString(), color = if (day == safeSelectedDay) Color.White else Color(0xFF0F172A))
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("إلغاء") }
                    Button(onClick = { onConfirm(year, month, safeSelectedDay) }, modifier = Modifier.weight(1f)) { Text("اختيار") }
                }
            }
        }
    }
}

@Composable
internal fun SafeTimePickerDialog(
    initialMillis: Long,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    val initial = remember(initialMillis) { Calendar.getInstance().apply { timeInMillis = initialMillis } }
    var hour by remember { mutableIntStateOf(initial.get(Calendar.HOUR_OF_DAY)) }
    var minute by remember { mutableIntStateOf((initial.get(Calendar.MINUTE) / 5) * 5) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            shape = RoundedCornerShape(26.dp),
            color = Color.White
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(18.dp)) {
                Text("اختر الوقت", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("لا توجد كتابة يدوية؛ اختر الساعة والدقيقة فقط.", color = Color(0xFF64748B), fontSize = 12.sp)
                Spacer(Modifier.height(14.dp))
                Text("الساعة", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                (0..23).chunked(6).forEach { rowHours ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        rowHours.forEach { item ->
                            TimeCell(
                                text = NumberFormatUtils.twoDigits(item),
                                selected = hour == item,
                                modifier = Modifier.weight(1f)
                            ) { hour = item }
                        }
                    }
                    Spacer(Modifier.height(5.dp))
                }
                Spacer(Modifier.height(10.dp))
                Text("الدقيقة", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                (0..55 step 5).toList().chunked(6).forEach { rowMinutes ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        rowMinutes.forEach { item ->
                            TimeCell(
                                text = NumberFormatUtils.twoDigits(item),
                                selected = minute == item,
                                modifier = Modifier.weight(1f)
                            ) { minute = item }
                        }
                    }
                    Spacer(Modifier.height(5.dp))
                }
                Spacer(Modifier.height(12.dp))
                Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF2FF))) {
                    Text(
                        "${NumberFormatUtils.twoDigits(hour)}:${NumberFormatUtils.twoDigits(minute)}",
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        textAlign = TextAlign.Center,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF4338CA)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("إلغاء") }
                    Button(onClick = { onConfirm(hour, minute) }, modifier = Modifier.weight(1f)) { Text("اختيار") }
                }
            }
        }
    }
}

@Composable
private fun TimeCell(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) Color(0xFF4F46E5) else Color(0xFFF8FAFC)
    ) {
        Text(
            text,
            modifier = Modifier.padding(vertical = 10.dp),
            textAlign = TextAlign.Center,
            color = if (selected) Color.White else Color(0xFF334155),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

internal fun setRelativeDate(baseMillis: Long, daysFromToday: Int): Long {
    val old = Calendar.getInstance().apply { timeInMillis = baseMillis }
    return Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, daysFromToday)
        set(Calendar.HOUR_OF_DAY, old.get(Calendar.HOUR_OF_DAY))
        set(Calendar.MINUTE, old.get(Calendar.MINUTE))
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

internal fun setTime(baseMillis: Long, hour: Int, minute: Int): Long = Calendar.getInstance().apply {
    timeInMillis = baseMillis
    set(Calendar.HOUR_OF_DAY, hour)
    set(Calendar.MINUTE, minute)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis
