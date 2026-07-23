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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.FileProvider
import java.io.File

class MainActivity : ComponentActivity() {
    private var refreshTick by mutableIntStateOf(0)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        AppLog.write(this, "NOTIFICATION_PERMISSION_RESULT", "granted=$granted")
        refreshTick++
    }

    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        runCatching {
            val text = contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                ?: error("تعذر قراءة الملف")
            TaskRepository.importJson(this, text).getOrThrow()
        }.onSuccess { count ->
            AlarmScheduler.restoreAll(this, "backup import")
            Toast.makeText(this, "تم استيراد ${count} مهمة", Toast.LENGTH_LONG).show()
            refreshTick++
        }.onFailure {
            AppLog.write(this, "BACKUP_IMPORT_FAILED", it.message.orEmpty())
            Toast.makeText(this, "فشل الاستيراد: ${it.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AlarmNotification.ensureChannel(this)
        AlarmScheduler.restoreAll(this, "app opened")
        AppLog.write(this, "MAIN_ACTIVITY_CREATED", "sdk=${Build.VERSION.SDK_INT} version=${BuildConfig.VERSION_NAME}")

        setContent {
            androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme(
                    colorScheme = lightColorScheme(
                        primary = Color(0xFF4F46E5),
                        onPrimary = Color.White,
                        secondary = Color(0xFF0F766E),
                        background = Color(0xFFF4F7FB),
                        surface = Color.White,
                        error = Color(0xFFDC2626)
                    )
                ) {
                    SmartTasksRoot(
                        refreshTick = refreshTick,
                        onRefresh = { refreshTick++ },
                        onSaveTask = ::saveTask,
                        onDeleteTask = ::deleteTask,
                        onToggleTask = ::toggleTask,
                        onRequestNotifications = ::requestNotificationPermission,
                        onOpenExactAlarmSettings = ::openExactAlarmSettings,
                        onOpenFullScreenSettings = ::openFullScreenSettings,
                        onOpenNotificationSettings = ::openNotificationSettings,
                        onOpenBatterySettings = ::openBatterySettings,
                        onTestNow = { AlarmScheduler.fireNow(this) },
                        onTestAfter30 = {
                            val result = AlarmScheduler.scheduleTest(this)
                            Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
                        },
                        onShareBackup = ::shareBackup,
                        onImportBackup = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                        onCreateLocalBackup = ::createLocalBackup,
                        onRestoreLocalBackup = ::restoreLocalBackup,
                        onShareLog = ::shareLog,
                        onClearLog = {
                            AppLog.clear(this)
                            AppLog.write(this, "LOG_CLEARED")
                            refreshTick++
                        }
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

    private fun saveTask(task: TaskItem) {
        TaskRepository.save(this, task)
        val result = AlarmScheduler.scheduleTask(this, task)
        Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
        refreshTick++
    }

    private fun deleteTask(task: TaskItem) {
        AlarmScheduler.cancelTask(this, task.id)
        TaskRepository.delete(this, task.id)
        refreshTick++
    }

    private fun toggleTask(task: TaskItem) {
        val completed = task.status != TaskStatus.COMPLETED
        val updated = TaskRepository.markCompleted(this, task.id, completed) ?: return
        if (completed) AlarmScheduler.cancelTask(this, task.id) else AlarmScheduler.scheduleTask(this, updated)
        refreshTick++
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else openNotificationSettings()
    }

    private fun openExactAlarmSettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName"))
        } else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
        launchSettings(intent, "EXACT_ALARM_SETTINGS_OPENED")
    }

    private fun openFullScreenSettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:$packageName"))
        } else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
        launchSettings(intent, "FULL_SCREEN_SETTINGS_OPENED")
    }

    private fun openNotificationSettings() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        }
        launchSettings(intent, "NOTIFICATION_SETTINGS_OPENED")
    }

    private fun openBatterySettings() {
        launchSettings(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS), "BATTERY_SETTINGS_OPENED")
    }

    private fun launchSettings(intent: Intent, event: String) {
        runCatching { startActivity(intent) }
            .onSuccess { AppLog.write(this, event) }
            .onFailure {
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
                AppLog.write(this, "SETTINGS_FALLBACK_OPENED", it.message.orEmpty())
            }
    }

    private fun shareBackup() {
        runCatching {
            val directory = File(filesDir, "backups").apply { mkdirs() }
            val file = File(directory, "SmartTasks-Backup.json")
            file.writeText(TaskRepository.exportJson(this), Charsets.UTF_8)
            shareFile(file, "application/json", "نسخة احتياطية لمهامي")
            AppLog.write(this, "BACKUP_SHARE_OPENED")
        }.onFailure { Toast.makeText(this, "تعذرت مشاركة النسخة", Toast.LENGTH_LONG).show() }
    }

    private fun createLocalBackup() {
        runCatching { TaskRepository.createLocalBackup(this) }
            .onSuccess { Toast.makeText(this, "تم إنشاء نسخة احتياطية محلية", Toast.LENGTH_SHORT).show() }
            .onFailure { Toast.makeText(this, it.message, Toast.LENGTH_LONG).show() }
    }

    private fun restoreLocalBackup() {
        TaskRepository.restoreLocalBackup(this)
            .onSuccess {
                AlarmScheduler.restoreAll(this, "local backup restore")
                Toast.makeText(this, "تمت استعادة ${it} مهمة", Toast.LENGTH_LONG).show()
                refreshTick++
            }
            .onFailure { Toast.makeText(this, it.message, Toast.LENGTH_LONG).show() }
    }

    private fun shareLog() {
        runCatching { shareFile(AppLog.logFile(this), "text/plain", "سجل تشخيص Smart Tasks") }
            .onFailure { Toast.makeText(this, "تعذرت مشاركة السجل", Toast.LENGTH_LONG).show() }
    }

    private fun shareFile(file: File, mime: String, subject: String) {
        val uri = FileProvider.getUriForFile(this, "$packageName.files", file)
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }, "مشاركة"))
    }
}
