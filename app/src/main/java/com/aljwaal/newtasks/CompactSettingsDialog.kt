package com.aljwaal.newtasks

import android.os.Build
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class SettingsSection {
    ALERTS,
    PERMISSIONS,
    ORGANIZATION,
    DATA
}

@Composable
internal fun CompactSettingsDialog(
    refreshTick: Int,
    onDismiss: () -> Unit,
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
    var mode by remember { mutableStateOf(AppPreferences.alarmSoundMode(context)) }
    var permissions by remember { mutableStateOf(PermissionInspector.snapshot(context)) }
    var activeSection by remember { mutableStateOf<SettingsSection?>(SettingsSection.ALERTS) }
    var showCategories by remember { mutableStateOf(false) }
    var showPriorities by remember { mutableStateOf(false) }

    LaunchedEffect(refreshTick) {
        permissions = PermissionInspector.snapshot(context)
        mode = AppPreferences.alarmSoundMode(context)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(10.dp),
            shape = RoundedCornerShape(26.dp),
            color = Color(0xFFF8FAFC)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SettingsHeader(onDismiss)

                SettingsSectionCard(
                    icon = Icons.Default.Notifications,
                    title = "التنبيه والصوت",
                    description = "الصوت والاهتزاز واختبار التنبيه",
                    expanded = activeSection == SettingsSection.ALERTS,
                    onClick = {
                        activeSection = toggleSection(activeSection, SettingsSection.ALERTS)
                    }
                ) {
                    AlarmSettingsContent(
                        mode = mode,
                        onMode = {
                            mode = it
                            AppPreferences.saveAlarmSoundMode(context, it)
                        },
                        onTestNow = onTestNow,
                        onTestAfter30 = onTestAfter30
                    )
                }

                SettingsSectionCard(
                    icon = Icons.Default.Security,
                    title = "الصلاحيات",
                    description = "الإشعارات والمنبه الدقيق والبطارية",
                    expanded = activeSection == SettingsSection.PERMISSIONS,
                    onClick = {
                        activeSection = toggleSection(activeSection, SettingsSection.PERMISSIONS)
                    }
                ) {
                    PermissionsContent(
                        permissions = permissions,
                        onRequestNotifications = onRequestNotifications,
                        onOpenExactAlarmSettings = onOpenExactAlarmSettings,
                        onOpenFullScreenSettings = onOpenFullScreenSettings,
                        onOpenNotificationSettings = onOpenNotificationSettings,
                        onOpenBatterySettings = onOpenBatterySettings
                    )
                }

                SettingsSectionCard(
                    icon = Icons.Default.Tune,
                    title = "التنظيم",
                    description = "التصنيفات والأولويات",
                    expanded = activeSection == SettingsSection.ORGANIZATION,
                    onClick = {
                        activeSection = toggleSection(activeSection, SettingsSection.ORGANIZATION)
                    }
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CompactAction(
                            Icons.Default.Category,
                            "التصنيفات",
                            { showCategories = true },
                            Modifier.weight(1f)
                        )
                        CompactAction(
                            Icons.Default.PriorityHigh,
                            "الأولويات",
                            { showPriorities = true },
                            Modifier.weight(1f)
                        )
                    }
                }

                SettingsSectionCard(
                    icon = Icons.Default.Storage,
                    title = "البيانات والسجل",
                    description = "النسخ الاحتياطي والاستيراد والتشخيص",
                    expanded = activeSection == SettingsSection.DATA,
                    onClick = {
                        activeSection = toggleSection(activeSection, SettingsSection.DATA)
                    }
                ) {
                    DataSettingsContent(
                        onShareBackup = onShareBackup,
                        onImportBackup = onImportBackup,
                        onCreateLocalBackup = onCreateLocalBackup,
                        onRestoreLocalBackup = onRestoreLocalBackup,
                        onShareLog = onShareLog,
                        onClearLog = onClearLog
                    )
                }

                Spacer(Modifier.weight(1f))
                Text(
                    "Smart Tasks ${BuildConfig.VERSION_NAME} • Android 5.0 فأعلى",
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp
                )
            }
        }
    }

    if (showCategories) {
        CategoryManagerDialog(
            onDismiss = { showCategories = false },
            onChanged = onRefresh
        )
    }
    if (showPriorities) {
        PriorityManagerDialog(
            onDismiss = { showPriorities = false },
            onChanged = onRefresh
        )
    }
}

