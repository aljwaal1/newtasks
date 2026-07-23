package com.aljwaal.newtasks

import android.content.Intent
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.runtime.mutableStateOf
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
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlarmActivity : ComponentActivity() {
    private var alarmTitle by mutableStateOf("حان موعد المهمة")
    private var alarmKind by mutableStateOf(AlarmScheduler.KIND_TEST)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        )
        readIntent(intent)
        AppLog.write(this, "ALARM_ACTIVITY_OPENED", "kind=$alarmKind title=$alarmTitle")

        setContent {
            androidx.compose.runtime.CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Rtl
            ) {
                MaterialTheme(
                    colorScheme = lightColorScheme(
                        primary = Color(0xFF4F46E5),
                        secondary = Color(0xFF0F766E),
                        background = Color(0xFFF8FAFC),
                        surface = Color.White,
                        error = Color(0xFFDC2626)
                    )
                ) {
                    AlarmScreen(
                        title = alarmTitle,
                        onDone = {
                            AppLog.write(this, "ALARM_DONE_FROM_SCREEN")
                            AlarmService.stop(this)
                            finishAndRemoveTask()
                        },
                        onSnooze5 = { snooze(5) },
                        onSnooze10 = { snooze(10) },
                        onStopSound = {
                            AppLog.write(this, "ALARM_SOUND_STOPPED_FROM_SCREEN")
                            AlarmPlayer.stop(this)
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readIntent(intent)
        AppLog.write(this, "ALARM_ACTIVITY_NEW_INTENT", "kind=$alarmKind title=$alarmTitle")
    }

    private fun readIntent(intent: Intent?) {
        alarmTitle = intent?.getStringExtra(AlarmScheduler.EXTRA_TITLE) ?: "حان موعد المهمة"
        alarmKind = intent?.getStringExtra(AlarmScheduler.EXTRA_KIND) ?: AlarmScheduler.KIND_TEST
    }

    private fun snooze(minutes: Int) {
        val result = AlarmScheduler.scheduleSnooze(this, minutes)
        AppLog.write(this, "ALARM_SNOOZED_FROM_SCREEN", "minutes=$minutes success=${result.success}")
        AlarmService.stop(this)
        finishAndRemoveTask()
    }
}

@Composable
private fun AlarmScreen(
    title: String,
    onDone: () -> Unit,
    onSnooze5: () -> Unit,
    onSnooze10: () -> Unit,
    onStopSound: () -> Unit
) {
    BackHandler(enabled = true) { }
    var now by mutableStateOf(Date())
    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            delay(1_000)
        }
    }

    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
    val date = SimpleDateFormat("EEEE، d MMMM", Locale("ar")).format(now)

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFFE0E7FF), RoundedCornerShape(28.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = null,
                        tint = Color(0xFF4338CA),
                        modifier = Modifier.padding(4.dp)
                    )
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "تنبيه مهمة",
                    fontSize = 18.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = time,
                    fontSize = 68.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF0F172A)
                )
                Text(text = date, fontSize = 17.sp, color = Color(0xFF64748B))
                Spacer(Modifier.height(22.dp))
                Text(
                    text = title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(24.dp))
                        .padding(horizontal = 22.dp, vertical = 26.dp),
                    textAlign = TextAlign.Center,
                    fontSize = 25.sp,
                    lineHeight = 35.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onDone,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E))
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Text("  تم الإنجاز", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onSnooze5,
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Snooze, contentDescription = null)
                        Text("  5 دقائق")
                    }
                    OutlinedButton(
                        onClick = onSnooze10,
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Snooze, contentDescription = null)
                        Text("  10 دقائق")
                    }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onStopSound,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.VolumeOff, contentDescription = null)
                    Text("  إيقاف الصوت فقط")
                }
            }
        }
    }
}
