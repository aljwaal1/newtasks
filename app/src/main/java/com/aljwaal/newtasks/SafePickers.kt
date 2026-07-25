package com.aljwaal.newtasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
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
                    }) { androidx.compose.material3.Icon(Icons.Default.ArrowForward, null) }
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
                    }) { androidx.compose.material3.Icon(Icons.Default.ArrowBack, null) }
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

private enum class TimePart {
    HOUR,
    MINUTE
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
    var selectedHour by remember(initialMillis) {
        mutableIntStateOf(initial.get(Calendar.HOUR_OF_DAY))
    }
    var selectedMinute by remember(initialMillis) {
        mutableIntStateOf(initial.get(Calendar.MINUTE))
    }
    var activePart by remember { mutableStateOf(TimePart.HOUR) }
    var enteredDigits by remember { mutableStateOf("") }

    fun resetEntry(part: TimePart) {
        activePart = part
        enteredDigits = ""
    }

    fun setActiveValue(value: Int) {
        when (activePart) {
            TimePart.HOUR -> selectedHour = value.coerceIn(0, 23)
            TimePart.MINUTE -> selectedMinute = value.coerceIn(0, 59)
        }
    }

    fun enterDigit(digit: Int) {
        val maximum = if (activePart == TimePart.HOUR) 23 else 59
        val candidateText = if (enteredDigits.length >= 2) {
            digit.toString()
        } else {
            enteredDigits + digit
        }
        val candidate = candidateText.toIntOrNull() ?: 0

        if (candidate <= maximum) {
            enteredDigits = candidateText
            setActiveValue(candidate)
            if (candidateText.length == 2) {
                if (activePart == TimePart.HOUR) {
                    activePart = TimePart.MINUTE
                }
                enteredDigits = ""
            }
        } else {
            enteredDigits = digit.toString()
            setActiveValue(digit)
        }
    }

    fun adjustActive(delta: Int) {
        enteredDigits = ""
        when (activePart) {
            TimePart.HOUR -> selectedHour = (selectedHour + delta + 24) % 24
            TimePart.MINUTE -> selectedMinute = (selectedMinute + delta + 60) % 60
        }
    }

    fun clearActive() {
        enteredDigits = ""
        setActiveValue(0)
    }

    fun backspaceActive() {
        if (enteredDigits.isEmpty()) {
            setActiveValue(0)
            return
        }
        enteredDigits = enteredDigits.dropLast(1)
        setActiveValue(enteredDigits.toIntOrNull() ?: 0)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(26.dp), color = Color.White) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("اختيار الوقت", fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Text(
                    "اختر الساعة أو الدقائق، ثم استخدم الأرقام أو زرّي + و−.",
                    color = Color(0xFF64748B),
                    fontSize = 12.sp
                )

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TimePartCard(
                            label = "الساعة",
                            value = selectedHour,
                            selected = activePart == TimePart.HOUR,
                            onSelect = { resetEntry(TimePart.HOUR) },
                            onMinus = {
                                resetEntry(TimePart.HOUR)
                                adjustActive(-1)
                            },
                            onPlus = {
                                resetEntry(TimePart.HOUR)
                                adjustActive(1)
                            },
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            ":",
                            fontSize = 31.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF334155)
                        )
                        TimePartCard(
                            label = "الدقائق",
                            value = selectedMinute,
                            selected = activePart == TimePart.MINUTE,
                            onSelect = { resetEntry(TimePart.MINUTE) },
                            onMinus = {
                                resetEntry(TimePart.MINUTE)
                                adjustActive(-1)
                            },
                            onPlus = {
                                resetEntry(TimePart.MINUTE)
                                adjustActive(1)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Text(
                    if (activePart == TimePart.HOUR) "أدخل الساعة من 00 إلى 23"
                    else "أدخل الدقائق من 00 إلى 59",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = Color(0xFF4338CA),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )

                NumericTimeKeypad(
                    onDigit = ::enterDigit,
                    onClear = ::clearActive,
                    onBackspace = ::backspaceActive,
                    onNext = {
                        if (activePart == TimePart.HOUR) resetEntry(TimePart.MINUTE)
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(0, 15, 30, 45).forEach { value ->
                        OutlinedButton(
                            onClick = {
                                selectedMinute = value
                                resetEntry(TimePart.MINUTE)
                            },
                            modifier = Modifier.weight(1f).height(38.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            Text(NumberFormatUtils.twoDigits(value), fontSize = 12.sp)
                        }
                    }
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF2FF))
                ) {
                    Text(
                        "${NumberFormatUtils.twoDigits(selectedHour)}:${NumberFormatUtils.twoDigits(selectedMinute)}",
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        textAlign = TextAlign.Center,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF4338CA)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("إلغاء")
                    }
                    Button(
                        onClick = { onConfirm(selectedHour, selectedMinute) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("اختيار الوقت")
                    }
                }
            }
        }
    }
}

@Composable
private fun TimePartCard(
    label: String,
    value: Int,
    selected: Boolean,
    onSelect: () -> Unit,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(onClick = onSelect),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) Color(0xFFE0E7FF) else Color(0xFFF8FAFC),
        border = androidx.compose.foundation.BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) Color(0xFF4F46E5) else Color(0xFFE2E8F0)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                label,
                color = if (selected) Color(0xFF4338CA) else Color(0xFF64748B),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                NumberFormatUtils.twoDigits(value),
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF0F172A)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = onMinus,
                    modifier = Modifier.weight(1f).height(34.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("−", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onPlus,
                    modifier = Modifier.weight(1f).height(34.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("+", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun NumericTimeKeypad(
    onDigit: (Int) -> Unit,
    onClear: () -> Unit,
    onBackspace: () -> Unit,
    onNext: () -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("C", "0", "⌫")
            ).forEach { rowValues ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    rowValues.forEach { key ->
                        OutlinedButton(
                            onClick = {
                                when (key) {
                                    "C" -> onClear()
                                    "⌫" -> onBackspace()
                                    else -> onDigit(key.toInt())
                                }
                            },
                            modifier = Modifier.weight(1f).height(43.dp),
                            contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(13.dp)
                        ) {
                            Text(
                                key,
                                fontSize = if (key.length == 1 && key[0].isDigit()) 19.sp else 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            TextButton(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth().height(34.dp)
            ) {
                Text("الانتقال إلى الدقائق")
            }
        }
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

internal fun setTime(baseMillis: Long, hour: Int, minute: Int): Long =
    Calendar.getInstance().apply {
        timeInMillis = baseMillis
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
