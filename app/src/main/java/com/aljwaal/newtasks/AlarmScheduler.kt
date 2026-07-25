package com.aljwaal.newtasks

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build

object AlarmScheduler {
    const val ACTION_FIRE = "com.aljwaal.newtasks.action.FIRE_ALARM"
    const val EXTRA_KIND = "alarm_kind"
    const val EXTRA_TASK_ID = "task_id"
    const val EXTRA_TITLE = "alarm_title"
    const val EXTRA_NOTES = "alarm_notes"

    const val KIND_TASK = "task"
    const val KIND_TEST = "test"
    const val KIND_SNOOZE = "snooze"

    data class ScheduleResult(
        val success: Boolean,
        val exact: Boolean,
        val triggerAtMillis: Long,
        val message: String
    )

    fun scheduleTask(context: Context, task: TaskItem): ScheduleResult {
        if (!task.reminderEnabled || task.status == TaskStatus.COMPLETED) {
            cancelTask(context, task.id)
            return ScheduleResult(true, true, task.dueAtMillis, "تم حفظ المهمة دون تنبيه")
        }
        return schedule(
            context = context,
            triggerAtMillis = task.dueAtMillis,
            requestCode = requestCode(task.id, KIND_TASK),
            kind = KIND_TASK,
            taskId = task.id,
            title = task.title,
            notes = task.notes
        )
    }

    fun scheduleTest(context: Context, delayMillis: Long = 30_000L): ScheduleResult =
        schedule(
            context = context,
            triggerAtMillis = System.currentTimeMillis() + delayMillis,
            requestCode = 1_102,
            kind = KIND_TEST,
            taskId = "",
            title = "اختبار تنبيه المهام",
            notes = "هذا اختبار للتأكد من عمل الصوت والاهتزاز وشاشة التنبيه."
        )

    fun scheduleSnooze(
        context: Context,
        taskId: String,
        title: String,
        notes: String,
        minutes: Int
    ): ScheduleResult {
        val triggerAt = System.currentTimeMillis() + minutes * 60_000L
        val result = schedule(
            context = context,
            triggerAtMillis = triggerAt,
            requestCode = requestCode(taskId.ifBlank { title }, KIND_SNOOZE),
            kind = KIND_SNOOZE,
            taskId = taskId,
            title = title,
            notes = notes
        )
        AppLog.write(
            context,
            "SNOOZE_SCHEDULED",
            "task=$taskId minutes=$minutes trigger=$triggerAt"
        )
        return result
    }

    fun fireNow(context: Context) {
        AppLog.write(context, "TEST_NOW_REQUESTED")
        context.sendBroadcast(
            Intent(context, AlarmReceiver::class.java).apply {
                action = ACTION_FIRE
                data = Uri.parse("smarttasks://alarm/test/now")
                putExtra(EXTRA_KIND, KIND_TEST)
                putExtra(EXTRA_TASK_ID, "")
                putExtra(EXTRA_TITLE, "اختبار فوري لتنبيه المهام")
                putExtra(
                    EXTRA_NOTES,
                    "إذا ظهرت هذه الشاشة مع الصوت والاهتزاز فمحرك التنبيه يعمل بنجاح."
                )
            }
        )
    }

    fun cancelTask(context: Context, taskId: String) {
        val alarmManager = alarmManager(context)
        listOf(KIND_TASK, KIND_SNOOZE).forEach { kind ->
            alarmManager.cancel(
                alarmPendingIntent(
                    context = context,
                    requestCode = requestCode(taskId, kind),
                    kind = kind,
                    taskId = taskId,
                    title = "",
                    notes = ""
                )
            )
        }
        AppLog.write(context, "TASK_ALARM_CANCELLED", "task=$taskId")
    }

