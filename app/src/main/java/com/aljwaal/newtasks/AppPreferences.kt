package com.aljwaal.newtasks

import android.content.Context

enum class AlarmSoundMode(
    val storageValue: String,
    val label: String,
    val description: String
) {
    VIBRATE_ONLY(
        storageValue = "vibrate_only",
        label = "بدون صوت مع اهتزاز",
        description = "اهتزاز فقط ويتوقف تلقائيًا."
    ),
    GENTLE_ONCE(
        storageValue = "gentle_once",
        label = "صوت خفيف مرة واحدة مع اهتزاز",
        description = "نغمة قصيرة غير متكررة، وهي الوضع الافتراضي."
    ),
    NORMAL_ALARM(
        storageValue = "normal_alarm",
        label = "صوت منبه عادي مع اهتزاز",
        description = "نغمة واضحة متكررة بحد أقصى 30 ثانية."
    );

    companion object {
        fun fromStorage(value: String?): AlarmSoundMode =
            entries.firstOrNull { it.storageValue == value } ?: GENTLE_ONCE
    }
}

object AppPreferences {
    private const val NAME = "smart_tasks_preferences"
    private const val KEY_DAILY_ENABLED = "daily_enabled"
    private const val KEY_DAILY_HOUR = "daily_hour"
    private const val KEY_DAILY_MINUTE = "daily_minute"
    private const val KEY_LAST_TRIGGER = "last_trigger"
    private const val KEY_ALARM_SOUND_MODE = "alarm_sound_mode"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun isDailyEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_DAILY_ENABLED, false)

    fun dailyHour(context: Context): Int = prefs(context).getInt(KEY_DAILY_HOUR, 9)

    fun dailyMinute(context: Context): Int = prefs(context).getInt(KEY_DAILY_MINUTE, 0)

    fun saveDaily(context: Context, enabled: Boolean, hour: Int, minute: Int) {
        prefs(context).edit()
            .putBoolean(KEY_DAILY_ENABLED, enabled)
            .putInt(KEY_DAILY_HOUR, hour)
            .putInt(KEY_DAILY_MINUTE, minute)
            .apply()
    }

    fun disableDaily(context: Context) {
        prefs(context).edit().putBoolean(KEY_DAILY_ENABLED, false).apply()
    }

    fun saveLastTrigger(context: Context, triggerAtMillis: Long) {
        prefs(context).edit().putLong(KEY_LAST_TRIGGER, triggerAtMillis).apply()
    }

    fun lastTrigger(context: Context): Long = prefs(context).getLong(KEY_LAST_TRIGGER, 0L)

    fun alarmSoundMode(context: Context): AlarmSoundMode =
        AlarmSoundMode.fromStorage(prefs(context).getString(KEY_ALARM_SOUND_MODE, null))

    fun saveAlarmSoundMode(context: Context, mode: AlarmSoundMode) {
        prefs(context).edit().putString(KEY_ALARM_SOUND_MODE, mode.storageValue).apply()
        AppLog.write(context, "ALARM_SOUND_MODE_CHANGED", "mode=${mode.storageValue}")
    }
}
