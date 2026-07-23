package com.aljwaal.newtasks

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessAlarm
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider

class MainActivity : ComponentActivity() {
    private var refreshTick by mutableIntStateOf(0)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        AppLog.write(this, "NOTIFICATION_PERMISSION_RESULT", "granted=$granted")
        refreshTick++
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AlarmNotification.ensureChannel(this)
        AppLog.write(
            this,
            "MAIN_ACTIVITY_CREATED",
            "sdk=${Build.VERSION.SDK_INT} version=${BuildConfig.VERSION_NAME}"
        )

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
                    MainScreen(
                        refreshTick = refreshTick,
                        requestNotifications = ::requestNotificationPermission,
                        openExactAlarmSettings = ::openExactAlarmSettings,
                        openFullScreenSettings = ::openFullScreenSettings,
                        openNotificationSettings = ::openNotificationSettings,
                        openBatterySettings = ::openBatterySettings,
                        scheduleDaily = ::scheduleDaily,
                        cancelDaily = ::cancelDaily,
                        testNow = ::testNow,
                        testAfter30Seconds = ::testAfter30Seconds,
                        shareLog = ::shareLog,
                        clearLog = ::clearLog,
                        refresh = { refreshTick++ }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshTick++
        AppLog.write(this, "MAIN_ACTIVITY_RESUMED")
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            openNotificationSettings()
        }
    }

    private fun openExactAlarmSettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(
                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                Uri.parse("package:$packageName")
            )
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
        }
        launchSettings(intent, "EXACT_ALARM_SETTINGS_OPENED")
    }

    private fun openFullScreenSettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Intent(
                Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                Uri.parse("package:$packageName")
            )
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
        }
        launchSettings(intent, "FULL_SCREEN_SETTINGS_OPENED")
    }

    private fun openNotificationSettings() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        }
        launchSettings(intent, "NOTIFICATION_SETTINGS_OPENED")
    }

    private fun openBatterySettings() {
        launchSettings(
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
            "BATTERY_SETTINGS_OPENED"
        )
    }

    private fun launchSettings(intent: Intent, logEvent: String) {
        runCatching {
            startActivity(intent)
            AppLog.write(this, logEvent)
        }.onFailure {
            AppLog.write(this, "SETTINGS_OPEN_FAILED", "${it.javaClass.simpleName}: ${it.message}")
            Toast.makeText(this, "تعذر فتح الإعداد المطلوب", Toast.LENGTH_LONG).show()
        }
    }

    private fun scheduleDaily(hour: Int, minute: Int) {
        val result = AlarmScheduler.scheduleDaily(this, hour, minute)
        Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
        refreshTick++
    }

    private fun cancelDaily() {
        AlarmScheduler.cancelDaily(this)
        Toast.makeText(this, "تم إلغاء الموعد اليومي", Toast.LENGTH_SHORT).show()
        refreshTick++
    }

    private fun testNow() {
        AlarmScheduler.fireNow(this)
        Toast.makeText(this, "تم إرسال اختبار التنبيه الفوري", Toast.LENGTH_SHORT).show()
    }

    private fun testAfter30Seconds() {
        val result = AlarmScheduler.scheduleTest(this)
        Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
        refreshTick++
    }

    private fun clearLog() {
        AppLog.clear(this)
        AppLog.write(this, "LOG_CLEARED")
        refreshTick++
    }

    private fun shareLog() {
        runCatching {
            val file = AppLog.logFile(this)
            val uri = FileProvider.getUriForFile(this, "$packageName.files", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Smart Tasks Alarm diagnostic log")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "مشاركة سجل التشخيص"))
            AppLog.write(this, "LOG_SHARE_OPENED")
        }.onFailure {
            AppLog.write(this, "LOG_SHARE_FAILED", it.message.orEmpty())
            Toast.makeText(this, "تعذرت مشاركة السجل", Toast.LENGTH_LONG).show()
        }
    }
}

