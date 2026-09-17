package com.example.smartmedicinebox.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medicines")
data class Medicine(
    @PrimaryKey(autoGenerate = true)
    val medicineId: Int = 0,
    val userId: String = "",
    val medicineName: String,
    val dosage: String,
    val scheduledTime: String, // Format: "HH:mm" e.g. "08:00"
    val compartment: Int = 1,  // Legacy protocol field; this app uses one box.
    val active: Boolean = true
)
