package com.example.smartmedicinebox.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.smartmedicinebox.data.database.AppDatabase
import com.example.smartmedicinebox.data.model.MedicationRecord
import com.example.smartmedicinebox.data.model.MedicationStatus
import com.example.smartmedicinebox.data.model.Medicine
import com.example.smartmedicinebox.data.remote.Esp32Client
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

internal fun nextReminderTime(
    scheduledTime: String,
    now: ZonedDateTime = ZonedDateTime.now()
): ZonedDateTime {
    val time = LocalTime.parse(scheduledTime, DateTimeFormatter.ofPattern("HH:mm"))
    val candidate = now.withHour(time.hour).withMinute(time.minute).withSecond(0).withNano(0)
    return if (candidate.isAfter(now)) candidate else candidate.plusDays(1)
}

object MedicationAlarmScheduler {
    private const val ACTION_REMINDER = "com.example.smartmedicinebox.MEDICATION_REMINDER"
    const val ACTION_CONFIRM_ACCESS = "com.example.smartmedicinebox.CONFIRM_ACCESS"
    const val ACTION_MARK_MISSED = "com.example.smartmedicinebox.MARK_MISSED"

    fun schedule(context: Context, medicine: Medicine) {
        if (!medicine.active || medicine.medicineId <= 0) {
            cancel(context, medicine.medicineId)
            return
        }

        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            nextReminderTime(medicine.scheduledTime).toInstant().toEpochMilli(),
            pendingIntent(context, medicine)
        )
    }

    fun cancel(context: Context, medicineId: Int) {
        if (medicineId <= 0) return
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.cancel(pendingIntent(context, medicineId))
    }

    suspend fun scheduleAll(context: Context) {
        AppDatabase.getDatabase(context).medicineDao().getAllActiveMedicinesOnce()
            .forEach { schedule(context, it) }
    }

    private fun pendingIntent(context: Context, medicine: Medicine): PendingIntent {
        val intent = reminderIntent(context, medicine.medicineId).apply {
            putExtra(EXTRA_MEDICINE_NAME, medicine.medicineName)
            putExtra(EXTRA_DOSAGE, medicine.dosage)
            putExtra(EXTRA_SCHEDULED_TIME, medicine.scheduledTime)
        }
        return PendingIntent.getBroadcast(
            context,
            medicine.medicineId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun pendingIntent(context: Context, medicineId: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            medicineId,
            reminderIntent(context, medicineId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun reminderIntent(context: Context, medicineId: Int) =
        Intent(context, MedicationReminderReceiver::class.java).apply {
            action = ACTION_REMINDER
            data = Uri.parse("smartmedicinebox://reminder/$medicineId")
            putExtra(EXTRA_MEDICINE_ID, medicineId)
        }

    fun actionPendingIntent(context: Context, medicineId: Int, action: String): PendingIntent {
        val intent = Intent(context, MedicationActionReceiver::class.java).apply {
            this.action = action
            data = Uri.parse("smartmedicinebox://reminder/$medicineId/$action")
            putExtra(EXTRA_MEDICINE_ID, medicineId)
        }
        val requestCode = medicineId * 10 + if (action == ACTION_CONFIRM_ACCESS) 1 else 2
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    internal const val EXTRA_MEDICINE_ID = "medicine_id"
    internal const val EXTRA_MEDICINE_NAME = "medicine_name"
    internal const val EXTRA_DOSAGE = "dosage"
    internal const val EXTRA_SCHEDULED_TIME = "scheduled_time"
}

class MedicationReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val medicineId = intent.getIntExtra(MedicationAlarmScheduler.EXTRA_MEDICINE_ID, 0)
        if (medicineId <= 0) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = AppDatabase.getDatabase(context)
                val medicine = database.medicineDao().getMedicineById(medicineId)
                if (medicine?.active != true) return@launch

                NotificationHelper.showMedicineReminder(
                    context = context,
                    medicineName = medicine.medicineName,
                    dosage = medicine.dosage,
                    scheduledTime = medicine.scheduledTime,
                    notificationId = medicine.medicineId
                )

                val date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                val existingRecord = database.medicationRecordDao()
                    .getRecordForMedicineOnDate(medicine.medicineId, date)
                if (existingRecord == null) {
                    database.medicationRecordDao().insertRecord(
                        MedicationRecord(
                            medicineId = medicine.medicineId,
                            medicineName = medicine.medicineName,
                            dosage = medicine.dosage,
                            scheduledDate = date,
                            scheduledTime = medicine.scheduledTime,
                            status = MedicationStatus.DUE
                        )
                    )
                } else if (existingRecord.status == MedicationStatus.PENDING) {
                    database.medicationRecordDao().updateStatus(
                        existingRecord.recordId,
                        MedicationStatus.DUE,
                        null
                    )
                }

                MedicationAlarmScheduler.schedule(context, medicine)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

class ReminderRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                MedicationAlarmScheduler.scheduleAll(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

class MedicationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val medicineId = intent.getIntExtra(MedicationAlarmScheduler.EXTRA_MEDICINE_ID, 0)
        if (medicineId <= 0) return
        val confirmed = intent.action == MedicationAlarmScheduler.ACTION_CONFIRM_ACCESS

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                NotificationHelper.cancelMedicineReminder(context, medicineId)

                val database = AppDatabase.getDatabase(context)
                val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                val record = database.medicationRecordDao()
                    .getRecordForMedicineOnDate(medicineId, today)
                if (record != null &&
                    (record.status == MedicationStatus.DUE || record.status == MedicationStatus.PENDING)
                ) {
                    if (confirmed) {
                        val now = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
                        database.medicationRecordDao().updateStatus(
                            record.recordId, MedicationStatus.CONFIRMED, now
                        )
                    } else {
                        database.medicationRecordDao().updateStatus(
                            record.recordId, MedicationStatus.MISSED, null
                        )
                    }
                }

                runCatching {
                    Esp32Client(context).acknowledgeMedicine(
                        medicineId,
                        if (confirmed) MedicationStatus.CONFIRMED.name else MedicationStatus.MISSED.name
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
