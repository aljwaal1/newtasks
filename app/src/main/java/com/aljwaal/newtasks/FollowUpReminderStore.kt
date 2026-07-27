package com.aljwaal.newtasks

import android.content.Context

/**
 * يخزن موعد التذكير التالي بصورة مستقلة عن تاريخ استحقاق المهمة.
 * بذلك لا يتغير dueAtMillis عند اختيار «ذكّرني غدًا» أو موعد مخصص.
 */
object FollowUpReminderStore {
    private const val PREFS = "smart_tasks_follow_up_reminders"
    private const val PREFIX = "task_"
    private val lock = Any()

    fun set(context: Context, taskId: String, triggerAtMillis: Long) = synchronized(lock) {
        if (taskId.isBlank()) return@synchronized
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putLong(PREFIX + taskId, triggerAtMillis)
            .apply()
    }

    fun get(context: Context, taskId: String): Long = synchronized(lock) {
        if (taskId.isBlank()) return@synchronized 0L
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(PREFIX + taskId, 0L)
    }

    fun remove(context: Context, taskId: String) = synchronized(lock) {
        if (taskId.isBlank()) return@synchronized
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(PREFIX + taskId)
            .apply()
    }

    fun all(context: Context): Map<String, Long> = synchronized(lock) {
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .all
            .mapNotNull { (key, value) ->
                val taskId = key.takeIf { it.startsWith(PREFIX) }
                    ?.removePrefix(PREFIX)
                    ?.takeIf { it.isNotBlank() }
                    ?: return@mapNotNull null
                val trigger = (value as? Long)?.takeIf { it > 0L }
                    ?: return@mapNotNull null
                taskId to trigger
            }
            .toMap()
    }
}
