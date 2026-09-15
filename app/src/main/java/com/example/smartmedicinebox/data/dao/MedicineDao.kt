package com.example.smartmedicinebox.data.dao

import androidx.room.*
import com.example.smartmedicinebox.data.model.Medicine
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicineDao {

    @Query("SELECT * FROM medicines WHERE active = 1 ORDER BY scheduledTime ASC")
    fun getAllActiveMedicines(): Flow<List<Medicine>>

    @Query("SELECT * FROM medicines WHERE active = 1 ORDER BY scheduledTime ASC")
    suspend fun getAllActiveMedicinesOnce(): List<Medicine>

    @Query("SELECT * FROM medicines ORDER BY scheduledTime ASC")
    fun getAllMedicines(): Flow<List<Medicine>>

    @Query("SELECT * FROM medicines WHERE medicineId = :id")
    suspend fun getMedicineById(id: Int): Medicine?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicine(medicine: Medicine): Long

    @Update
    suspend fun updateMedicine(medicine: Medicine): Int

    @Delete
    suspend fun deleteMedicine(medicine: Medicine): Int

    @Query("UPDATE medicines SET active = 0 WHERE medicineId = :id")
    suspend fun deactivateMedicine(id: Int): Int
}
