package com.aljwaal.newtasks

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
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

        AppLog.write(this, "ALARM_SERVICE_STARTED", "kind=$kind task=$taskId")
        startForeground(
            AlarmNotification.NOTIFICATION_ID,
            AlarmNotification.build(this, taskId, title, notes, kind)
        )
        AppLog.write(this, "NOTIFICATION_POSTED", "fullScreenIntent=true")
        AlarmPlayer.start(this)

        val activityIntent = Intent(this, AlarmActivity::class.java).apply {
            data = Uri.parse("smarttasks://ring/$kind/${taskId.ifBlank { "test" }}")
            putExtra(AlarmScheduler.EXTRA_TASK_ID, taskId)
            putExtra(AlarmScheduler.EXTRA_TITLE, title)
            putExtra(AlarmScheduler.EXTRA_NOTES, notes)
            putExtra(AlarmScheduler.EXTRA_KIND, kind)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        runCatching {
            startActivity(activityIntent)
            AppLog.write(this, "FULL_SCREEN_START_REQUESTED", "direct=true")
        }.onFailure {
            AppLog.write(this, "FULL_SCREEN_START_FAILED", "${it.javaClass.simpleName}: ${it.message}")
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
        fun start(context: Context, taskId: String, title: String, notes: String, kind: String) {
            val intent = Intent(context, AlarmService::class.java).apply {
                putExtra(AlarmScheduler.EXTRA_TASK_ID, taskId)
                putExtra(AlarmScheduler.EXTRA_TITLE, title)
                putExtra(AlarmScheduler.EXTRA_NOTES, notes)
                putExtra(AlarmScheduler.EXTRA_KIND, kind)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            AlarmPlayer.stop(context)
            AlarmNotification.cancel(context)
            context.stopService(Intent(context, AlarmService::class.java))
            AppLog.write(context, "ALARM_STOP_REQUESTED")
        }
    }
}
