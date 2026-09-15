package com.example.smartmedicinebox.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.provider.Settings
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.smartmedicinebox.MainActivity

object NotificationHelper {
    private const val CHANNEL_ID = "medicine_reminder_v2"
    private const val CHANNEL_NAME = "Medicine Reminder"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications for medication reminders"
            enableLights(true)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 250, 500)
            setSound(
                Settings.System.DEFAULT_ALARM_ALERT_URI,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build()
            )
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    fun showMedicineReminder(
        context: Context,
        medicineName: String,
        dosage: String,
        scheduledTime: String,
        notificationId: Int
    ) {
        createNotificationChannel(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, notificationId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Medicine reminder")
            .setContentText("$medicineName: $dosage is due at $scheduledTime")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("It is time to access $medicineName ($dosage). Scheduled at $scheduledTime."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_menu_agenda,
                "Record access",
                MedicationAlarmScheduler.actionPendingIntent(
                    context, notificationId, MedicationAlarmScheduler.ACTION_CONFIRM_ACCESS
                )
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Missed",
                MedicationAlarmScheduler.actionPendingIntent(
                    context, notificationId, MedicationAlarmScheduler.ACTION_MARK_MISSED
                )
            )
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, notification)
    }

    fun cancelMedicineReminder(context: Context, medicineId: Int) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(medicineId)
    }

    fun showMissedAlert(
        context: Context,
        medicineName: String,
        scheduledTime: String,
        notificationId: Int
    ) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Missed medication")
            .setContentText("No box access was recorded for $medicineName. Scheduled: $scheduledTime")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, notification)
    }
}
