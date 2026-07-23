package com.aljwaal.newtasks

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat

object AlarmNotification {
    const val CHANNEL_ID = "smart_tasks_urgent_alarm_v2"
    const val NOTIFICATION_ID = 7301

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(NotificationChannel(
            CHANNEL_ID,
            "تنبيهات المهام العاجلة",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "تنبيهات المهام مع شاشة كاملة وصوت واهتزاز"
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            enableLights(true)
            lightColor = Color.RED
            enableVibration(false)
            setSound(null, null)
        })
        AppLog.write(context, "NOTIFICATION_CHANNEL_CREATED", "id=$CHANNEL_ID")
    }

    fun build(
        context: Context,
        taskId: String,
        title: String,
        notes: String,
        kind: String
    ): Notification {
        ensureChannel(context)
        val fullScreenIntent = Intent(context, AlarmActivity::class.java).apply {
            data = Uri.parse("smarttasks://ring/$kind/${taskId.ifBlank { "test" }}")
            putExtra(AlarmScheduler.EXTRA_TASK_ID, taskId)
            putExtra(AlarmScheduler.EXTRA_TITLE, title)
            putExtra(AlarmScheduler.EXTRA_NOTES, notes)
            putExtra(AlarmScheduler.EXTRA_KIND, kind)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            3101 + taskId.hashCode().and(0x0FFF),
            fullScreenIntent,
            pendingIntentFlags()
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm)
            .setColor(0xFF4F46E5.toInt())
            .setContentTitle(title)
            .setContentText(notes.ifBlank { "حان موعد المهمة" })
            .setStyle(NotificationCompat.BigTextStyle().bigText(notes.ifBlank { "حان موعد المهمة — اضغط لفتح شاشة التنبيه" }))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(fullScreenPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setSilent(true)
            .addAction(0, "تم الإنجاز", actionPendingIntent(context, AlarmActionReceiver.ACTION_DONE, taskId, title, notes, 3201))
            .addAction(0, "تأجيل 5 دقائق", actionPendingIntent(context, AlarmActionReceiver.ACTION_SNOOZE_5, taskId, title, notes, 3202))
            .addAction(0, "تأجيل 10 دقائق", actionPendingIntent(context, AlarmActionReceiver.ACTION_SNOOZE_10, taskId, title, notes, 3203))
            .build()
    }

    fun cancel(context: Context) {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(NOTIFICATION_ID)
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
        baseCode + taskId.hashCode().and(0x0FFF),
        Intent(context, AlarmActionReceiver::class.java).apply {
            this.action = action
            data = Uri.parse("smarttasks://action/$action/${taskId.ifBlank { "test" }}")
            putExtra(AlarmScheduler.EXTRA_TASK_ID, taskId)
            putExtra(AlarmScheduler.EXTRA_TITLE, title)
            putExtra(AlarmScheduler.EXTRA_NOTES, notes)
        },
        pendingIntentFlags()
    )

    private fun pendingIntentFlags(): Int = PendingIntent.FLAG_UPDATE_CURRENT or
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
}
