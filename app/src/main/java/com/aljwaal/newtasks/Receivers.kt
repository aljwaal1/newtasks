package com.aljwaal.newtasks

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        val copiedIntent = Intent(intent)

        Thread({
            var wakeLock: PowerManager.WakeLock? = null
            try {
                val kind = copiedIntent.getStringExtra(AlarmScheduler.EXTRA_KIND)
                    ?: AlarmScheduler.KIND_TEST
                val taskId = copiedIntent.getStringExtra(AlarmScheduler.EXTRA_TASK_ID).orEmpty()

                val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as PowerManager
                wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "SmartTasksAlarm:receiver"
                ).apply {
                    setReferenceCounted(false)
                    acquire(30_000L)
                }
                AppLog.write(appContext, "WAKE_LOCK_ACQUIRED", "timeout=30000")

                val repositoryTask = taskId.takeIf { it.isNotBlank() }
                    ?.let { TaskRepository.get(appContext, it) }
                val title = repositoryTask?.title
                    ?: copiedIntent.getStringExtra(AlarmScheduler.EXTRA_TITLE)
                    ?: "حان موعد المهمة"
                val notes = repositoryTask?.notes
                    ?: copiedIntent.getStringExtra(AlarmScheduler.EXTRA_NOTES).orEmpty()

                AppLog.write(appContext, "ALARM_RECEIVED", "kind=$kind task=$taskId")
                if (kind == AlarmScheduler.KIND_TASK) {
                    AlarmScheduler.onAlarmFired(appContext, taskId)
                }

                val serviceStarted = runCatching {
                    AlarmService.start(appContext, taskId, title, notes, kind, launchScreen = true)
                    true
                }.getOrElse { error ->
                    AppLog.write(
                        appContext,
                        "FOREGROUND_SERVICE_START_FAILED",
                        "${error.javaClass.simpleName}: ${error.message}"
                    )
                    false
                }

                if (!serviceStarted) {
                    AlarmNotification.post(appContext, taskId, title, notes, kind)
                    AlarmActivityLauncher.launch(
                        context = appContext,
                        requestCode = stableRequestCode("receiver:$kind:$taskId"),
                        taskId = taskId,
                        title = title,
                        notes = notes,
                        kind = kind
                    )
                }
            } catch (error: Throwable) {
                AppLog.write(
                    appContext,
                    "ALARM_RECEIVER_FAILED",
                    "${error.javaClass.simpleName}: ${error.message}"
                )
            } finally {
                runCatching { if (wakeLock?.isHeld == true) wakeLock?.release() }
                AppLog.write(appContext, "WAKE_LOCK_RELEASED")
                pendingResult.finish()
            }
        }, "SmartTasks-AlarmReceiver").start()
    }
}

class AlarmActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        val copiedIntent = Intent(intent)

        // أوقف الصوت فورًا، ثم نفّذ تحديث البيانات خارج خيط النظام الرئيسي.
        AlarmService.stop(appContext)

        Thread({
            try {
                val taskId = copiedIntent.getStringExtra(AlarmScheduler.EXTRA_TASK_ID).orEmpty()
                val title = copiedIntent.getStringExtra(AlarmScheduler.EXTRA_TITLE)
                    ?: "حان موعد المهمة"
                val notes = copiedIntent.getStringExtra(AlarmScheduler.EXTRA_NOTES).orEmpty()
                AppLog.write(
                    appContext,
                    "ALARM_ACTION_RECEIVED",
                    "action=${copiedIntent.action} task=$taskId"
                )

                when (copiedIntent.action) {
                    ACTION_DONE -> {
                        if (taskId.isNotBlank()) {
                            val task = TaskRepository.get(appContext, taskId)
                            if (task?.repeatRule == RepeatRule.NONE) {
                                TaskRepository.markCompleted(appContext, taskId, true)
                                AlarmScheduler.cancelTask(appContext, taskId)
                            } else {
                                AppLog.write(
                                    appContext,
                                    "REPEATING_OCCURRENCE_COMPLETED",
                                    "task=$taskId next=${task?.dueAtMillis}"
                                )
                            }
                        }
                    }

                    ACTION_SNOOZE_5 ->
                        AlarmScheduler.scheduleSnooze(appContext, taskId, title, notes, 5)

                    ACTION_SNOOZE_10 ->
                        AlarmScheduler.scheduleSnooze(appContext, taskId, title, notes, 10)
                }
            } catch (error: Throwable) {
                AppLog.write(
                    appContext,
                    "ALARM_ACTION_FAILED",
                    "${error.javaClass.simpleName}: ${error.message}"
                )
            } finally {
                pendingResult.finish()
            }
        }, "SmartTasks-AlarmAction").start()
    }

    companion object {
        const val ACTION_DONE = "com.aljwaal.newtasks.action.DONE"
        const val ACTION_SNOOZE_5 = "com.aljwaal.newtasks.action.SNOOZE_5"
        const val ACTION_SNOOZE_10 = "com.aljwaal.newtasks.action.SNOOZE_10"
    }
}

class SystemEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        val action = intent.action ?: "unknown"

        Thread({
            try {
                AppLog.write(appContext, "SYSTEM_EVENT_RECEIVED", "action=$action")
                AlarmNotification.ensureChannel(appContext)
                AlarmScheduler.restoreAll(appContext, action)
            } catch (error: Throwable) {
                AppLog.write(
                    appContext,
                    "SYSTEM_EVENT_RESTORE_FAILED",
                    "${error.javaClass.simpleName}: ${error.message}"
                )
            } finally {
                pendingResult.finish()
            }
        }, "SmartTasks-SystemRestore").start()
    }
}

private fun stableRequestCode(value: String): Int =
    100_000 + (value.hashCode().toLong() and 0x7FFFFFFFL).rem(800_000L).toInt()
