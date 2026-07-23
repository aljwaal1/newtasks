package com.aljwaal.newtasks

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val kind = intent.getStringExtra(AlarmScheduler.EXTRA_KIND) ?: AlarmScheduler.KIND_TEST
        val taskId = intent.getStringExtra(AlarmScheduler.EXTRA_TASK_ID).orEmpty()
        val repositoryTask = taskId.takeIf { it.isNotBlank() }?.let { TaskRepository.get(context, it) }
        val title = repositoryTask?.title ?: intent.getStringExtra(AlarmScheduler.EXTRA_TITLE) ?: "حان موعد المهمة"
        val notes = repositoryTask?.notes ?: intent.getStringExtra(AlarmScheduler.EXTRA_NOTES).orEmpty()

        AppLog.write(context, "ALARM_RECEIVED", "kind=$kind task=$taskId")
        if (kind == AlarmScheduler.KIND_TASK) AlarmScheduler.onAlarmFired(context, taskId)

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        runCatching {
            powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SmartTasksAlarm:receiver").apply {
                acquire(30_000L)
            }
            AppLog.write(context, "WAKE_LOCK_ACQUIRED", "timeout=30000")
        }.onFailure { AppLog.write(context, "WAKE_LOCK_FAILED", it.message.orEmpty()) }

        runCatching { AlarmService.start(context, taskId, title, notes, kind) }
            .onFailure { AppLog.write(context, "FOREGROUND_SERVICE_START_FAILED", "${it.javaClass.simpleName}: ${it.message}") }
    }
}

class AlarmActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(AlarmScheduler.EXTRA_TASK_ID).orEmpty()
        val title = intent.getStringExtra(AlarmScheduler.EXTRA_TITLE) ?: "حان موعد المهمة"
        val notes = intent.getStringExtra(AlarmScheduler.EXTRA_NOTES).orEmpty()
        AppLog.write(context, "ALARM_ACTION_RECEIVED", "action=${intent.action} task=$taskId")
        when (intent.action) {
            ACTION_DONE -> {
                if (taskId.isNotBlank()) {
                    val task = TaskRepository.get(context, taskId)
                    if (task?.repeatRule == RepeatRule.NONE) {
                        TaskRepository.markCompleted(context, taskId, true)
                        AlarmScheduler.cancelTask(context, taskId)
                    } else {
                        AppLog.write(context, "REPEATING_OCCURRENCE_COMPLETED", "task=$taskId next=${task?.dueAtMillis}")
                    }
                }
            }
            ACTION_SNOOZE_5 -> AlarmScheduler.scheduleSnooze(context, taskId, title, notes, 5)
            ACTION_SNOOZE_10 -> AlarmScheduler.scheduleSnooze(context, taskId, title, notes, 10)
        }
        AlarmService.stop(context)
    }

    companion object {
        const val ACTION_DONE = "com.aljwaal.newtasks.action.DONE"
        const val ACTION_SNOOZE_5 = "com.aljwaal.newtasks.action.SNOOZE_5"
        const val ACTION_SNOOZE_10 = "com.aljwaal.newtasks.action.SNOOZE_10"
    }
}

class SystemEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AppLog.write(context, "SYSTEM_EVENT_RECEIVED", "action=${intent.action}")
        AlarmNotification.ensureChannel(context)
        AlarmScheduler.restoreAll(context, intent.action ?: "unknown")
    }
}
