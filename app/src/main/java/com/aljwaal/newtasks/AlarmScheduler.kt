package com.aljwaal.newtasks

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object AlarmScheduler {
    const val ACTION_FIRE = "com.aljwaal.newtasks.action.FIRE_ALARM"
    const val EXTRA_KIND = "alarm_kind"
    const val EXTRA_TITLE = "alarm_title"
    const val EXTRA_FORCE_ACTIVITY = "force_activity"

    const val KIND_DAILY = "daily"
    const val KIND_TEST = "test"
    const val KIND_SNOOZE = "snooze"

    private const val REQUEST_DAILY = 1101
    private const val REQUEST_TEST = 1102
    private const val REQUEST_SNOOZE = 1103

    data class ScheduleResult(
        val success: Boolean,
        val exact: Boolean,
        val triggerAtMillis: Long,
        val message: String
    )

    fun scheduleDaily(context: Context, hour: Int, minute: Int): ScheduleResult {
        require(hour in 0..23 && minute in 0..59)
        val triggerAt = nextDailyTrigger(hour, minute)
        AppPreferences.saveDaily(context, enabled = true, hour = hour, minute = minute)
        val result = schedule(
            context = context,
            triggerAtMillis = triggerAt,
            requestCode = REQUEST_DAILY,
            kind = KIND_DAILY,
            title = "موعد مراجعة المهام",
            forceActivity = false
        )
        AppPreferences.saveLastTrigger(context, triggerAt)
        AppLog.write(
            context,
            "DAILY_ALARM_SAVED",
            "time=%02d:%02d trigger=${format(triggerAt)} exact=${result.exact}".format(hour, minute)
        )
        return result
    }

    fun restoreDaily(context: Context, reason: String): ScheduleResult? {
        if (!AppPreferences.isDailyEnabled(context)) {
            AppLog.write(context, "RESTORE_SKIPPED", "reason=$reason daily=false")
            return null
        }
        val result = scheduleDaily(
            context,
            AppPreferences.dailyHour(context),
            AppPreferences.dailyMinute(context)
        )
        AppLog.write(context, "DAILY_ALARM_RESTORED", "reason=$reason ${result.message}")
        return result
    }

    fun scheduleTest(context: Context, delayMillis: Long = 30_000L): ScheduleResult {
        val triggerAt = System.currentTimeMillis() + delayMillis
        return schedule(
            context,
            triggerAtMillis = triggerAt,
            requestCode = REQUEST_TEST,
            kind = KIND_TEST,
            title = "اختبار تنبيه المهام",
            forceActivity = false
        )
    }

    fun scheduleSnooze(context: Context, minutes: Int): ScheduleResult {
        val triggerAt = System.currentTimeMillis() + minutes * 60_000L
        val result = schedule(
            context,
            triggerAtMillis = triggerAt,
            requestCode = REQUEST_SNOOZE,
            kind = KIND_SNOOZE,
            title = "تذكير مؤجل لمدة $minutes دقائق",
            forceActivity = false
        )
        AppLog.write(context, "SNOOZE_SCHEDULED", "minutes=$minutes trigger=${format(triggerAt)}")
        return result
    }

    fun fireNow(context: Context) {
        AppLog.write(context, "TEST_NOW_REQUESTED")
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_FIRE
            putExtra(EXTRA_KIND, KIND_TEST)
            putExtra(EXTRA_TITLE, "اختبار فوري لتنبيه المهام")
            putExtra(EXTRA_FORCE_ACTIVITY, true)
        }
        context.sendBroadcast(intent)
    }

    fun cancelDaily(context: Context) {
        val alarmManager = alarmManager(context)
        alarmManager.cancel(alarmPendingIntent(context, REQUEST_DAILY, KIND_DAILY, "", false))
        AppPreferences.disableDaily(context)
        AppLog.write(context, "DAILY_ALARM_CANCELLED")
    }

    private fun schedule(
        context: Context,
        triggerAtMillis: Long,
        requestCode: Int,
        kind: String,
        title: String,
        forceActivity: Boolean
    ): ScheduleResult {
        val alarmManager = alarmManager(context)
        val operation = alarmPendingIntent(context, requestCode, kind, title, forceActivity)
        val exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()

        AppLog.write(
            context,
            "ALARM_SCHEDULE_REQUEST",
            "kind=$kind trigger=${format(triggerAtMillis)} exactPermission=$exactAllowed sdk=${Build.VERSION.SDK_INT}"
        )

        return runCatching {
            if (exactAllowed) {
                val showIntent = PendingIntent.getActivity(
                    context,
                    2100 + requestCode,
                    Intent(context, MainActivity::class.java).addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    ),
                    pendingIntentFlags()
                )
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent),
                    operation
                )
                AppLog.write(context, "EXACT_ALARM_REGISTERED", "kind=$kind")
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    operation
                )
                AppLog.write(context, "INEXACT_FALLBACK_REGISTERED", "kind=$kind mode=idle")
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation)
                AppLog.write(context, "INEXACT_FALLBACK_REGISTERED", "kind=$kind mode=legacy")
            }
            ScheduleResult(
                success = true,
                exact = exactAllowed,
                triggerAtMillis = triggerAtMillis,
                message = if (exactAllowed) {
                    "تم تثبيت التنبيه الدقيق في ${format(triggerAtMillis)}"
                } else {
                    "تم تثبيت تنبيه تقريبي. امنح صلاحية المنبهات الدقيقة لضمان الموعد."
                }
            )
        }.getOrElse { error ->
            AppLog.write(context, "ALARM_SCHEDULE_FAILED", "${error.javaClass.simpleName}: ${error.message}")
            ScheduleResult(false, false, triggerAtMillis, "فشل تثبيت التنبيه: ${error.message}")
        }
    }

    private fun alarmPendingIntent(
        context: Context,
        requestCode: Int,
        kind: String,
        title: String,
        forceActivity: Boolean
    ): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_FIRE
            putExtra(EXTRA_KIND, kind)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_FORCE_ACTIVITY, forceActivity)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            pendingIntentFlags()
        )
    }

    private fun alarmManager(context: Context): AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun pendingIntentFlags(): Int =
        PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0

    private fun nextDailyTrigger(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (!after(now)) add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
    }

    fun format(value: Long): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(value))
}
