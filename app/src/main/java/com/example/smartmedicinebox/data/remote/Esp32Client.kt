package com.example.smartmedicinebox.data.remote

import android.content.Context
import com.example.smartmedicinebox.data.model.Medicine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class DeviceConfig(val baseUrl: String, val token: String)

data class DeviceInfo(
    val deviceId: String,
    val ipAddress: String,
    val firmwareVersion: String
)

data class DeviceEvent(
    val sequence: Long,
    val medicineId: Int,
    val status: String,
    val confirmationTime: String?
)

enum class DeviceConnectionStatus {
    UNCONFIGURED,
    CHECKING,
    CONNECTED,
    DISCONNECTED
}

data class DeviceConnectionState(
    val status: DeviceConnectionStatus = DeviceConnectionStatus.UNCONFIGURED,
    val deviceName: String? = null,
    val message: String = "Not linked"
)

class Esp32Client(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun config(): DeviceConfig = DeviceConfig(
        baseUrl = preferences.getString(KEY_BASE_URL, DEFAULT_HOTSPOT_ADDRESS).orEmpty(),
        token = preferences.getString(KEY_TOKEN, "").orEmpty()
    )

    fun saveConfig(address: String, token: String) {
        val normalizedAddress = address.trim()
            .removeSuffix("/")
            .let { if (it.isNotEmpty() && !it.startsWith("http")) "http://$it" else it }
        preferences.edit()
            .putString(KEY_BASE_URL, normalizedAddress)
            .putString(KEY_TOKEN, token.trim())
            .apply()
    }

    suspend fun health(): DeviceInfo = withContext(Dispatchers.IO) {
        val json = JSONObject(request("GET", "/api/health"))
        DeviceInfo(
            deviceId = json.optString("deviceId", "Smart Medicine Box"),
            ipAddress = json.optString("ip"),
            firmwareVersion = json.optString("firmwareVersion")
        )
    }

    suspend fun syncMedicine(medicine: Medicine) = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("medicineId", medicine.medicineId)
            .put("name", medicine.medicineName)
            .put("dosage", medicine.dosage)
            .put("time", medicine.scheduledTime)
            .put("compartment", medicine.compartment)
            .put("active", medicine.active)
        request("POST", "/api/schedule", body.toString())
        Unit
    }

    suspend fun deleteMedicine(medicineId: Int) = withContext(Dispatchers.IO) {
        request("DELETE", "/api/schedule?id=$medicineId")
        Unit
    }

    suspend fun acknowledgeMedicine(medicineId: Int, status: String) = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("medicineId", medicineId)
            .put("status", status)
        request("POST", "/api/ack", body.toString())
        Unit
    }

    suspend fun latestEvent(): DeviceEvent? = withContext(Dispatchers.IO) {
        val response = request("GET", "/api/event")
        if (response.isBlank() || response == "null") return@withContext null
        val json = JSONObject(response)
        if (!json.has("sequence") || json.optLong("sequence") <= 0L) return@withContext null
        DeviceEvent(
            sequence = json.getLong("sequence"),
            medicineId = json.getInt("medicineId"),
            status = json.getString("status"),
            confirmationTime = json.optString("confirmationTime").takeIf { it.isNotBlank() }
        )
    }

    fun lastEventSequence(): Long = preferences.getLong(KEY_LAST_EVENT, 0L)

    fun markEventProcessed(sequence: Long) {
        preferences.edit().putLong(KEY_LAST_EVENT, sequence).apply()
    }

    private fun request(method: String, path: String, body: String? = null): String {
        val config = config()
        require(config.baseUrl.isNotBlank()) { "Device address is not configured" }
        require(config.token.isNotBlank()) { "Pairing token is not configured" }

        val connection = URL("${config.baseUrl}$path").openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = method
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("X-Device-Key", config.token)
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.bufferedWriter().use { it.write(body) }
            }

            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (responseCode !in 200..299) {
                throw DeviceApiException(responseCode, response.ifBlank { "Device request failed" })
            }
            response
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        private const val PREFERENCES = "esp32_device"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_TOKEN = "token"
        private const val KEY_LAST_EVENT = "last_event_sequence"
        private const val DEFAULT_HOTSPOT_ADDRESS = "http://192.168.4.1"
        private const val CONNECT_TIMEOUT_MS = 2500
        private const val READ_TIMEOUT_MS = 2500
    }
}

class DeviceApiException(val responseCode: Int, message: String) : Exception(message)
