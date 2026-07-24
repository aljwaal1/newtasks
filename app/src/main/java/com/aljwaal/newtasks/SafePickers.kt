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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.util.Calendar

@Composable
internal fun SafeDatePickerDialog(
    initialMillis: Long,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int, Int) -> Unit
) {
    val initial = remember(initialMillis) {
        Calendar.getInstance().apply { timeInMillis = initialMillis }
    }
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
                Text(
                    "الأرقام تظهر دائمًا بصيغة 0 1 2 3",
                    color = Color(0xFF64748B),
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = {
                        month--
                        if (month < 0) {
                            month = 11
                            year--
                        }
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
                        if (month > 11) {
                            month = 0
                            year++
                        }
                    }) { Icon(Icons.Default.ArrowBack, null) }
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("س", "ح", "ن", "ث", "ر", "خ", "ج").forEach {
                        Text(
                            it,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.height(5.dp))
                cells.chunked(7).forEach { week ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        repeat(7) { index ->
                            val day = week.getOrNull(index) ?: 0
                            Box(
                                modifier = Modifier.weight(1f).padding(2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (day > 0) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (day == safeSelectedDay) Color(0xFF4F46E5)
                                                else Color.Transparent
                                            )
                                            .clickable { selectedDay = day },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            day.toString(),
                                            color = if (day == safeSelectedDay) Color.White
                                            else Color(0xFF0F172A)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("إلغاء")
                    }
                    Button(
                        onClick = { onConfirm(year, month, safeSelectedDay) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("اختيار")
                    }
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
    val initial = remember(initialMillis) {
        Calendar.getInstance().apply { timeInMillis = initialMillis }
    }
    var hourText by remember(initialMillis) {
        mutableStateOf(NumberFormatUtils.twoDigits(initial.get(Calendar.HOUR_OF_DAY)))
    }
    var minuteText by remember(initialMillis) {
        mutableStateOf(NumberFormatUtils.twoDigits(initial.get(Calendar.MINUTE)))
    }
    val minuteFocus = remember { FocusRequester() }
    val hour = hourText.toIntOrNull()
    val minute = minuteText.toIntOrNull()
    val hourValid = hour != null && hour in 0..23
    val minuteValid = minute != null && minute in 0..59
    val valid = hourValid && minuteValid

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(26.dp), color = Color.White) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("إدخال الوقت", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(
                    "الدقائق أولًا بصريًا، ثم الساعة، مع عرض الوقت النهائي بصيغة الساعة:الدقائق.",
                    color = Color(0xFF64748B),
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = minuteText,
                        onValueChange = { minuteText = sanitizeTimePart(it) },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(minuteFocus)
                            .onFocusChanged { state ->
                                if (!state.isFocused && minuteValid) {
                                    minuteText = NumberFormatUtils.twoDigits(minute!!)
                                }
                            },
                        label = { Text("الدقائق") },
                        placeholder = { Text("00") },
                        singleLine = true,
                        isError = minuteText.isNotEmpty() && !minuteValid,
                        supportingText = {
                            Text(
                                if (minuteValid || minuteText.isEmpty()) "00–59"
                                else "دقائق غير صحيحة"
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                    Text(":", fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = hourText,
                        onValueChange = { input ->
                            hourText = sanitizeTimePart(input)
                            if (hourText.length == 2 && hourText.toIntOrNull() in 0..23) {
                                minuteFocus.requestFocus()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { state ->
                                if (!state.isFocused && hourValid) {
                                    hourText = NumberFormatUtils.twoDigits(hour!!)
                                }
                            },
                        label = { Text("الساعة") },
                        placeholder = { Text("00") },
                        singleLine = true,
                        isError = hourText.isNotEmpty() && !hourValid,
                        supportingText = {
                            Text(
                                if (hourValid || hourText.isEmpty()) "00–23"
                                else "ساعة غير صحيحة"
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { minuteFocus.requestFocus() }
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF2FF))
                ) {
                    Text(
                        if (valid) {
                            "${NumberFormatUtils.twoDigits(hour!!)}:${NumberFormatUtils.twoDigits(minute!!)}"
                        } else {
                            "--:--"
                        },
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        textAlign = TextAlign.Center,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF4338CA)
                    )
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("إلغاء")
                    }
                    Button(
                        onClick = { onConfirm(hour!!, minute!!) },
                        enabled = valid,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("اختيار")
                    }
                }
            }
        }
    }
}

private fun sanitizeTimePart(value: String): String =
    NumberFormatUtils.latinDigits(value)
        .filter { it.isDigit() }
        .take(2)

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

internal fun setTime(baseMillis: Long, hour: Int, minute: Int): Long =
    Calendar.getInstance().apply {
        timeInMillis = baseMillis
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
