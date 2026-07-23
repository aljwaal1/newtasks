package com.aljwaal.newtasks

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build

/**
 * ينشئ ويفعّل PendingIntent موثوقًا لشاشة المنبه.
 *
 * بدء Activity مباشرة من Service أو BroadcastReceiver قد يحظره Android عندما يكون
 * التطبيق في الخلفية. لذلك نستخدم PendingIntent مع خيارات Background Activity Launch
 * المتاحة في الأنظمة الحديثة، ونبقي الإشعار الكامل كمسار احتياطي.
 */
object AlarmActivityLauncher {

    fun pendingIntent(
        context: Context,
        requestCode: Int,
        taskId: String,
        title: String,
        notes: String,
        kind: String
    ): PendingIntent {
        val intent = alarmIntent(context, taskId, title, notes, kind)
        val options = creatorOptions()
        return if (options == null) {
            PendingIntent.getActivity(context, requestCode, intent, pendingIntentFlags())
        } else {
            PendingIntent.getActivity(context, requestCode, intent, pendingIntentFlags(), options)
        }
    }

    fun launch(
        context: Context,
        requestCode: Int,
        taskId: String,
        title: String,
        notes: String,
        kind: String
    ): Boolean {
        val pendingIntent = pendingIntent(context, requestCode, taskId, title, notes, kind)
        return runCatching {
            val options = senderOptions()
            if (options == null) {
                pendingIntent.send()
            } else {
                pendingIntent.send(context, 0, null, null, null, null, options)
            }
            AppLog.write(context, "ALARM_ACTIVITY_PENDING_INTENT_SENT", "kind=$kind task=$taskId")
            true
        }.getOrElse { error ->
            AppLog.write(
                context,
                "ALARM_ACTIVITY_PENDING_INTENT_FAILED",
                "${error.javaClass.simpleName}: ${error.message}"
            )
            false
        }
    }

    private fun alarmIntent(
        context: Context,
        taskId: String,
        title: String,
        notes: String,
        kind: String
    ) = Intent(context, AlarmActivity::class.java).apply {
        data = Uri.parse("smarttasks://ring/$kind/${taskId.ifBlank { "test" }}")
        putExtra(AlarmScheduler.EXTRA_TASK_ID, taskId)
        putExtra(AlarmScheduler.EXTRA_TITLE, title)
        putExtra(AlarmScheduler.EXTRA_NOTES, notes)
        putExtra(AlarmScheduler.EXTRA_KIND, kind)
        addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        )
    }

    private fun creatorOptions(): android.os.Bundle? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return null
        return ActivityOptions.makeBasic().apply {
            setPendingIntentCreatorBackgroundActivityStartMode(backgroundStartMode())
        }.toBundle()
    }

    private fun senderOptions(): android.os.Bundle? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return null
        return ActivityOptions.makeBasic().apply {
            setPendingIntentBackgroundActivityStartMode(backgroundStartMode())
        }.toBundle()
    }

    @Suppress("DEPRECATION")
    private fun backgroundStartMode(): Int =
        if (Build.VERSION.SDK_INT >= 36) {
            ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_ALWAYS
        } else {
            ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
        }

    private fun pendingIntentFlags(): Int =
        PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
}
