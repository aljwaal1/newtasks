package com.aljwaal.newtasks

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object AlarmNotification {
    const val CHANNEL_ID = "smart_tasks_urgent_alarm_v2"
    const val NOTIFICATION_ID = 7301

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "تنبيهات المهام",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تنبيهات المهام مع شاشة كاملة وتحكم مباشر بالإيقاف"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                enableLights(true)
                lightColor = Color.RED
                enableVibration(false)
                setSound(null, null)
            }
        )
        AppLog.write(context, "NOTIFICATION_CHANNEL_CREATED", "id=$CHANNEL_ID")
    }

    fun build(
        context: Context,
        taskId: String,
        title: String,
        notes: String,
        kind: String
    ): Notification = buildInternal(
        context = context,
        taskId = taskId,
        title = title,
        notes = notes,
        kind = kind,
        activeAlarm = true
    )

    private fun buildVisualReminder(
        context: Context,
        taskId: String,
        title: String,
        notes: String,
        kind: String
    ): Notification = buildInternal(
        context = context,
        taskId = taskId,
        title = title,
        notes = notes,
        kind = kind,
        activeAlarm = false
    )

    private fun buildInternal(
        context: Context,
        taskId: String,
        title: String,
        notes: String,
        kind: String,
        activeAlarm: Boolean
    ): Notification {
        ensureChannel(context)
        val screenPendingIntent = AlarmActivityLauncher.pendingIntent(
            context = context,
            requestCode = stableRequestCode("screen:$kind:$taskId", 3_101),
            taskId = taskId,
            title = title,
            notes = notes,
            kind = kind
        )
        val body = notes.ifBlank {
            if (activeAlarm) {
                "حان موعد المهمة — اضغط لفتح شاشة التنبيه"
            } else {
                "انتهى صوت التنبيه، وما زالت المهمة بانتظارك"
            }
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm)
            .setColor(0xFF4F46E5.toInt())
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setCategory(
                if (activeAlarm) NotificationCompat.CATEGORY_ALARM
                else NotificationCompat.CATEGORY_REMINDER
            )
            .setPriority(
                if (activeAlarm) NotificationCompat.PRIORITY_MAX
                else NotificationCompat.PRIORITY_HIGH
            )
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(activeAlarm)
            .setAutoCancel(!activeAlarm)
            .setOnlyAlertOnce(true)
            .setContentIntent(screenPendingIntent)
            .setSilent(true)
            .addAction(
                R.drawable.ic_alarm,
                if (activeAlarm) "إيقاف فورًا" else "إغلاق",
                actionPendingIntent(
                    context,
                    AlarmActionReceiver.ACTION_STOP,
                    taskId,
                    title,
                    notes,
                    3_200
                )
            )
            .addAction(
                R.drawable.ic_alarm,
                "تم الإنجاز",
                actionPendingIntent(
                    context,
                    AlarmActionReceiver.ACTION_DONE,
                    taskId,
                    title,
                    notes,
                    3_201
                )
            )
            .addAction(
                R.drawable.ic_alarm,
                "تأجيل 5 دقائق",
                actionPendingIntent(
                    context,
                    AlarmActionReceiver.ACTION_SNOOZE_5,
                    taskId,
                    title,
                    notes,
                    3_202
                )
            )

        if (activeAlarm) {
            builder.setFullScreenIntent(screenPendingIntent, true)
        }
        return builder.build()
    }

    fun post(
        context: Context,
        taskId: String,
        title: String,
        notes: String,
        kind: String
    ): Boolean = notifySafely(
        context,
        build(context, taskId, title, notes, kind),
        event = "NOTIFICATION_POSTED_DIRECTLY",
        details = "kind=$kind task=$taskId"
    )

    fun postVisualReminder(
        context: Context,
        taskId: String,
        title: String,
        notes: String,
        kind: String
    ): Boolean = notifySafely(
        context,
        buildVisualReminder(context, taskId, title, notes, kind),
        event = "VISUAL_REMINDER_POSTED",
        details = "kind=$kind task=$taskId"
    )

    private fun notifySafely(
        context: Context,
        notification: Notification,
        event: String,
        details: String
    ): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            AppLog.write(context, "NOTIFICATION_POST_SKIPPED", "permission=false")
            return false
        }

        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) {
            AppLog.write(context, "NOTIFICATION_POST_SKIPPED", "notificationsDisabled=true")
            return false
        }

        return runCatching {
            manager.notify(NOTIFICATION_ID, notification)
            AppLog.write(context, event, details)
            true
        }.getOrElse { error ->
            AppLog.write(
                context,
                "NOTIFICATION_POST_FAILED",
                "${error.javaClass.simpleName}: ${error.message}"
            )
            false
        }
    }

    fun cancel(context: Context) {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .cancel(NOTIFICATION_ID)
    }

    private fun actionPendingIntent(
        context: Context,
        action: String,
        taskId: String,
        title: String,
        notes: String,
        baseCode: Int
    ): PendingIntent = PendingIntent.getBroadcast(
        context,
        stableRequestCode("$action:$taskId", baseCode),
        Intent(context, AlarmActionReceiver::class.java).apply {
            this.action = action
            data = Uri.parse("smarttasks://action/$action/${taskId.ifBlank { "test" }}")
            putExtra(AlarmScheduler.EXTRA_TASK_ID, taskId)
            putExtra(AlarmScheduler.EXTRA_TITLE, title)
            putExtra(AlarmScheduler.EXTRA_NOTES, notes)
        },
        pendingIntentFlags()
    )

    private fun stableRequestCode(value: String, base: Int): Int =
        base + (value.hashCode().toLong() and 0x7FFFFFFFL).rem(100_000L).toInt()

    private fun pendingIntentFlags(): Int =
        PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
}
