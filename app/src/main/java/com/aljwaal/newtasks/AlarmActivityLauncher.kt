package com.aljwaal.newtasks

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi

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
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Api34.createPendingIntent(
                context,
                requestCode,
                intent,
                pendingIntentFlags()
            )
        } else {
            PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                pendingIntentFlags()
            )
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                Api34.send(context, pendingIntent)
            } else {
                pendingIntent.send()
            }
            AppLog.write(
                context,
                "ALARM_ACTIVITY_PENDING_INTENT_SENT",
                "kind=$kind task=$taskId"
            )
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
        putExtra(AlarmScheduler.EXTRA_TITLE, NumberFormatUtils.latinDigits(title))
        putExtra(AlarmScheduler.EXTRA_NOTES, NumberFormatUtils.latinDigits(notes))
        putExtra(AlarmScheduler.EXTRA_KIND, kind)
        addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        )
    }

    private fun pendingIntentFlags(): Int =
        PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private object Api34 {
        fun createPendingIntent(
            context: Context,
            requestCode: Int,
            intent: Intent,
            flags: Int
        ): PendingIntent {
            val options = ActivityOptions.makeBasic().apply {
                setPendingIntentCreatorBackgroundActivityStartMode(mode())
            }.toBundle()
            return PendingIntent.getActivity(context, requestCode, intent, flags, options)
        }

        fun send(context: Context, pendingIntent: PendingIntent) {
            val options = ActivityOptions.makeBasic().apply {
                setPendingIntentBackgroundActivityStartMode(mode())
            }.toBundle()
            pendingIntent.send(context, 0, null, null, null, null, options)
        }

        @Suppress("DEPRECATION")
        private fun mode(): Int =
            if (Build.VERSION.SDK_INT >= 36) {
                Api36.allowAlwaysMode()
            } else {
                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
            }
    }

    @RequiresApi(36)
    private object Api36 {
        fun allowAlwaysMode(): Int =
            ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_ALWAYS
    }
}
