package com.aljwaal.newtasks

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val kind = intent.getStringExtra(AlarmScheduler.EXTRA_KIND) ?: AlarmScheduler.KIND_TEST
        val title = intent.getStringExtra(AlarmScheduler.EXTRA_TITLE) ?: "حان موعد المهمة"
        val forceActivity = intent.getBooleanExtra(AlarmScheduler.EXTRA_FORCE_ACTIVITY, false)

        AppLog.write(
            context,
            "ALARM_RECEIVED",
            "action=${intent.action} kind=$kind forceActivity=$forceActivity"
        )

        if (kind == AlarmScheduler.KIND_DAILY) {
            AlarmScheduler.restoreDaily(context, reason = "daily alarm fired")
        }

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        runCatching {
            powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "SmartTasksAlarm:receiver"
            ).apply {
                acquire(30_000L)
            }
            AppLog.write(context, "WAKE_LOCK_ACQUIRED", "timeout=30000")
        }.onFailure {
            AppLog.write(context, "WAKE_LOCK_FAILED", it.message.orEmpty())
        }

        runCatching {
            AlarmService.start(context, title, kind, forceActivity)
        }.onFailure {
            AppLog.write(
                context,
                "FOREGROUND_SERVICE_START_FAILED",
                "${it.javaClass.simpleName}: ${it.message}"
            )
        }
    }
}

class AlarmActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AppLog.write(context, "ALARM_ACTION_RECEIVED", "action=${intent.action}")
        when (intent.action) {
            ACTION_SNOOZE_5 -> AlarmScheduler.scheduleSnooze(context, 5)
            ACTION_SNOOZE_10 -> AlarmScheduler.scheduleSnooze(context, 10)
            ACTION_DONE, ACTION_STOP -> Unit
        }
        AlarmService.stop(context)
    }

    companion object {
        const val ACTION_DONE = "com.aljwaal.newtasks.action.DONE"
        const val ACTION_STOP = "com.aljwaal.newtasks.action.STOP"
        const val ACTION_SNOOZE_5 = "com.aljwaal.newtasks.action.SNOOZE_5"
        const val ACTION_SNOOZE_10 = "com.aljwaal.newtasks.action.SNOOZE_10"
    }
}

class SystemEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AppLog.write(context, "SYSTEM_EVENT_RECEIVED", "action=${intent.action}")
        AlarmNotification.ensureChannel(context)
        AlarmScheduler.restoreDaily(context, reason = intent.action ?: "unknown")
    }
}
