package com.example.smartmedicinebox.data.repository

import com.example.smartmedicinebox.data.dao.MedicationRecordDao
import com.example.smartmedicinebox.data.dao.MedicineDao
import com.example.smartmedicinebox.data.model.Medicine
import com.example.smartmedicinebox.data.model.MedicationRecord
import com.example.smartmedicinebox.data.model.MedicationStatus
import kotlinx.coroutines.flow.Flow

class MedicineRepository(
    private val medicineDao: MedicineDao,
    private val recordDao: MedicationRecordDao
) {
    // Medicine CRUD
    val allActiveMedicines: Flow<List<Medicine>> = medicineDao.getAllActiveMedicines()
    val allMedicines: Flow<List<Medicine>> = medicineDao.getAllMedicines()

    suspend fun getMedicineById(id: Int): Medicine? = medicineDao.getMedicineById(id)

    suspend fun getAllActiveMedicinesOnce(): List<Medicine> = medicineDao.getAllActiveMedicinesOnce()

    suspend fun insertMedicine(medicine: Medicine): Long = medicineDao.insertMedicine(medicine)

    suspend fun updateMedicine(medicine: Medicine) = medicineDao.updateMedicine(medicine)

    suspend fun deleteMedicine(medicine: Medicine) = medicineDao.deleteMedicine(medicine)

    // Medication Records
    val allRecords: Flow<List<MedicationRecord>> = recordDao.getAllRecords()

    fun getRecordsByDate(date: String): Flow<List<MedicationRecord>> =
        recordDao.getRecordsByDate(date)

    suspend fun getRecordForMedicineOnDate(medicineId: Int, date: String): MedicationRecord? =
        recordDao.getRecordForMedicineOnDate(medicineId, date)

    fun getTodayPendingRecords(date: String): Flow<List<MedicationRecord>> =
        recordDao.getTodayPendingRecords(date)

    suspend fun insertRecord(record: MedicationRecord): Long = recordDao.insertRecord(record)

    suspend fun updateRecord(record: MedicationRecord) = recordDao.updateRecord(record)

    suspend fun updateRecordStatus(
        recordId: Int,
        status: MedicationStatus,
        confirmationTime: String? = null
    ) = recordDao.updateStatus(recordId, status, confirmationTime)

    // Auto-generate today's records from active medicines
    suspend fun generateTodayRecords(todayDate: String, existingRecords: List<MedicationRecord>) {
        val medicines = medicineDao.getAllActiveMedicines()
        // This is called from ViewModel with existing records check
    }
}
