package com.aljwaal.newtasks

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.content.ContextCompat

class AlarmService : Service() {

    override fun onCreate() {
        super.onCreate()
        AppLog.write(this, "ALARM_SERVICE_CREATED")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val taskId = intent?.getStringExtra(AlarmScheduler.EXTRA_TASK_ID).orEmpty()
        val title = intent?.getStringExtra(AlarmScheduler.EXTRA_TITLE) ?: "حان موعد المهمة"
        val notes = intent?.getStringExtra(AlarmScheduler.EXTRA_NOTES).orEmpty()
        val kind = intent?.getStringExtra(AlarmScheduler.EXTRA_KIND) ?: AlarmScheduler.KIND_TEST
        val launchScreen = intent?.getBooleanExtra(EXTRA_LAUNCH_SCREEN, true) != false

        AppLog.write(
            this,
            "ALARM_SERVICE_STARTED",
            "kind=$kind task=$taskId launchScreen=$launchScreen"
        )

        val foregroundStarted = runCatching {
            startForeground(
                AlarmNotification.NOTIFICATION_ID,
                AlarmNotification.build(this, taskId, title, notes, kind)
            )
            AppLog.write(this, "NOTIFICATION_POSTED", "fullScreenIntent=true")
            true
        }.getOrElse { error ->
            AppLog.write(
                this,
                "FOREGROUND_PROMOTION_FAILED",
                "${error.javaClass.simpleName}: ${error.message}"
            )
            false
        }

        if (!foregroundStarted) {
            // حتى إذا منع النظام الخدمة، حاول إظهار واجهة المنبه والإشعار العادي.
            AlarmNotification.post(this, taskId, title, notes, kind)
            AlarmActivityLauncher.launch(
                context = this,
                requestCode = stableRequestCode("promotion-fallback:$kind:$taskId"),
                taskId = taskId,
                title = title,
                notes = notes,
                kind = kind
            )
            stopSelf(startId)
            return START_NOT_STICKY
        }

        AlarmPlayer.start(this)

        if (launchScreen) {
            val launched = AlarmActivityLauncher.launch(
                context = this,
                requestCode = stableRequestCode("service:$kind:$taskId"),
                taskId = taskId,
                title = title,
                notes = notes,
                kind = kind
            )
            AppLog.write(this, "ALARM_SCREEN_LAUNCH_RESULT", "success=$launched")
        } else {
            AppLog.write(this, "ALARM_SCREEN_LAUNCH_SKIPPED", "activityAlreadyVisible=true")
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        AlarmPlayer.stop(this)
        AppLog.write(this, "ALARM_SERVICE_DESTROYED")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val EXTRA_LAUNCH_SCREEN = "launch_alarm_screen"

        fun start(
            context: Context,
            taskId: String,
            title: String,
            notes: String,
            kind: String,
            launchScreen: Boolean = true
        ) {
            val intent = Intent(context, AlarmService::class.java).apply {
                putExtra(AlarmScheduler.EXTRA_TASK_ID, taskId)
                putExtra(AlarmScheduler.EXTRA_TITLE, title)
                putExtra(AlarmScheduler.EXTRA_NOTES, notes)
                putExtra(AlarmScheduler.EXTRA_KIND, kind)
                putExtra(EXTRA_LAUNCH_SCREEN, launchScreen)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            AlarmPlayer.stop(context)
            AlarmNotification.cancel(context)
            context.stopService(Intent(context, AlarmService::class.java))
            AppLog.write(context, "ALARM_STOP_REQUESTED")
        }

        private fun stableRequestCode(value: String): Int =
            50_000 + (value.hashCode().toLong() and 0x7FFFFFFFL).rem(800_000L).toInt()
    }
}
