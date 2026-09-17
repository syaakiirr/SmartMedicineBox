package com.example.smartmedicinebox.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smartmedicinebox.ui.viewmodel.AiAssistantState
import com.example.smartmedicinebox.ui.viewmodel.AiAssistantViewModel
import com.example.smartmedicinebox.ui.viewmodel.AiChatMessage
import com.example.smartmedicinebox.ui.viewmodel.AiMessageRole

private val suggestedQuestions = listOf(
    "Paracetamol untuk apa?",
    "Saya demam dan sakit kepala",
    "Apa kesan sampingan amlodipine?"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantScreen(viewModel: AiAssistantViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    var messageText by rememberSaveable { mutableStateOf("") }
    var showPrivacyDialog by rememberSaveable { mutableStateOf(!state.hasAcceptedPrivacy) }
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size, state.isLoading) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size + 1)
        }
    }

    if (showPrivacyDialog) {
        PrivacyNoticeDialog(
            onAccept = {
                viewModel.acceptPrivacyNotice()
                showPrivacyDialog = false
            },
            onDismiss = { showPrivacyDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("AI Medicine Assistant")
                        Text(
                            "General medicine and symptom guidance",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
            )
        },
        bottomBar = {
            MessageComposer(
                value = messageText,
                enabled = !state.isLoading,
                onValueChange = { messageText = it.take(AiAssistantViewModel.MAX_MESSAGE_LENGTH) },
                onSend = {
                    viewModel.sendMessage(messageText)
                    messageText = ""
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { MedicalSafetyCard() }
            item { ConnectionBanner(state) }

            state.errorMessage?.let { error ->
                item {
                    ErrorCard(error = error, onDismiss = viewModel::dismissError)
                }
            }

            if (state.messages.isEmpty()) {
                item {
                    EmptyChatIntro(
                        onSuggestionClick = {
                            messageText = it
                            viewModel.sendMessage(it)
                            messageText = ""
                        },
                        suggestionsEnabled = state.hasAcceptedPrivacy && state.hasApiKey && state.hasInternet
                    )
                }
            } else {
                items(state.messages, key = { it.id }) { message ->
                    MessageBubble(message)
                }
            }

            if (state.isLoading) {
                item { LoadingBubble() }
            }
        }
    }
}

@Composable
private fun MedicalSafetyCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                "General information only. This AI cannot diagnose illness or replace a doctor or pharmacist. Call 999 for a medical emergency.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun ConnectionBanner(state: AiAssistantState) {
    val message = when {
        !state.hasAcceptedPrivacy -> "Review the privacy notice before sending health information to Google Gemini."
        !state.hasApiKey -> "AI access is not configured in this app build."
        state.hasInternet -> "AI online. Mobile data can stay on while SmartMedBox Wi-Fi is connected."
        else -> "AI needs internet. Turn on mobile data or connect to an internet-enabled Wi-Fi."
    }
    Surface(
        color = if (state.hasApiKey && state.hasInternet) {
            MaterialTheme.colorScheme.tertiaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        shape = MaterialTheme.shapes.small
    ) {
        Text(message, modifier = Modifier.fillMaxWidth().padding(12.dp), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun EmptyChatIntro(
    onSuggestionClick: (String) -> Unit,
    suggestionsEnabled: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primaryContainer) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.padding(18.dp).size(32.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Text("Ask about a medicine or symptom", style = MaterialTheme.typography.titleMedium)
        Text(
            "Type a medicine name, active ingredient, or describe what you are experiencing.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(suggestedQuestions) { suggestion ->
                SuggestionChip(
                    onClick = { onSuggestionClick(suggestion) },
                    enabled = suggestionsEnabled,
                    label = { Text(suggestion) }
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(message: AiChatMessage) {
    val isUser = message.role == AiMessageRole.USER
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.88f),
            color = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    if (isUser) "You" else "AI Assistant",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                )
                Text(
                    message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun LoadingBubble() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.medium) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Text("Preparing a response...", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun ErrorCard(error: String, onDismiss: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                error,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Dismiss error")
            }
        }
    }
}

@Composable
private fun MessageComposer(
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 2.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().imePadding().padding(12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask about a medicine or symptom") },
                minLines = 1,
                maxLines = 4,
                enabled = enabled,
                shape = MaterialTheme.shapes.medium,
                supportingText = {
                    Text("${value.length}/${AiAssistantViewModel.MAX_MESSAGE_LENGTH}")
                }
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onSend, enabled = enabled && value.isNotBlank()) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send question")
            }
        }
    }
}

@Composable
private fun PrivacyNoticeDialog(
    onAccept: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Before using AI") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Your questions and recent chat context are sent to Google Gemini to generate an answer. Do not include your full name, ID number, address, or other identifying details.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Google processes prompts and responses according to the Gemini API terms and the data policy for your API plan. Free and paid plans may handle data differently, so avoid submitting sensitive information.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "AI answers may be incomplete or outdated. Verify medicine information with the product label, pharmacist, or doctor.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            Button(onClick = onAccept) { Text("I understand") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Not now") }
        }
    )
}