private fun toggleSection(
    current: SettingsSection?,
    requested: SettingsSection
): SettingsSection? = if (current == requested) null else requested

@Composable
private fun SettingsHeader(onDismiss: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.size(45.dp),
            shape = RoundedCornerShape(15.dp),
            color = Color(0xFFE0E7FF)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Settings, null, tint = Color(0xFF4338CA))
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("الإعدادات", fontSize = 23.sp, fontWeight = FontWeight.Black)
            Text("اختر القسم الذي تحتاجه فقط", color = Color(0xFF64748B), fontSize = 11.sp)
        }
        IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, "إغلاق")
        }
    }
}

@Composable
private fun SettingsSectionCard(
    icon: ImageVector,
    title: String,
    description: String,
    expanded: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = if (expanded) 2.dp else 0.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(38.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = if (expanded) Color(0xFFE0E7FF) else Color(0xFFF1F5F9)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            icon,
                            null,
                            modifier = Modifier.size(19.dp),
                            tint = if (expanded) Color(0xFF4338CA) else Color(0xFF64748B)
                        )
                    }
                }
                Spacer(Modifier.width(9.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        description,
                        color = Color(0xFF64748B),
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null,
                    tint = Color(0xFF94A3B8)
                )
            }
            if (expanded) {
                HorizontalDivider(color = Color(0xFFF1F5F9))
                Column(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun AlarmSettingsContent(
    mode: AlarmSoundMode,
    onMode: (AlarmSoundMode) -> Unit,
    onTestNow: () -> Unit,
    onTestAfter30: () -> Unit
) {
    AlarmSoundMode.entries.forEach { item ->
        SoundModeRow(
            mode = item,
            selected = mode == item,
            onClick = { onMode(item) }
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CompactAction(
            Icons.Default.Alarm,
            "اختبار الآن",
            onTestNow,
            Modifier.weight(1f),
            primary = true
        )
        CompactAction(
            Icons.Default.Timer,
            "بعد 30 ثانية",
            onTestAfter30,
            Modifier.weight(1f)
        )
    }
}

@Composable
private fun PermissionsContent(
    permissions: PermissionSnapshot,
    onRequestNotifications: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onOpenFullScreenSettings: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        PermissionTile(
            "الإشعارات",
            permissions.notifications,
            if (Build.VERSION.SDK_INT >= 33 && !permissions.notifications) {
                onRequestNotifications
            } else {
                onOpenNotificationSettings
            },
            Modifier.weight(1f)
        )
        PermissionTile(
            "المنبه الدقيق",
            permissions.exactAlarms,
            onOpenExactAlarmSettings,
            Modifier.weight(1f)
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        PermissionTile(
            "الشاشة المنبثقة",
            permissions.fullScreen,
            onOpenFullScreenSettings,
            Modifier.weight(1f)
        )
        PermissionTile(
            "البطارية",
            permissions.batteryUnrestricted,
            onOpenBatterySettings,
            Modifier.weight(1f)
        )
    }
}

@Composable
private fun DataSettingsContent(
    onShareBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onCreateLocalBackup: () -> Unit,
    onRestoreLocalBackup: () -> Unit,
    onShareLog: () -> Unit,
    onClearLog: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CompactAction(
            Icons.Default.Backup,
            "نسخة محلية",
            onCreateLocalBackup,
            Modifier.weight(1f)
        )
        CompactAction(
            Icons.Default.Restore,
            "استعادة",
            onRestoreLocalBackup,
            Modifier.weight(1f)
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CompactAction(
            Icons.Default.Share,
            "مشاركة JSON",
            onShareBackup,
            Modifier.weight(1f)
        )
        CompactAction(
            Icons.Default.ImportExport,
            "استيراد",
            onImportBackup,
            Modifier.weight(1f)
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CompactAction(
            Icons.Default.Share,
            "مشاركة السجل",
            onShareLog,
            Modifier.weight(1f)
        )
        CompactAction(
            Icons.Default.Delete,
            "مسح السجل",
            onClearLog,
            Modifier.weight(1f)
        )
    }
}

@Composable
private fun SoundModeRow(
    mode: AlarmSoundMode,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(13.dp),
        color = if (selected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = onClick)
            Column(modifier = Modifier.weight(1f)) {
                Text(mode.label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    mode.description,
                    color = Color(0xFF64748B),
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun PermissionTile(
    label: String,
    granted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier
) {
    Surface(
        modifier = modifier.height(54.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(13.dp),
        color = if (granted) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Notifications,
                null,
                modifier = Modifier.size(18.dp),
                tint = if (granted) Color(0xFF15803D) else Color(0xFFDC2626)
            )
            Spacer(Modifier.width(6.dp))
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CompactAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier,
    primary: Boolean = false
) {
    if (primary) {
        Button(
            onClick = onClick,
            modifier = modifier.height(42.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 7.dp),
            shape = RoundedCornerShape(13.dp)
        ) {
            Icon(icon, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(label, fontSize = 11.sp, maxLines = 1)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(42.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 7.dp),
            shape = RoundedCornerShape(13.dp)
        ) {
            Icon(icon, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(label, fontSize = 11.sp, maxLines = 1)
        }
    }
}

@Composable
private fun CategoryManagerDialog(
    onDismiss: () -> Unit,
    onChanged: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var values by remember { mutableStateOf<List<String>>(emptyList()) }
    var newValue by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        values = withContext(Dispatchers.IO) { TaskRepository.categories(context) }
    }

    ManageValuesDialog(
        title = "التصنيفات",
        values = values,
        newValue = newValue,
        onNewValue = { newValue = NumberFormatUtils.latinDigits(it) },
        onAdd = {
            val value = newValue.trim()
            if (value.isBlank()) return@ManageValuesDialog
            scope.launch {
                values = withContext(Dispatchers.IO) {
                    TaskRepository.addCategory(context, value)
                    TaskRepository.categories(context)
                }
                newValue = ""
                onChanged()
            }
        },
        canDelete = { it !in defaultCategoryNames },
        onDelete = { value ->
            scope.launch {
                values = withContext(Dispatchers.IO) {
                    TaskRepository.deleteCategory(context, value)
                    TaskRepository.categories(context)
                }
                onChanged()
            }
        },
        onDismiss = onDismiss
    )
}

@Composable
private fun PriorityManagerDialog(
    onDismiss: () -> Unit,
    onChanged: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var priorities by remember { mutableStateOf(TaskPriority.defaults) }
    var newValue by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        priorities = withContext(Dispatchers.IO) { TaskRepository.priorities(context) }
    }

    ManageValuesDialog(
        title = "الأولويات",
        values = priorities.map { it.label },
        newValue = newValue,
        onNewValue = { newValue = NumberFormatUtils.latinDigits(it) },
        onAdd = {
            val value = newValue.trim()
            if (value.isBlank()) return@ManageValuesDialog
            scope.launch {
                withContext(Dispatchers.IO) { TaskRepository.addPriority(context, value) }
                priorities = withContext(Dispatchers.IO) { TaskRepository.priorities(context) }
                newValue = ""
                onChanged()
            }
        },
        canDelete = { label -> priorities.firstOrNull { it.label == label }?.isDefault == false },
        onDelete = { label ->
            val id = priorities.firstOrNull { it.label == label }?.id ?: return@ManageValuesDialog
            scope.launch {
                withContext(Dispatchers.IO) { TaskRepository.deletePriority(context, id) }
                priorities = withContext(Dispatchers.IO) { TaskRepository.priorities(context) }
                onChanged()
            }
        },
        onDismiss = onDismiss
    )
}

@Composable
private fun ManageValuesDialog(
    title: String,
    values: List<String>,
    newValue: String,
    onNewValue: (String) -> Unit,
    onAdd: () -> Unit,
    canDelete: (String) -> Boolean,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().height(460.dp),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        title,
                        modifier = Modifier.weight(1f),
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newValue,
                        onValueChange = onNewValue,
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text("إضافة") }
                    )
                    IconButton(onClick = onAdd) { Icon(Icons.Default.Add, null) }
                }
                Spacer(Modifier.height(7.dp))
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(values, key = { it }) { value ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(value, modifier = Modifier.weight(1f))
                            if (canDelete(value)) {
                                IconButton(onClick = { onDelete(value) }) {
                                    Icon(Icons.Default.Delete, null, tint = Color(0xFFDC2626))
                                }
                            }
                        }
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                    }
                }
            }
        }
    }
}

private val defaultCategoryNames = setOf(
    "عام", "العمل", "المنزل", "الدراسة", "الصحة", "المواعيد"
)
