package com.aljwaal.newtasks

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    val scope = rememberCoroutineScope()
    var permissions by remember { mutableStateOf(PermissionInspector.snapshot(context)) }
    var categories by remember { mutableStateOf<List<String>>(emptyList()) }
    var priorities by remember { mutableStateOf(TaskPriority.defaults) }
    var newCategory by remember { mutableStateOf("") }
    var newPriority by remember { mutableStateOf("") }
    var priorityMessage by remember { mutableStateOf<String?>(null) }
    var logText by remember { mutableStateOf("جاري تحميل سجل التشخيص...") }

    LaunchedEffect(refreshTick) {
        permissions = PermissionInspector.snapshot(context)
        val snapshot = withContext(Dispatchers.IO) {
            Triple(
                TaskRepository.categories(context),
                TaskRepository.priorities(context),
                AppLog.readTail(context, 12_000)
            )
        }
        categories = snapshot.first
        priorities = snapshot.second
        logText = snapshot.third
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { SectionHeader("جاهزية التنبيهات", "افحص الصلاحيات المهمة") }
        item {
            PermissionRow(
                "الإشعارات",
                permissions.notifications,
                "ضبط",
                if (Build.VERSION.SDK_INT >= 33 && !permissions.notifications) {
                    onRequestNotifications
                } else {
                    onOpenNotificationSettings
                }
            )
        }
        item {
            PermissionRow(
                "المنبهات الدقيقة",
                permissions.exactAlarms,
                "منح",
                onOpenExactAlarmSettings
            )
        }
        item {
            PermissionRow(
                "الشاشة الكاملة",
                permissions.fullScreen,
                "فتح",
                onOpenFullScreenSettings
            )
        }
        item {
            PermissionRow(
                "قناة التنبيه",
                permissions.alarmChannel,
                "ضبط",
                onOpenNotificationSettings
            )
        }
        item {
            PermissionRow(
                "قيود البطارية",
                permissions.batteryUnrestricted,
                "فتح",
                onOpenBatterySettings
            )
        }
        item {
            OutlinedButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Refresh, null)
                Text("  تحديث حالة الصلاحيات")
            }
        }

        item { SectionHeader("اختبار المحرك", "تأكد من التنبيه قبل الاعتماد عليه") }
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onTestNow,
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Icon(Icons.Default.NotificationsActive, null)
                        Text("  تجربة التنبيه الآن")
                    }
                    OutlinedButton(
                        onClick = onTestAfter30,
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Icon(Icons.Default.Timer, null)
                        Text("  تجربة بعد 30 ثانية")
                    }
                }
            }
        }

        item { SectionHeader("التصنيفات", "أنشئ تصنيفات تناسبك") }
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newCategory,
                            onValueChange = {
                                newCategory = NumberFormatUtils.latinDigits(it)
                            },
                            modifier = Modifier.weight(1f),
                            label = { Text("تصنيف جديد") },
                            singleLine = true
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                val value = NumberFormatUtils.latinDigits(newCategory).trim()
                                if (value.isBlank()) return@IconButton
                                scope.launch {
                                    val updated = withContext(Dispatchers.IO) {
                                        TaskRepository.addCategory(context, value)
                                        TaskRepository.categories(context)
                                    }
                                    newCategory = ""
                                    categories = updated
                                    onRefresh()
                                }
                            }
                        ) {
                            Icon(Icons.Default.Add, null, tint = Color(0xFF4F46E5))
                        }
                    }
                    categories.forEach { category ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(category, modifier = Modifier.weight(1f))
                            if (category !in DEFAULT_CATEGORIES) {
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            categories = withContext(Dispatchers.IO) {
                                                TaskRepository.deleteCategory(context, category)
                                                TaskRepository.categories(context)
                                            }
                                            onRefresh()
                                        }
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        null,
                                        tint = Color(0xFFDC2626)
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                    }
                }
            }
        }

        item { SectionHeader("الأولويات", "الافتراضي: عادية، متوسطة، عاجلة") }
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "حالة المهمة منفصلة وتبقى فقط: قيد التنفيذ أو مكتملة.",
                        color = Color(0xFF64748B),
                        fontSize = 12.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newPriority,
                            onValueChange = {
                                newPriority = NumberFormatUtils.latinDigits(it)
                                priorityMessage = null
                            },
                            modifier = Modifier.weight(1f),
                            label = { Text("أولوية إضافية") },
                            singleLine = true
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                val value = NumberFormatUtils.latinDigits(newPriority).trim()
                                if (value.isBlank()) return@IconButton
                                scope.launch {
                                    val added = withContext(Dispatchers.IO) {
                                        TaskRepository.addPriority(context, value)
                                    }
                                    if (added) {
                                        newPriority = ""
                                        priorities = withContext(Dispatchers.IO) {
                                            TaskRepository.priorities(context)
                                        }
                                        priorityMessage = "تمت إضافة الأولوية."
                                        onRefresh()
                                    } else {
                                        priorityMessage = "الاسم موجود أو غير صالح."
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Add, null, tint = Color(0xFF4F46E5))
                        }
                    }
                    priorities.forEach { priority ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                priority.label,
                                modifier = Modifier.weight(1f),
                                fontWeight = if (priority.isDefault) FontWeight.Bold else FontWeight.Normal
                            )
                            if (!priority.isDefault) {
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            val deleted = withContext(Dispatchers.IO) {
                                                TaskRepository.deletePriority(context, priority.id)
                                            }
                                            if (deleted) {
                                                priorities = withContext(Dispatchers.IO) {
                                                    TaskRepository.priorities(context)
                                                }
                                                priorityMessage = "تم حذف الأولوية."
                                                onRefresh()
                                            } else {
                                                priorityMessage = "لا يمكن حذف أولوية مستخدمة في مهمة."
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        null,
                                        tint = Color(0xFFDC2626)
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                    }
                    priorityMessage?.let {
                        Text(
                            it,
                            color = if (it.startsWith("تم")) Color(0xFF047857) else Color(0xFFB91C1C),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        item { SectionHeader("النسخ الاحتياطي", "بياناتك محلية ويمكن نقلها") }
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    SettingsAction(Icons.Default.Backup, "إنشاء نسخة محلية", onCreateLocalBackup)
                    SettingsAction(Icons.Default.Restore, "استعادة آخر نسخة محلية", onRestoreLocalBackup)
                    SettingsAction(Icons.Default.Share, "مشاركة نسخة JSON", onShareBackup)
                    SettingsAction(Icons.Default.ImportExport, "استيراد نسخة من الجهاز", onImportBackup)
                }
            }
        }

        item { SectionHeader("التشخيص", "السجل يوضح كل مرحلة من التنبيه") }
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        logText,
                        color = Color(0xFFCBD5E1),
                        fontSize = 11.sp,
                        lineHeight = 17.sp,
                        maxLines = 24,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onShareLog,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, null)
                            Text(" مشاركة")
                        }
                        OutlinedButton(
                            onClick = onClearLog,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Delete, null)
                            Text(" مسح")
                        }
                    }
                }
            }
        }
        item {
            Text(
                "Smart Tasks ${BuildConfig.VERSION_NAME} • Android 5.0 فأعلى",
                color = Color(0xFF94A3B8),
                fontSize = 12.sp
            )
            Spacer(Modifier.height(70.dp))
        }
    }
}

private val DEFAULT_CATEGORIES = setOf(
    "عام",
    "العمل",
    "المنزل",
    "الدراسة",
    "الصحة",
    "المواعيد"
)
