package com.example.smartmedicinebox.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
                    Text("Medicine Assistant")
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
            item { MedicalSafetyNotice() }
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
private fun MedicalSafetyNotice() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Warning,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "General information only. Not a diagnosis. Emergency: call 999.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ConnectionBanner(state: AiAssistantState) {
    val message = when {
        !state.hasAcceptedPrivacy -> "Review the privacy notice before sending health information to Google Gemini."
        !state.hasApiKey -> "AI access is not configured in this app build."
        state.hasInternet -> return
        else -> "AI needs internet. Turn on mobile data or connect to an internet-enabled Wi-Fi."
    }
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
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
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("What can I help with?", style = MaterialTheme.typography.titleMedium)
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
            Column(modifier = Modifier.padding(14.dp)) {
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
                supportingText = if (value.length >= 900) {
                    { Text("${value.length}/${AiAssistantViewModel.MAX_MESSAGE_LENGTH}") }
                } else null
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
                    "Questions and recent chat are sent to Google Gemini and handled under its API terms. Do not include personal or sensitive details.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "AI answers may be wrong or outdated and are not a diagnosis. Verify medicine information with the label, pharmacist, or doctor.",
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
