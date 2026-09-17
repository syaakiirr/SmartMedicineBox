package com.example.smartmedicinebox.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

data class AiApiMessage(val role: String, val content: String)

class OpenAiClient(context: Context) {
    private val connectivityManager = context.getSystemService(ConnectivityManager::class.java)

    @Suppress("DEPRECATION")
    suspend fun ask(apiKey: String, messages: List<AiApiMessage>): String = withContext(Dispatchers.IO) {
        require(apiKey.isNotBlank()) { "OpenAI API key is not configured" }

        val input = JSONArray()
        messages.takeLast(MAX_HISTORY_MESSAGES).forEach { message ->
            input.put(
                JSONObject()
                    .put("role", message.role)
                    .put("content", message.content)
            )
        }
        val requestBody = JSONObject()
            .put("model", MODEL)
            .put("instructions", MEDICAL_INSTRUCTIONS)
            .put("input", input)
            .put("max_output_tokens", MAX_OUTPUT_TOKENS)
            .put("store", false)

        val url = URL(RESPONSES_URL)
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
        val connection = (internetNetwork?.openConnection(url) ?: url.openConnection()) as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.doOutput = true
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
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
                throw OpenAiApiException(responseCode, errorMessage(responseCode, responseBody))
            }
            parseResponse(responseBody)
        } catch (_: SocketTimeoutException) {
            throw OpenAiApiException(408, "The AI request timed out. Check your internet and try again.")
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        private const val RESPONSES_URL = "https://api.openai.com/v1/responses"
        private const val MODEL = "gpt-4o-mini"
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 45_000
        private const val MAX_HISTORY_MESSAGES = 10
        private const val MAX_OUTPUT_TOKENS = 700

        private val MEDICAL_INSTRUCTIONS = """
            You are the Smart Medicine Box AI assistant. Give clear, concise general health information in the same language as the user (Malay or English).

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
            if (root.optString("status") == "incomplete") {
                throw OpenAiApiException(502, "The AI response was incomplete. Please ask again with a shorter question.")
            }
            val output = root.optJSONArray("output") ?: JSONArray()
            val textParts = mutableListOf<String>()
            for (outputIndex in 0 until output.length()) {
                val item = output.optJSONObject(outputIndex) ?: continue
                val content = item.optJSONArray("content") ?: continue
                for (contentIndex in 0 until content.length()) {
                    val part = content.optJSONObject(contentIndex) ?: continue
                    when (part.optString("type")) {
                        "output_text" -> part.optString("text").takeIf { it.isNotBlank() }?.let(textParts::add)
                        "refusal" -> part.optString("refusal").takeIf { it.isNotBlank() }?.let { return it }
                    }
                }
            }
            if (textParts.isNotEmpty()) return textParts.joinToString("\n")
            throw OpenAiApiException(502, "The AI returned an empty response. Please try again.")
        }

        private fun errorMessage(responseCode: Int, responseBody: String): String {
            val apiMessage = runCatching {
                JSONObject(responseBody).optJSONObject("error")?.optString("message")
            }.getOrNull().orEmpty()
            return when (responseCode) {
                401 -> "The OpenAI API key is invalid. Update it in AI settings."
                429 -> "The OpenAI API quota or rate limit was reached. Check API billing and try again."
                in 500..599 -> "The AI service is temporarily unavailable. Please try again."
                else -> apiMessage.ifBlank { "The AI request failed (HTTP $responseCode)." }
            }
        }
    }
}

class OpenAiApiException(val responseCode: Int, message: String) : Exception(message)
