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
internal fun SettingsScreen(
    refreshTick: Int,
    onRefresh: () -> Unit,
    onRequestNotifications: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onOpenFullScreenSettings: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onTestNow: () -> Unit,
    onTestAfter30: () -> Unit,
    onShareBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onCreateLocalBackup: () -> Unit,
    onRestoreLocalBackup: () -> Unit,
    onShareLog: () -> Unit,
    onClearLog: () -> Unit
) {
    val context = LocalContext.current
    var permissions by remember { mutableStateOf(PermissionInspector.snapshot(context)) }
    var categories by remember { mutableStateOf(TaskRepository.categories(context)) }
    var newCategory by remember { mutableStateOf("") }
    var logText by remember { mutableStateOf(AppLog.readTail(context, 12_000)) }

    LaunchedEffect(refreshTick) {
        permissions = PermissionInspector.snapshot(context)
        categories = TaskRepository.categories(context)
        logText = AppLog.readTail(context, 12_000)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { SectionHeader("جاهزية التنبيهات", "افحص الصلاحيات المهمة") }
        item { PermissionRow("الإشعارات", permissions.notifications, "ضبط", if (Build.VERSION.SDK_INT >= 33 && !permissions.notifications) onRequestNotifications else onOpenNotificationSettings) }
        item { PermissionRow("المنبهات الدقيقة", permissions.exactAlarms, "منح", onOpenExactAlarmSettings) }
        item { PermissionRow("الشاشة الكاملة", permissions.fullScreen, "فتح", onOpenFullScreenSettings) }
        item { PermissionRow("قناة التنبيه", permissions.alarmChannel, "ضبط", onOpenNotificationSettings) }
        item { PermissionRow("قيود البطارية", permissions.batteryUnrestricted, "فتح", onOpenBatterySettings) }
        item {
            OutlinedButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Refresh, null); Text("  تحديث حالة الصلاحيات")
            }
        }

        item { SectionHeader("اختبار المحرك", "تأكد من التنبيه قبل الاعتماد عليه") }
        item {
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = onTestNow, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                        Icon(Icons.Default.NotificationsActive, null); Text("  تجربة التنبيه الآن")
                    }
                    OutlinedButton(onClick = onTestAfter30, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                        Icon(Icons.Default.Timer, null); Text("  تجربة بعد 30 ثانية")
                    }
                }
            }
        }

        item { SectionHeader("التصنيفات", "أنشئ تصنيفات تناسبك") }
        item {
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newCategory,
                            onValueChange = { newCategory = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("تصنيف جديد") },
                            singleLine = true
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = {
                            if (TaskRepository.addCategory(context, newCategory)) {
                                newCategory = ""
                                categories = TaskRepository.categories(context)
                            }
                        }) { Icon(Icons.Default.Add, null, tint = Color(0xFF4F46E5)) }
                    }
                    categories.forEach { category ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(category, modifier = Modifier.weight(1f))
                            if (category !in listOf("عام", "العمل", "المنزل", "الدراسة", "الصحة", "المواعيد")) {
                                IconButton(onClick = {
                                    TaskRepository.deleteCategory(context, category)
                                    categories = TaskRepository.categories(context)
                                }) { Icon(Icons.Default.Delete, null, tint = Color(0xFFDC2626)) }
                            }
                        }
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                    }
                }
            }
        }

        item { SectionHeader("النسخ الاحتياطي", "بياناتك محلية ويمكن نقلها") }
        item {
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    SettingsAction(Icons.Default.Backup, "إنشاء نسخة محلية", onCreateLocalBackup)
                    SettingsAction(Icons.Default.Restore, "استعادة آخر نسخة محلية", onRestoreLocalBackup)
                    SettingsAction(Icons.Default.Share, "مشاركة نسخة JSON", onShareBackup)
                    SettingsAction(Icons.Default.ImportExport, "استيراد نسخة من الجهاز", onImportBackup)
                }
            }
        }

        item { SectionHeader("التشخيص", "السجل يوضح كل مرحلة من التنبيه") }
        item {
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(logText, color = Color(0xFFCBD5E1), fontSize = 11.sp, lineHeight = 17.sp, maxLines = 24, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onShareLog, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Share, null); Text(" مشاركة")
                        }
                        OutlinedButton(onClick = onClearLog, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Delete, null); Text(" مسح")
                        }
                    }
                }
            }
        }
        item {
            Text("Smart Tasks ${BuildConfig.VERSION_NAME} • Android 5.0 فأعلى", color = Color(0xFF94A3B8), fontSize = 12.sp)
            Spacer(Modifier.height(70.dp))
        }
    }
}