@Composable
private fun MainScreen(
    refreshTick: Int,
    requestNotifications: () -> Unit,
    openExactAlarmSettings: () -> Unit,
    openFullScreenSettings: () -> Unit,
    openNotificationSettings: () -> Unit,
    openBatterySettings: () -> Unit,
    scheduleDaily: (Int, Int) -> Unit,
    cancelDaily: () -> Unit,
    testNow: () -> Unit,
    testAfter30Seconds: () -> Unit,
    shareLog: () -> Unit,
    clearLog: () -> Unit,
    refresh: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var permissions by remember { mutableStateOf(PermissionInspector.snapshot(context)) }
    var logText by remember { mutableStateOf(AppLog.readTail(context)) }
    var timeText by remember {
        mutableStateOf(
            "%02d:%02d".format(
                AppPreferences.dailyHour(context),
                AppPreferences.dailyMinute(context)
            )
        )
    }
    var inputError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(refreshTick) {
        AlarmNotification.ensureChannel(context)
        permissions = PermissionInspector.snapshot(context)
        logText = AppLog.readTail(context)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HeaderCard()

            SectionTitle("جاهزية التنبيه", "يجب أن تكون الحالات الأساسية خضراء")
            PermissionStatusCard(
                title = "الإشعارات",
                description = if (permissions.notifications) "مسموح بعرض الإشعارات" else "الإشعارات محظورة أو لم تُمنح",
                granted = permissions.notifications,
                buttonLabel = "ضبط الإشعارات",
                onClick = if (Build.VERSION.SDK_INT >= 33 && !permissions.notifications) requestNotifications else openNotificationSettings
            )
            PermissionStatusCard(
                title = "المنبهات الدقيقة",
                description = if (permissions.exactAlarms) "يمكن تثبيت الموعد بدقة" else "سيكون الموعد تقريبيًا حتى منح الصلاحية",
                granted = permissions.exactAlarms,
                buttonLabel = "منح الصلاحية",
                onClick = openExactAlarmSettings
            )
            PermissionStatusCard(
                title = "الظهور بملء الشاشة",
                description = if (permissions.fullScreen) "مسموح بعرض شاشة المنبه" else "قد يظهر إشعار فقط بدل شاشة كاملة",
                granted = permissions.fullScreen,
                buttonLabel = "فتح الإعداد",
                onClick = openFullScreenSettings
            )
            PermissionStatusCard(
                title = "قناة التنبيه",
                description = if (permissions.alarmChannel) "قناة التنبيه العاجل مفعلة" else "قناة التنبيه محظورة من إعدادات النظام",
                granted = permissions.alarmChannel,
                buttonLabel = "إعدادات الإشعار",
                onClick = openNotificationSettings
            )
            PermissionStatusCard(
                title = "قيود البطارية",
                description = if (permissions.batteryUnrestricted) "التطبيق غير مقيّد بالبطارية" else "بعض الهواتف قد تؤخر التنبيه بسبب توفير الطاقة",
                granted = permissions.batteryUnrestricted,
                buttonLabel = "إعدادات البطارية",
                onClick = openBatterySettings
            )

            SectionTitle("اختبار التنبيه", "اختبر المحرك قبل الاعتماد على الموعد اليومي")
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = testNow,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null)
                        Text("  تجربة التنبيه الآن", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = testAfter30Seconds,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Timer, contentDescription = null)
                        Text("  تجربة بعد 30 ثانية")
                    }
                }
            }

            SectionTitle("الموعد اليومي", "يفتح التنبيه يوميًا في الوقت المحدد")
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = timeText,
                        onValueChange = {
                            if (it.length <= 5) timeText = it
                            inputError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("الوقت بصيغة 24 ساعة") },
                        placeholder = { Text("مثال: 14:30") },
                        leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null) },
                        singleLine = true,
                        isError = inputError != null,
                        supportingText = { inputError?.let { Text(it) } }
                    )
                    Text(
                        text = if (AppPreferences.isDailyEnabled(context)) {
                            "الموعد اليومي مفعّل — الموعد القادم: ${AlarmScheduler.format(AppPreferences.lastTrigger(context))}"
                        } else {
                            "الموعد اليومي غير مفعّل حاليًا"
                        },
                        color = Color(0xFF475569),
                        fontSize = 14.sp
                    )
                    Button(
                        onClick = {
                            val match = Regex("^([01]\\d|2[0-3]):([0-5]\\d)$").matchEntire(timeText)
                            if (match == null) {
                                inputError = "اكتب وقتًا صحيحًا مثل 09:30 أو 14:05"
                            } else {
                                scheduleDaily(
                                    match.groupValues[1].toInt(),
                                    match.groupValues[2].toInt()
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E))
                    ) {
                        Icon(Icons.Default.AccessAlarm, contentDescription = null)
                        Text("  تثبيت الموعد اليومي")
                    }
                    OutlinedButton(
                        onClick = cancelDaily,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Cancel, contentDescription = null)
                        Text("  إلغاء الموعد اليومي")
                    }
                }
            }

            SectionTitle("سجل التشخيص", "يعرض المسار الكامل للتنبيه والأخطاء")
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SelectionContainer {
                        Text(
                            text = logText,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF020617), RoundedCornerShape(14.dp))
                                .padding(12.dp),
                            color = Color(0xFFE2E8F0),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(onClick = refresh, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Text(" تحديث")
                        }
                        OutlinedButton(onClick = shareLog, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Text(" مشاركة")
                        }
                    }
                    OutlinedButton(onClick = clearLog, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.BugReport, contentDescription = null)
                        Text(" مسح السجل وبدء اختبار جديد")
                    }
                }
            }

            Text(
                text = "الإصدار ${BuildConfig.VERSION_NAME} — Android SDK ${Build.VERSION.SDK_INT}",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                textAlign = TextAlign.Center,
                color = Color(0xFF64748B),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun HeaderCard() {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF312E81))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .background(Color(0x334F46E5), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AccessAlarm,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "منبه المهام الذكي",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "تنبيه دقيق، شاشة كاملة، صوت واهتزاز، ولوج تشخيصي واضح",
                    color = Color(0xFFC7D2FE),
                    lineHeight = 21.sp
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(top = 4.dp)) {
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
        Text(subtitle, fontSize = 13.sp, color = Color(0xFF64748B))
    }
}

@Composable
private fun PermissionStatusCard(
    title: String,
    description: String,
    granted: Boolean,
    buttonLabel: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (granted) Color(0xFFF0FDFA) else Color(0xFFFFF7ED)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = if (granted) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = null,
                    tint = if (granted) Color(0xFF0F766E) else Color(0xFFEA580C)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    Text(description, fontSize = 13.sp, color = Color(0xFF475569))
                }
                Icon(
                    Icons.Default.Security,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8)
                )
            }
            if (!granted) {
                OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
                    Text(buttonLabel)
                }
            }
        }
    }
}
