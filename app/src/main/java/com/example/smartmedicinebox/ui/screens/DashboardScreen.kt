package com.example.smartmedicinebox.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.smartmedicinebox.ui.components.MedicationStatusBadge
import com.example.smartmedicinebox.ui.components.RecordStatusCard
import com.example.smartmedicinebox.data.remote.DeviceConnectionStatus
import com.example.smartmedicinebox.ui.viewmodel.MedicineViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MedicineViewModel,
    userName: String
) {
    val todayRecords by viewModel.todayRecords.collectAsState()
    val medicines by viewModel.allMedicines.collectAsState()
    val deviceConnection by viewModel.deviceConnection.collectAsState()
    val nextMedicine = viewModel.getNextMedicine()
    val greeting = if (userName.isBlank()) "Your medicines" else "Hello, $userName"
    var showDeviceSetup by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val isOnline = deviceConnection.status == DeviceConnectionStatus.CONNECTED

    if (showDeviceSetup) {
        DeviceSetupDialog(
            initialAddress = viewModel.deviceConfig().baseUrl,
            initialToken = viewModel.deviceConfig().token,
            deviceName = deviceConnection.deviceName,
            connectionStatus = deviceConnection.status,
            connectionMessage = deviceConnection.message,
            onDismiss = { showDeviceSetup = false },
            onSave = { address, token ->
                viewModel.configureDevice(address, token)
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            greeting,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Smart Medicine Box",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    Surface(
                        modifier = Modifier.heightIn(min = 48.dp),
                        onClick = { showDeviceSetup = true },
                        shape = MaterialTheme.shapes.small,
                        color = when (deviceConnection.status) {
                            DeviceConnectionStatus.CONNECTED -> MaterialTheme.colorScheme.primaryContainer
                            DeviceConnectionStatus.DISCONNECTED -> MaterialTheme.colorScheme.errorContainer
                            else -> MaterialTheme.colorScheme.secondaryContainer
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (isOnline) Icons.Default.Wifi else Icons.Default.WifiOff,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = deviceConnection.message,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(start = 16.dp, top = 20.dp, end = 16.dp, bottom = 24.dp)
        ) {
            item {
                Text("Next medicine", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                if (nextMedicine != null) {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = nextMedicine.scheduledTime,
                                        style = MaterialTheme.typography.headlineLarge,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = nextMedicine.medicineName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            "BOX",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            medicines.firstOrNull { it.medicineId == nextMedicine.medicineId }
                                                ?.compartment?.toString() ?: "-",
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = nextMedicine.dosage,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                MedicationStatusBadge(nextMedicine.status)
                            }
                        }
                    }
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Column {
                                Text(
                                    "Today's schedule is complete",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    "No more box access is due today.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Today's schedule", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "${todayRecords.size} scheduled",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (todayRecords.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Default.Inventory2,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text("Nothing scheduled today", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Add a medicine from the Schedule tab.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(todayRecords.sortedBy { it.scheduledTime }, key = { it.recordId }) { record ->
                    RecordStatusCard(
                        record = record,
                        compartment = medicines.firstOrNull { it.medicineId == record.medicineId }?.compartment,
                        onConfirm = {
                            viewModel.confirmMedicineTaken(it)
                            scope.launch { snackbarHostState.showSnackbar("Box access recorded") }
                        },
                        onMissed = {
                            viewModel.markAsMissed(it)
                            scope.launch { snackbarHostState.showSnackbar("Marked as missed") }
                        },
                        showActions = true
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceSetupDialog(
    initialAddress: String,
    initialToken: String,
    deviceName: String?,
    connectionStatus: DeviceConnectionStatus,
    connectionMessage: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var address by remember(initialAddress) { mutableStateOf(initialAddress) }
    var token by remember(initialToken) { mutableStateOf(initialToken) }
    val canSave = address.isNotBlank() && token.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (deviceName == null) "Connect medicine box" else deviceName) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Keep this phone and the medicine box on the same Wi-Fi. Enter the address and pairing code provided during box setup.",
                    style = MaterialTheme.typography.bodyMedium
                )
                when (connectionStatus) {
                    DeviceConnectionStatus.CHECKING -> LinearProgressIndicator(Modifier.fillMaxWidth())
                    DeviceConnectionStatus.CONNECTED -> Text(
                        "Connected. Schedules are ready to sync.",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    DeviceConnectionStatus.DISCONNECTED -> Text(
                        "$connectionMessage. Check the Wi-Fi, address, and pairing code, then try again.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    else -> Unit
                }
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Medicine box address") },
                    placeholder = { Text("192.168.0.50") },
                    supportingText = { Text("For example, 192.168.0.50") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text("Pairing code") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (connectionStatus == DeviceConnectionStatus.CONNECTED) onDismiss()
                    else onSave(address, token)
                },
                enabled = canSave && connectionStatus != DeviceConnectionStatus.CHECKING
            ) {
                Text(if (connectionStatus == DeviceConnectionStatus.CONNECTED) "Done" else "Connect")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
