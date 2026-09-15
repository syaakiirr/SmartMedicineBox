package com.example.smartmedicinebox.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartmedicinebox.data.database.AppDatabase
import com.example.smartmedicinebox.data.model.Medicine
import com.example.smartmedicinebox.data.model.MedicationRecord
import com.example.smartmedicinebox.data.model.MedicationStatus
import com.example.smartmedicinebox.data.repository.MedicineRepository
import com.example.smartmedicinebox.data.remote.DeviceApiException
import com.example.smartmedicinebox.data.remote.DeviceConfig
import com.example.smartmedicinebox.data.remote.DeviceConnectionState
import com.example.smartmedicinebox.data.remote.DeviceConnectionStatus
import com.example.smartmedicinebox.data.remote.DeviceEvent
import com.example.smartmedicinebox.data.remote.Esp32Client
import com.example.smartmedicinebox.notification.MedicationAlarmScheduler
import com.example.smartmedicinebox.notification.NotificationHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class MedicineViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = MedicineRepository(db.medicineDao(), db.medicationRecordDao())
    private val deviceClient = Esp32Client(application)
    private val deviceCheckMutex = Mutex()

    val allMedicines: StateFlow<List<Medicine>> = repository.allActiveMedicines
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRecords: StateFlow<List<MedicationRecord>> = repository.allRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _todayRecords = MutableStateFlow<List<MedicationRecord>>(emptyList())
    val todayRecords: StateFlow<List<MedicationRecord>> = _todayRecords

    private val _selectedMedicine = MutableStateFlow<Medicine?>(null)
    val selectedMedicine: StateFlow<Medicine?> = _selectedMedicine

    private val _deviceConnection = MutableStateFlow(DeviceConnectionState())
    val deviceConnection: StateFlow<DeviceConnectionState> = _deviceConnection

    private val today: String
        get() = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

    init {
        loadTodayRecords()
        monitorDevice()
        viewModelScope.launch { MedicationAlarmScheduler.scheduleAll(application) }
    }

    private fun loadTodayRecords() {
        viewModelScope.launch {
            repository.getRecordsByDate(today).collect { records ->
                _todayRecords.value = records
                // Auto-generate records for today's active medicines if not yet created
                generateMissingTodayRecords(records)
            }
        }
    }

    private suspend fun generateMissingTodayRecords(existingRecords: List<MedicationRecord>) {
        val medicines = allMedicines.value
        val existingMedicineIds = existingRecords.map { it.medicineId }.toSet()
        medicines.forEach { medicine ->
            if (medicine.medicineId !in existingMedicineIds) {
                val newRecord = MedicationRecord(
                    medicineId = medicine.medicineId,
                    medicineName = medicine.medicineName,
                    dosage = medicine.dosage,
                    scheduledDate = today,
                    scheduledTime = medicine.scheduledTime,
                    status = MedicationStatus.PENDING
                )
                repository.insertRecord(newRecord)
            }
        }
        // Update any PENDING records whose time has passed to DUE
        updateDueStatuses(existingRecords)
    }

    private suspend fun updateDueStatuses(records: List<MedicationRecord>) {
        val nowTime = LocalTime.now()
        records.filter { it.status == MedicationStatus.PENDING }.forEach { record ->
            try {
                val scheduledTime = LocalTime.parse(record.scheduledTime, DateTimeFormatter.ofPattern("HH:mm"))
                if (nowTime.isAfter(scheduledTime)) {
                    repository.updateRecordStatus(record.recordId, MedicationStatus.DUE)
                }
            } catch (_: Exception) {}
        }
    }

    fun insertMedicine(medicine: Medicine) {
        viewModelScope.launch {
            val id = repository.insertMedicine(medicine)
            val savedMedicine = medicine.copy(medicineId = id.toInt())
            // Create today's record for the new medicine
            val record = MedicationRecord(
                medicineId = id.toInt(),
                medicineName = medicine.medicineName,
                dosage = medicine.dosage,
                scheduledDate = today,
                scheduledTime = medicine.scheduledTime,
                status = MedicationStatus.PENDING
            )
            repository.insertRecord(record)
            MedicationAlarmScheduler.schedule(getApplication(), savedMedicine)
            syncMedicineWithDevice(savedMedicine)
        }
    }

    fun updateMedicine(medicine: Medicine) {
        viewModelScope.launch {
            repository.updateMedicine(medicine)
            MedicationAlarmScheduler.schedule(getApplication(), medicine)
            syncMedicineWithDevice(medicine)
        }
    }

    fun deleteMedicine(medicine: Medicine) {
        viewModelScope.launch {
            repository.deleteMedicine(medicine)
            MedicationAlarmScheduler.cancel(getApplication(), medicine.medicineId)
            if (_deviceConnection.value.status == DeviceConnectionStatus.CONNECTED) {
                runCatching { deviceClient.deleteMedicine(medicine.medicineId) }
                    .onFailure { setDisconnected(it) }
            }
        }
    }

    fun selectMedicine(medicine: Medicine?) {
        _selectedMedicine.value = medicine
    }

    fun confirmMedicineTaken(record: MedicationRecord) {
        NotificationHelper.cancelMedicineReminder(getApplication(), record.medicineId)
        viewModelScope.launch {
            val now = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
            repository.updateRecordStatus(record.recordId, MedicationStatus.CONFIRMED, now)
            acknowledgeDevice(record.medicineId, MedicationStatus.CONFIRMED)
        }
    }

    fun markAsMissed(record: MedicationRecord) {
        NotificationHelper.cancelMedicineReminder(getApplication(), record.medicineId)
        viewModelScope.launch {
            repository.updateRecordStatus(record.recordId, MedicationStatus.MISSED)
            acknowledgeDevice(record.medicineId, MedicationStatus.MISSED)
        }
    }

    fun getNextMedicine(): MedicationRecord? {
        val now = LocalTime.now()
        return todayRecords.value
            .filter { it.status == MedicationStatus.PENDING || it.status == MedicationStatus.DUE }
            .sortedBy { it.scheduledTime }
            .firstOrNull()
    }

    fun deviceConfig(): DeviceConfig = deviceClient.config()

    fun configureDevice(address: String, token: String) {
        deviceClient.saveConfig(address, token)
        _deviceConnection.value = DeviceConnectionState(
            status = DeviceConnectionStatus.CHECKING,
            message = "Checking"
        )
        viewModelScope.launch { checkDevice(syncSchedules = true) }
    }

    fun refreshDeviceConnection() {
        viewModelScope.launch { checkDevice(syncSchedules = true) }
    }

    private fun monitorDevice() {
        viewModelScope.launch {
            while (true) {
                checkDevice(syncSchedules = false)
                delay(DEVICE_POLL_INTERVAL_MS)
            }
        }
    }

    private suspend fun checkDevice(syncSchedules: Boolean) = deviceCheckMutex.withLock {
        val config = deviceClient.config()
        if (config.baseUrl.isBlank() || config.token.isBlank()) {
            _deviceConnection.value = DeviceConnectionState()
            return@withLock
        }

        val wasConnected = _deviceConnection.value.status == DeviceConnectionStatus.CONNECTED
        _deviceConnection.value = _deviceConnection.value.copy(
            status = DeviceConnectionStatus.CHECKING,
            message = "Checking"
        )
        runCatching {
            val info = deviceClient.health()
            _deviceConnection.value = DeviceConnectionState(
                status = DeviceConnectionStatus.CONNECTED,
                deviceName = info.deviceId,
                message = "Connected"
            )
            if (syncSchedules || !wasConnected) syncAllMedicines()
            processLatestDeviceEvent()
        }.onFailure { setDisconnected(it) }
    }

    private suspend fun syncAllMedicines() {
        repository.getAllActiveMedicinesOnce().forEach { deviceClient.syncMedicine(it) }
    }

    private suspend fun syncMedicineWithDevice(medicine: Medicine) {
        if (_deviceConnection.value.status != DeviceConnectionStatus.CONNECTED) return
        runCatching { deviceClient.syncMedicine(medicine) }
            .onFailure { setDisconnected(it) }
    }

    private suspend fun acknowledgeDevice(medicineId: Int, status: MedicationStatus) {
        if (_deviceConnection.value.status != DeviceConnectionStatus.CONNECTED) return
        runCatching { deviceClient.acknowledgeMedicine(medicineId, status.name) }
            .onFailure { setDisconnected(it) }
    }

    private suspend fun processLatestDeviceEvent() {
        val event = deviceClient.latestEvent() ?: return
        if (event.sequence <= deviceClient.lastEventSequence()) return

        applyDeviceEvent(event)
        deviceClient.markEventProcessed(event.sequence)
    }

    private suspend fun applyDeviceEvent(event: DeviceEvent) {
        val record = repository.getRecordForMedicineOnDate(event.medicineId, today) ?: return
        val status = when (event.status) {
            MedicationStatus.CONFIRMED.name -> MedicationStatus.CONFIRMED
            MedicationStatus.MISSED.name -> MedicationStatus.MISSED
            else -> return
        }
        repository.updateRecordStatus(record.recordId, status, event.confirmationTime)
        NotificationHelper.cancelMedicineReminder(getApplication(), event.medicineId)
    }

    private fun setDisconnected(error: Throwable) {
        Log.e("MedicineViewModel", "ESP32 request failed", error)
        val message = when (error) {
            is DeviceApiException -> if (error.responseCode == 401) "Pairing token rejected" else "Device error"
            else -> "Not reachable"
        }
        _deviceConnection.value = DeviceConnectionState(
            status = DeviceConnectionStatus.DISCONNECTED,
            message = message
        )
    }

    companion object {
        private const val DEVICE_POLL_INTERVAL_MS = 10_000L
    }
}
