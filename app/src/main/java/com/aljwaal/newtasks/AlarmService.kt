package com.aljwaal.newtasks

import android.app.KeyguardManager
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
        val title = intent?.getStringExtra(AlarmScheduler.EXTRA_TITLE)
            ?: "حان موعد المهمة"
        val kind = intent?.getStringExtra(AlarmScheduler.EXTRA_KIND)
            ?: AlarmScheduler.KIND_TEST
        val forceActivity = intent?.getBooleanExtra(AlarmScheduler.EXTRA_FORCE_ACTIVITY, false) == true

        AppLog.write(
            this,
            "ALARM_SERVICE_STARTED",
            "kind=$kind forceActivity=$forceActivity"
        )

        val notification = AlarmNotification.build(this, title, kind)
        startForeground(AlarmNotification.NOTIFICATION_ID, notification)
        AppLog.write(this, "NOTIFICATION_POSTED", "fullScreenIntent=true")
        AlarmPlayer.start(this)

        val keyguard = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (forceActivity || keyguard.isKeyguardLocked) {
            val activityIntent = Intent(this, AlarmActivity::class.java).apply {
                putExtra(AlarmScheduler.EXTRA_TITLE, title)
                putExtra(AlarmScheduler.EXTRA_KIND, kind)
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
            }
            runCatching {
                startActivity(activityIntent)
                AppLog.write(this, "FULL_SCREEN_START_REQUESTED", "direct=true")
            }.onFailure {
                AppLog.write(
                    this,
                    "FULL_SCREEN_START_FAILED",
                    "${it.javaClass.simpleName}: ${it.message}"
                )
            }
        } else {
            AppLog.write(this, "FULL_SCREEN_DELEGATED_TO_NOTIFICATION", "screenUnlocked=true")
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
        fun start(context: Context, title: String, kind: String, forceActivity: Boolean) {
            val intent = Intent(context, AlarmService::class.java).apply {
                putExtra(AlarmScheduler.EXTRA_TITLE, title)
                putExtra(AlarmScheduler.EXTRA_KIND, kind)
                putExtra(AlarmScheduler.EXTRA_FORCE_ACTIVITY, forceActivity)
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
