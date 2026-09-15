package com.example.smartmedicinebox.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MedicationStatus {
    PENDING,    // Scheduled but time not yet arrived
    DUE,        // Time has arrived, waiting for action
    CONFIRMED,  // Box was accessed / medicine taken
    MISSED      // No confirmation after configured period
}

@Entity(tableName = "medication_records")
data class MedicationRecord(
    @PrimaryKey(autoGenerate = true)
    val recordId: Int = 0,
    val medicineId: Int,
    val medicineName: String,
    val dosage: String,
    val scheduledDate: String,      // Format: "yyyy-MM-dd"
    val scheduledTime: String,      // Format: "HH:mm"
    val status: MedicationStatus = MedicationStatus.PENDING,
    val confirmationTime: String? = null  // Format: "HH:mm" when box access was recorded
)
