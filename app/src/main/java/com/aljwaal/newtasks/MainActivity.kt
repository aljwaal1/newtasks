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
import androidx.lifecycle.lifecycleScope
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val text = contentResolver.openInputStream(uri)
                        ?.bufferedReader(Charsets.UTF_8)
                        ?.use { it.readText() }
                        ?: error("تعذر قراءة الملف")
                    val count = TaskRepository.importJson(this@MainActivity, text).getOrThrow()
                    AlarmScheduler.restoreAll(this@MainActivity, "backup import")
                    count
                }
            }
            result.onSuccess { count ->
                Toast.makeText(
                    this@MainActivity,
                    "تم استيراد ${NumberFormatUtils.number(count)} مهمة",
                    Toast.LENGTH_LONG
                ).show()
                refreshTick++
            }.onFailure { error ->
                AppLog.write(this@MainActivity, "BACKUP_IMPORT_FAILED", error.message.orEmpty())
                Toast.makeText(
                    this@MainActivity,
                    "فشل الاستيراد: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
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
                        onImportBackup = {
                            importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                        },
                        onCreateLocalBackup = ::createLocalBackup,
                        onRestoreLocalBackup = ::restoreLocalBackup,
                        onShareLog = ::shareLog,
                        onClearLog = {
                            lifecycleScope.launch(Dispatchers.IO) {
                                AppLog.clear(this@MainActivity)
                                AppLog.write(this@MainActivity, "LOG_CLEARED")
                            }
                            refreshTick++
                        }
                    )
                }
            }
        }

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                AlarmScheduler.restoreAll(this@MainActivity, "app opened")
            }
            refreshTick++
        }
    }

    override fun onResume() {
        super.onResume()
        refreshTick++
        AppLog.write(this, "MAIN_ACTIVITY_RESUMED")
    }

    private fun saveTask(task: TaskItem) {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    TaskRepository.save(this@MainActivity, task)
                    AlarmScheduler.scheduleTask(this@MainActivity, task)
                }
            }
            result.onSuccess { scheduleResult ->
                Toast.makeText(this@MainActivity, scheduleResult.message, Toast.LENGTH_SHORT).show()
                refreshTick++
            }.onFailure { error ->
                AppLog.write(this@MainActivity, "TASK_SAVE_FAILED", error.message.orEmpty())
                Toast.makeText(
                    this@MainActivity,
                    "تعذر حفظ المهمة: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun deleteTask(task: TaskItem) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                AlarmScheduler.cancelTask(this@MainActivity, task.id)
                TaskRepository.delete(this@MainActivity, task.id)
            }
            refreshTick++
        }
    }

    private fun toggleTask(task: TaskItem) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val completed = task.status != TaskStatus.COMPLETED
                val updated = TaskRepository.markCompleted(
                    this@MainActivity,
                    task.id,
                    completed
                ) ?: return@withContext
                if (completed) {
                    AlarmScheduler.cancelTask(this@MainActivity, task.id)
                } else {
                    AlarmScheduler.scheduleTask(this@MainActivity, updated)
                }
            }
            refreshTick++
        }
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

    private fun launchSettings(intent: Intent, event: String) {
        runCatching { startActivity(intent) }
            .onSuccess { AppLog.write(this, event) }
            .onFailure { error ->
                runCatching {
                    startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:$packageName")
                        )
                    )
                }
                AppLog.write(this, "SETTINGS_FALLBACK_OPENED", error.message.orEmpty())
            }
    }

    private fun shareBackup() {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val directory = File(filesDir, "backups").apply { mkdirs() }
                    File(directory, "SmartTasks-Backup.json").apply {
                        writeText(TaskRepository.exportJson(this@MainActivity), Charsets.UTF_8)
                    }
                }
            }
            result.onSuccess { file ->
                shareFile(file, "application/json", "نسخة احتياطية لمهامي")
                AppLog.write(this@MainActivity, "BACKUP_SHARE_OPENED")
            }.onFailure {
                Toast.makeText(this@MainActivity, "تعذرت مشاركة النسخة", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun createLocalBackup() {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { TaskRepository.createLocalBackup(this@MainActivity) }
            }
            result.onSuccess {
                Toast.makeText(
                    this@MainActivity,
                    "تم إنشاء نسخة احتياطية محلية",
                    Toast.LENGTH_SHORT
                ).show()
            }.onFailure { error ->
                Toast.makeText(this@MainActivity, error.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun restoreLocalBackup() {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                TaskRepository.restoreLocalBackup(this@MainActivity).map { count ->
                    AlarmScheduler.restoreAll(this@MainActivity, "local backup restore")
                    count
                }
            }
            result.onSuccess { count ->
                Toast.makeText(
                    this@MainActivity,
                    "تمت استعادة ${NumberFormatUtils.number(count)} مهمة",
                    Toast.LENGTH_LONG
                ).show()
                refreshTick++
            }.onFailure { error ->
                Toast.makeText(this@MainActivity, error.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun shareLog() {
        runCatching {
            shareFile(AppLog.logFile(this), "text/plain", "سجل تشخيص Smart Tasks")
        }.onFailure {
            Toast.makeText(this, "تعذرت مشاركة السجل", Toast.LENGTH_LONG).show()
        }
    }

    private fun shareFile(file: File, mime: String, subject: String) {
        val uri = FileProvider.getUriForFile(this, "$packageName.files", file)
        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = mime
                    putExtra(Intent.EXTRA_SUBJECT, subject)
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                "مشاركة"
            )
        )
    }
}