    fun restoreAll(context: Context, reason: String): Int {
        var restored = 0
        val now = System.currentTimeMillis()
        TaskRepository.list(context)
            .filter { it.status == TaskStatus.PENDING && it.reminderEnabled }
            .forEach { original ->
                var task = original
                if (task.dueAtMillis <= now && task.lastNotifiedAtMillis >= task.dueAtMillis) {
                    val next = TaskRepository.nextOccurrence(task) ?: return@forEach
                    task = task.copy(dueAtMillis = next, lastNotifiedAtMillis = 0L)
                    TaskRepository.save(context, task)
                } else if (task.dueAtMillis <= now && task.lastNotifiedAtMillis == 0L) {
                    task = task.copy(dueAtMillis = now + 5_000L)
                }
                if (scheduleTask(context, task).success) restored++
            }
        AppLog.write(context, "ALL_ALARMS_RESTORED", "reason=$reason count=$restored")
        return restored
    }

    fun onAlarmFired(context: Context, taskId: String) {
        if (taskId.isBlank()) return
        val task = TaskRepository.get(context, taskId) ?: return
        TaskRepository.updateLastNotified(context, taskId)
        val next = TaskRepository.nextOccurrence(task)
        if (next != null) {
            val updated = task.copy(
                dueAtMillis = next,
                status = TaskStatus.PENDING,
                completedAtMillis = 0L,
                lastNotifiedAtMillis = 0L
            )
            TaskRepository.save(context, updated)
            scheduleTask(context, updated)
            AppLog.write(context, "REPEATING_TASK_RESCHEDULED", "task=$taskId next=$next")
        }
    }

    private fun schedule(
        context: Context,
        triggerAtMillis: Long,
        requestCode: Int,
        kind: String,
        taskId: String,
        title: String,
        notes: String
    ): ScheduleResult {
        val alarmManager = alarmManager(context)
        val operation = alarmPendingIntent(
            context = context,
            requestCode = requestCode,
            kind = kind,
            taskId = taskId,
            title = title,
            notes = notes
        )
        val exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()
        val safeTrigger = triggerAtMillis.coerceAtLeast(System.currentTimeMillis() + 1_000L)

        AppLog.write(
            context,
            "ALARM_SCHEDULE_REQUEST",
            "kind=$kind task=$taskId trigger=$safeTrigger exactPermission=$exactAllowed sdk=${Build.VERSION.SDK_INT}"
        )

        return runCatching {
            if (exactAllowed) {
                val showIntent = PendingIntent.getActivity(
                    context,
                    requestCode + 20_000,
                    Intent(context, MainActivity::class.java).apply {
                        data = Uri.parse("smarttasks://show/$taskId")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    },
                    pendingIntentFlags()
                )
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(safeTrigger, showIntent),
                    operation
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    safeTrigger,
                    operation
                )
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, safeTrigger, operation)
            }
            AppLog.write(
                context,
                "ALARM_REGISTERED",
                "kind=$kind task=$taskId exact=$exactAllowed"
            )
            ScheduleResult(
                success = true,
                exact = exactAllowed,
                triggerAtMillis = safeTrigger,
                message = if (exactAllowed) {
                    "تم تثبيت التنبيه بدقة"
                } else {
                    "تم تثبيت التنبيه، وقد يتأخر قليلًا بسبب إعدادات النظام"
                }
            )
        }.getOrElse { error ->
            AppLog.write(
                context,
                "ALARM_SCHEDULE_FAILED",
                "${error.javaClass.simpleName}: ${error.message}"
            )
            ScheduleResult(
                success = false,
                exact = false,
                triggerAtMillis = safeTrigger,
                message = "فشل تثبيت التنبيه: ${error.message}"
            )
        }
    }

    private fun alarmPendingIntent(
        context: Context,
        requestCode: Int,
        kind: String,
        taskId: String,
        title: String,
        notes: String
    ): PendingIntent = PendingIntent.getBroadcast(
        context,
        requestCode,
        Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_FIRE
            data = Uri.parse(
                "smarttasks://alarm/$kind/${taskId.ifBlank { requestCode.toString() }}"
            )
            putExtra(EXTRA_KIND, kind)
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TITLE, NumberFormatUtils.latinDigits(title))
            putExtra(EXTRA_NOTES, NumberFormatUtils.latinDigits(notes))
        },
        pendingIntentFlags()
    )

    private fun requestCode(id: String, kind: String): Int =
        2_000 + (("$kind:$id".hashCode().toLong() and 0x7FFFFFFFL) % 900_000L).toInt()

    private fun alarmManager(context: Context): AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun pendingIntentFlags(): Int =
        PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
}
