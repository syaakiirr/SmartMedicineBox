package com.example.smartmedicinebox.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

data class AiApiMessage(val role: String, val content: String)

class GeminiClient(context: Context) {
    private val connectivityManager = context.getSystemService(ConnectivityManager::class.java)

    @Suppress("DEPRECATION")
    suspend fun ask(apiKey: String, messages: List<AiApiMessage>): String = withContext(Dispatchers.IO) {
        require(apiKey.isNotBlank()) { "Gemini API key is not configured" }

        val contents = JSONArray()
        messages.takeLast(MAX_HISTORY_MESSAGES).forEach { message ->
            contents.put(
                JSONObject()
                    .put("role", if (message.role == "assistant") "model" else "user")
                    .put("parts", JSONArray().put(JSONObject().put("text", message.content)))
            )
        }
        val requestBody = JSONObject()
            .put(
                "system_instruction",
                JSONObject().put("parts", JSONArray().put(JSONObject().put("text", MEDICAL_INSTRUCTIONS)))
            )
            .put("contents", contents)
            .put(
                "generationConfig",
                JSONObject()
                    .put("maxOutputTokens", MAX_OUTPUT_TOKENS)
                    .put("temperature", 0.2)
            )

        val url = URL(GEMINI_URL)
        fun isValidated(network: android.net.Network): Boolean {
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }
        val activeNetwork = connectivityManager.activeNetwork?.takeIf(::isValidated)
        val internetNetwork = activeNetwork ?: connectivityManager.allNetworks
            .filter(::isValidated)
            .maxByOrNull { network ->
                val capabilities = connectivityManager.getNetworkCapabilities(network)
                if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true) 1 else 0
            }
        var retryAttempt = 0
        while (true) {
            val connection = (internetNetwork?.openConnection(url) ?: url.openConnection()) as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.connectTimeout = CONNECT_TIMEOUT_MS
                connection.readTimeout = READ_TIMEOUT_MS
                connection.doOutput = true
                connection.setRequestProperty("x-goog-api-key", apiKey)
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")
                connection.outputStream.bufferedWriter().use { it.write(requestBody.toString()) }

                val responseCode = connection.responseCode
                val responseStream = if (responseCode in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }
                val responseBody = responseStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (responseCode !in 200..299) {
                    if (shouldRetry(responseCode, retryAttempt)) {
                        retryAttempt++
                        delay(RETRY_BASE_DELAY_MS * retryAttempt)
                        continue
                    }
                    throw GeminiApiException(responseCode, errorMessage(responseCode, responseBody))
                }
                return@withContext parseResponse(responseBody)
            } catch (_: SocketTimeoutException) {
                if (retryAttempt < MAX_RETRY_ATTEMPTS) {
                    retryAttempt++
                    delay(RETRY_BASE_DELAY_MS * retryAttempt)
                    continue
                }
                throw GeminiApiException(408, "The AI request timed out. Check your internet and try again.")
            } finally {
                connection.disconnect()
            }
        }
        error("Unreachable retry state")
    }

    companion object {
        private const val GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent"
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 45_000
        private const val MAX_HISTORY_MESSAGES = 10
        private const val MAX_OUTPUT_TOKENS = 4_096
        private const val MAX_RETRY_ATTEMPTS = 2
        private const val RETRY_BASE_DELAY_MS = 750L

        private val MEDICAL_INSTRUCTIONS = """
            You are the Smart Medicine Box AI assistant. Give clear, concise general health information in the same language as the user (Malay or English). Keep routine answers under 250 words unless the user asks for more detail.

            MEDICINE QUESTIONS:
            - Explain common uses, active ingredients, common side effects, important warnings, and when to ask a pharmacist or doctor.
            - If a brand name is ambiguous, ask for the active ingredient, strength, and country instead of guessing.
            - Never prescribe, recommend starting or stopping a medicine, or give a personalized dose. Tell users to follow their prescription or product label and consult a pharmacist or doctor.

            SYMPTOM QUESTIONS:
            - Do not diagnose. Describe a short list of possible common causes using uncertainty language such as "may" or "could".
            - Ask relevant follow-up questions when information is insufficient.
            - Give safe self-care guidance only when appropriate, plus when to seek a clinic or pharmacist.
            - Clearly advise urgent care or Malaysia emergency number 999 for red flags such as chest pain, severe trouble breathing, fainting, new weakness on one side, confusion, seizure, severe allergic reaction, uncontrolled bleeding, or thoughts of self-harm.
            - For pregnancy, children, older adults, chronic disease, or possible medicine interactions, advise professional review.

            End health answers with a brief reminder that the information is general and not a medical diagnosis. Do not claim that a medicine has been identified from text alone with certainty.
            Use plain text with short paragraphs or simple bullet characters, not Markdown tables. Remind users that medicine information may be incomplete or outdated and should be verified against the product label or with a pharmacist.
        """.trimIndent()

        internal fun parseResponse(responseBody: String): String {
            val root = JSONObject(responseBody)
            val blockReason = root.optJSONObject("promptFeedback")?.optString("blockReason").orEmpty()
            if (blockReason.isNotBlank()) {
                throw GeminiApiException(400, "Gemini blocked this request for safety. Rephrase the question and try again.")
            }
            val candidates = root.optJSONArray("candidates") ?: JSONArray()
            if (candidates.length() == 0) {
                throw GeminiApiException(502, "Gemini returned an empty response. Please try again.")
            }
            val candidate = candidates.optJSONObject(0) ?: JSONObject()
            val finishReason = candidate.optString("finishReason")
            if (finishReason == "MAX_TOKENS") {
                throw GeminiApiException(502, "The AI response was incomplete. Please ask again with a shorter question.")
            }
            if (finishReason.isNotBlank() && finishReason != "STOP") {
                throw GeminiApiException(502, "Gemini could not complete the response ($finishReason). Please rephrase and try again.")
            }
            val parts = candidate.optJSONObject("content")?.optJSONArray("parts") ?: JSONArray()
            val textParts = mutableListOf<String>()
            for (partIndex in 0 until parts.length()) {
                parts.optJSONObject(partIndex)
                    ?.optString("text")
                    ?.takeIf { it.isNotBlank() }
                    ?.let(textParts::add)
            }
            if (textParts.isNotEmpty()) return textParts.joinToString("\n")
            throw GeminiApiException(502, "Gemini returned an empty response. Please try again.")
        }

        internal fun shouldRetry(responseCode: Int, retryAttempt: Int): Boolean =
            responseCode in 500..599 && retryAttempt < MAX_RETRY_ATTEMPTS

        private fun errorMessage(responseCode: Int, responseBody: String): String {
            val apiMessage = runCatching {
                JSONObject(responseBody).optJSONObject("error")?.optString("message")
            }.getOrNull().orEmpty()
            return when (responseCode) {
                400, 401, 403 -> if (apiMessage.contains("API key", ignoreCase = true)) {
                    "AI access is invalid or restricted. Install the latest app or contact the maintainer."
                } else {
                    apiMessage.ifBlank { "Gemini rejected the request. Please check the question and try again." }
                }
                404 -> "The selected Gemini model is unavailable. Please update the app or try again later."
                429 -> "The Gemini API quota or rate limit was reached. Check the API plan and try again."
                in 500..599 -> "The AI service is temporarily unavailable. Please try again."
                else -> apiMessage.ifBlank { "The AI request failed (HTTP $responseCode)." }
            }
        }
    }
}

class GeminiApiException(val responseCode: Int, message: String) : Exception(message)
