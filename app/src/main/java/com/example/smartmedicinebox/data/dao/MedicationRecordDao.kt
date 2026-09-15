package com.example.smartmedicinebox.data.dao

import androidx.room.*
import com.example.smartmedicinebox.data.model.MedicationRecord
import com.example.smartmedicinebox.data.model.MedicationStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationRecordDao {

    @Query("SELECT * FROM medication_records ORDER BY scheduledDate DESC, scheduledTime ASC")
    fun getAllRecords(): Flow<List<MedicationRecord>>

    @Query("SELECT * FROM medication_records WHERE scheduledDate = :date ORDER BY scheduledTime ASC")
    fun getRecordsByDate(date: String): Flow<List<MedicationRecord>>

    @Query("SELECT * FROM medication_records WHERE status = :status ORDER BY scheduledDate DESC")
    fun getRecordsByStatus(status: MedicationStatus): Flow<List<MedicationRecord>>

    @Query("SELECT * FROM medication_records WHERE medicineId = :medicineId ORDER BY scheduledDate DESC")
    fun getRecordsByMedicine(medicineId: Int): Flow<List<MedicationRecord>>

    @Query("SELECT * FROM medication_records WHERE medicineId = :medicineId AND scheduledDate = :date LIMIT 1")
    suspend fun getRecordForMedicineOnDate(medicineId: Int, date: String): MedicationRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: MedicationRecord): Long

    @Update
    suspend fun updateRecord(record: MedicationRecord): Int

    @Query("UPDATE medication_records SET status = :status, confirmationTime = :confirmationTime WHERE recordId = :recordId")
    suspend fun updateStatus(recordId: Int, status: MedicationStatus, confirmationTime: String?): Int

    @Query("DELETE FROM medication_records WHERE recordId = :recordId")
    suspend fun deleteRecord(recordId: Int): Int

    @Query("SELECT * FROM medication_records WHERE scheduledDate = :date AND (status = 'PENDING' OR status = 'DUE') ORDER BY scheduledTime ASC")
    fun getTodayPendingRecords(date: String): Flow<List<MedicationRecord>>
}
