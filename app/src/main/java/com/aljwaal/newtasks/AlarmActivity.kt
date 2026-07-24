package com.aljwaal.newtasks

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AlarmActivity : ComponentActivity() {
    private var taskId by mutableStateOf("")
    private var alarmTitle by mutableStateOf("حان موعد المهمة")
    private var alarmNotes by mutableStateOf("")
    private var alarmKind = AlarmScheduler.KIND_TEST

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAlarmWindow()
        readIntent(intent)
        ensureAlarmServiceOnlyWhenMissing()
        AppLog.write(this, "ALARM_ACTIVITY_OPENED", "task=$taskId title=$alarmTitle")
        render()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readIntent(intent)
        ensureAlarmServiceOnlyWhenMissing()
        AppLog.write(this, "ALARM_ACTIVITY_NEW_INTENT", "task=$taskId title=$alarmTitle")
    }

    private fun render() {
        setContent {
            androidx.compose.runtime.CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Rtl
            ) {
                MaterialTheme(
                    colorScheme = lightColorScheme(
                        primary = Color(0xFF4F46E5),
                        secondary = Color(0xFF0F766E),
                        background = Color(0xFFF4F7FB),
                        surface = Color.White,
                        error = Color(0xFFDC2626)
                    )
                ) {
                    AlarmScreen(
                        title = alarmTitle,
                        notes = alarmNotes,
                        onStopAlarm = ::stopAlarm,
                        onDone = ::complete,
                        onSnooze5 = { snooze(5) },
                        onSnooze10 = { snooze(10) }
                    )
                }
            }
        }
    }

    private fun configureAlarmWindow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        )
    }

    private fun readIntent(intent: Intent?) {
        taskId = intent?.getStringExtra(AlarmScheduler.EXTRA_TASK_ID).orEmpty()
        alarmTitle = NumberFormatUtils.latinDigits(
            intent?.getStringExtra(AlarmScheduler.EXTRA_TITLE) ?: "حان موعد المهمة"
        )
        alarmNotes = NumberFormatUtils.latinDigits(
            intent?.getStringExtra(AlarmScheduler.EXTRA_NOTES).orEmpty()
        )
        alarmKind = intent?.getStringExtra(AlarmScheduler.EXTRA_KIND)
            ?: AlarmScheduler.KIND_TEST
    }

    private fun ensureAlarmServiceOnlyWhenMissing() {
        if (AlarmService.isRunning()) {
            AppLog.write(this, "ALARM_ACTIVITY_SERVICE_START_SKIPPED", "alreadyRunning=true")
            return
        }
        runCatching {
            AlarmService.start(
                context = this,
                taskId = taskId,
                title = alarmTitle,
                notes = alarmNotes,
                kind = alarmKind,
                launchScreen = false
            )
        }.onFailure { error ->
            AppLog.write(
                this,
                "ALARM_ACTIVITY_SERVICE_START_FAILED",
                "${error.javaClass.simpleName}: ${error.message}"
            )
        }
    }

    private fun stopAlarm() {
        AlarmService.stop(this)
        AppLog.write(this, "ALARM_STOPPED_FROM_SCREEN", "task=$taskId")
        finishAndRemoveTask()
    }

    private fun complete() {
        val currentTaskId = taskId
        AlarmService.stop(this)
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                if (currentTaskId.isNotBlank()) {
                    val task = TaskRepository.get(this@AlarmActivity, currentTaskId)
                    if (task?.repeatRule == RepeatRule.NONE) {
                        TaskRepository.markCompleted(this@AlarmActivity, currentTaskId, true)
                        AlarmScheduler.cancelTask(this@AlarmActivity, currentTaskId)
                    } else {
                        AppLog.write(
                            this@AlarmActivity,
                            "REPEATING_OCCURRENCE_COMPLETED",
                            "task=$currentTaskId next=${task?.dueAtMillis}"
                        )
                    }
                }
                AppLog.write(
                    this@AlarmActivity,
                    "ALARM_DONE_FROM_SCREEN",
                    "task=$currentTaskId"
                )
            }
            finishAndRemoveTask()
        }
    }

    private fun snooze(minutes: Int) {
        val currentTaskId = taskId
        val currentTitle = alarmTitle
        val currentNotes = alarmNotes
        AlarmService.stop(this)
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                AlarmScheduler.scheduleSnooze(
                    this@AlarmActivity,
                    currentTaskId,
                    currentTitle,
                    currentNotes,
                    minutes
                )
                AppLog.write(
                    this@AlarmActivity,
                    "ALARM_SNOOZED_FROM_SCREEN",
                    "task=$currentTaskId minutes=$minutes"
                )
            }
            finishAndRemoveTask()
        }
    }
}

@Composable
private fun AlarmScreen(
    title: String,
    notes: String,
    onStopAlarm: () -> Unit,
    onDone: () -> Unit,
    onSnooze5: () -> Unit,
    onSnooze10: () -> Unit
) {
    BackHandler(enabled = true) { onStopAlarm() }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (currentCoroutineContext().isActive) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(
            modifier = Modifier.fillMaxSize().padding(18.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFFE0E7FF), RoundedCornerShape(26.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Alarm, null, tint = Color(0xFF4338CA))
                }
                Spacer(Modifier.height(12.dp))
                Text("تنبيه مهمة", color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
                Text(
                    NumberFormatUtils.formatTime(now),
                    fontSize = 58.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF0F172A)
                )
                Text(NumberFormatUtils.formatWeekdayDate(now), color = Color(0xFF64748B))
                Spacer(Modifier.height(14.dp))
                AlarmContent(title, notes)
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onStopAlarm,
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Icon(Icons.Default.VolumeOff, null)
                    Text("  إيقاف التنبيه فورًا", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onDone,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E))
                ) {
                    Icon(Icons.Default.CheckCircle, null)
                    Text("  تم الإنجاز", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SnoozeButton("5 دقائق", onSnooze5, Modifier.weight(1f))
                    SnoozeButton("10 دقائق", onSnooze10, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun AlarmContent(title: String, notes: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(22.dp))
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            title,
            textAlign = TextAlign.Center,
            fontSize = 23.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A)
        )
        if (notes.isNotBlank()) {
            Spacer(Modifier.height(7.dp))
            Text(
                notes,
                textAlign = TextAlign.Center,
                color = Color(0xFF64748B),
                lineHeight = 21.sp,
                maxLines = 3
            )
        }
    }
}

@Composable
private fun SnoozeButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(15.dp)
    ) {
        Icon(Icons.Default.Snooze, null)
        Text("  $label")
    }
}
