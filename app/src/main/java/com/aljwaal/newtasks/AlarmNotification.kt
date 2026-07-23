package com.aljwaal.newtasks

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat

object AlarmNotification {
    const val CHANNEL_ID = "smart_tasks_urgent_alarm_v1"
    const val NOTIFICATION_ID = 7301

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "تنبيهات المهام العاجلة",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "تنبيهات المهام التي تحتاج ظهورًا فوريًا"
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            enableLights(true)
            lightColor = Color.RED
            enableVibration(false)
            setSound(null, null)
            setBypassDnd(false)
        }
        manager.createNotificationChannel(channel)
        AppLog.write(context, "NOTIFICATION_CHANNEL_CREATED", "id=$CHANNEL_ID")
    }

    fun build(context: Context, title: String, kind: String): Notification {
        ensureChannel(context)
        val fullScreenIntent = Intent(context, AlarmActivity::class.java).apply {
            putExtra(AlarmScheduler.EXTRA_TITLE, title)
            putExtra(AlarmScheduler.EXTRA_KIND, kind)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            3101,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm)
            .setColor(0xFF4F46E5.toInt())
            .setContentTitle(title)
            .setContentText("حان موعد المهمة — اضغط لفتح شاشة التنبيه")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(fullScreenPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setSilent(true)
            .addAction(
                0,
                "تم الإنجاز",
                actionPendingIntent(context, AlarmActionReceiver.ACTION_DONE, 3201)
            )
            .addAction(
                0,
                "تأجيل 5 دقائق",
                actionPendingIntent(context, AlarmActionReceiver.ACTION_SNOOZE_5, 3202)
            )
            .addAction(
                0,
                "تأجيل 10 دقائق",
                actionPendingIntent(context, AlarmActionReceiver.ACTION_SNOOZE_10, 3203)
            )
            .build()
    }

    fun cancel(context: Context) {
        context.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
    }

    private fun actionPendingIntent(context: Context, action: String, requestCode: Int): PendingIntent {
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, AlarmActionReceiver::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
