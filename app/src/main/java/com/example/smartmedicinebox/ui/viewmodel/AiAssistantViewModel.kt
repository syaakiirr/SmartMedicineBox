package com.example.smartmedicinebox.ui.viewmodel

import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartmedicinebox.data.remote.AiApiMessage
import com.example.smartmedicinebox.data.remote.AiKeyStore
import com.example.smartmedicinebox.data.remote.GeminiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

enum class AiMessageRole { USER, ASSISTANT }

data class AiChatMessage(
    val id: Long,
    val role: AiMessageRole,
    val content: String
)

data class AiAssistantState(
    val messages: List<AiChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val hasApiKey: Boolean = false,
    val hasAcceptedPrivacy: Boolean = false,
    val hasInternet: Boolean = false,
    val errorMessage: String? = null
)

class AiAssistantViewModel(application: Application) : AndroidViewModel(application) {
    private val keyStore = AiKeyStore(application)
    private val geminiClient = GeminiClient(application)
    private val connectivityManager = application.getSystemService(ConnectivityManager::class.java)
    private val messageIds = AtomicLong(0)
    private val _state = MutableStateFlow(
        AiAssistantState(
            hasApiKey = keyStore.hasKey(),
            hasAcceptedPrivacy = keyStore.hasAcceptedPrivacy(),
            hasInternet = hasValidatedInternet()
        )
    )
    val state: StateFlow<AiAssistantState> = _state.asStateFlow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = updateInternetState()
        override fun onLost(network: Network) = updateInternetState()
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) =
            updateInternetState()
    }

    init {
        connectivityManager.registerNetworkCallback(
            NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build(),
            networkCallback
        )
    }

    fun acceptPrivacyNotice() {
        keyStore.acceptPrivacyNotice()
        _state.update { it.copy(hasAcceptedPrivacy = true, errorMessage = null) }
    }

    fun sendMessage(text: String) {
        val question = text.trim()
        if (question.isBlank() || _state.value.isLoading) return
        val urgentMessage = urgentSafetyMessage(question)
        if (urgentMessage != null) {
            val userMessage = AiChatMessage(messageIds.incrementAndGet(), AiMessageRole.USER, question)
            val assistantMessage = AiChatMessage(messageIds.incrementAndGet(), AiMessageRole.ASSISTANT, urgentMessage)
            _state.update {
                it.copy(
                    messages = it.messages + userMessage + assistantMessage,
                    isLoading = false,
                    errorMessage = null
                )
            }
            return
        }
        if (!_state.value.hasAcceptedPrivacy) {
            _state.update { it.copy(errorMessage = "Review and accept the AI privacy notice first.") }
            return
        }
        if (question.length > MAX_MESSAGE_LENGTH) {
            _state.update { it.copy(errorMessage = "Keep each question under $MAX_MESSAGE_LENGTH characters.") }
            return
        }
        if (!keyStore.hasKey()) {
            _state.update { it.copy(errorMessage = "AI access is not configured in this app build.") }
            return
        }
        if (!_state.value.hasInternet) {
            _state.update {
                it.copy(errorMessage = "No internet connection. Mobile data can stay on while SmartMedBox Wi-Fi is connected.")
            }
            return
        }

        val userMessage = AiChatMessage(messageIds.incrementAndGet(), AiMessageRole.USER, question)
        _state.update { it.copy(messages = it.messages + userMessage, isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            runCatching {
                val history = _state.value.messages.map { message ->
                    AiApiMessage(
                        role = if (message.role == AiMessageRole.USER) "user" else "assistant",
                        content = message.content
                    )
                }
                geminiClient.ask(keyStore.apiKey(), history)
            }.onSuccess { answer ->
                val assistantMessage = AiChatMessage(
                    messageIds.incrementAndGet(),
                    AiMessageRole.ASSISTANT,
                    answer
                )
                _state.update {
                    it.copy(messages = it.messages + assistantMessage, isLoading = false, errorMessage = null)
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        messages = it.messages.filterNot { message -> message.id == userMessage.id },
                        isLoading = false,
                        errorMessage = error.message ?: "Unable to contact the AI service."
                    )
                }
            }
        }
    }

    fun dismissError() {
        _state.update { it.copy(errorMessage = null) }
    }

    private fun updateInternetState() {
        _state.update { it.copy(hasInternet = hasValidatedInternet()) }
    }

    @Suppress("DEPRECATION")
    private fun hasValidatedInternet(): Boolean {
        return connectivityManager.allNetworks.any { network ->
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }
    }

    override fun onCleared() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
        super.onCleared()
    }

    companion object {
        const val MAX_MESSAGE_LENGTH = 1_000

        internal fun urgentSafetyMessage(text: String): String? {
            val normalized = text.lowercase()
            val redFlags = listOf(
                "chest pain", "sakit dada", "severe trouble breathing", "susah bernafas",
                "sesak nafas", "fainted", "pengsan", "one side weak", "sebelah badan lemah",
                "seizure", "sawan", "severe allergic", "alahan teruk", "uncontrolled bleeding",
                "pendarahan tak berhenti", "suicide", "bunuh diri", "self harm", "cederakan diri"
            )
            if (redFlags.none(normalized::contains)) return null
            return "This may be a medical emergency. Call Malaysia emergency number 999 now or go to the nearest emergency department. Do not wait for an AI response. If possible, stay with another person and tell the emergency team about medicines taken."
        }
    }
}
