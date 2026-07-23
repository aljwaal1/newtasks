package com.aljwaal.newtasks

import android.content.Context

object AppPreferences {
    private const val NAME = "smart_tasks_preferences"
    private const val KEY_DAILY_ENABLED = "daily_enabled"
    private const val KEY_DAILY_HOUR = "daily_hour"
    private const val KEY_DAILY_MINUTE = "daily_minute"
    private const val KEY_LAST_TRIGGER = "last_trigger"

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
}
