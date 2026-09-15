package com.example.smartmedicinebox.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartmedicinebox.data.model.MedicationRecord
import com.example.smartmedicinebox.data.model.MedicationStatus
import com.example.smartmedicinebox.ui.components.MedicationStatusBadge
import com.example.smartmedicinebox.ui.viewmodel.MedicineViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaregiverScreen(
    viewModel: MedicineViewModel
) {
    val todayRecords by viewModel.todayRecords.collectAsState()
    val allRecords by viewModel.allRecords.collectAsState()
    val medicines by viewModel.allMedicines.collectAsState()

    val confirmedCount = todayRecords.count { it.status == MedicationStatus.CONFIRMED }
    val missedCount    = todayRecords.count { it.status == MedicationStatus.MISSED }
    val dueCount       = todayRecords.count { it.status == MedicationStatus.DUE }
    val pendingCount   = todayRecords.count { it.status == MedicationStatus.PENDING }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Caregiver overview", fontWeight = FontWeight.Bold)
                        Text(
                            "Medicine box activity",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Summary Overview Cards
            item {
                Text("Today's Overview", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OverviewCard(
                        Icons.Default.CheckCircle, "$confirmedCount", "Accessed",
                        MaterialTheme.colorScheme.tertiaryContainer,
                        MaterialTheme.colorScheme.onTertiaryContainer,
                        Modifier.weight(1f)
                    )
                    OverviewCard(
                        Icons.Default.Cancel, "$missedCount", "Missed",
                        if (missedCount > 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                        if (missedCount > 0) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface,
                        Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OverviewCard(
                        Icons.Default.NotificationsActive, "$dueCount", "Due now",
                        if (dueCount > 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                        if (dueCount > 0) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface,
                        Modifier.weight(1f)
                    )
                    OverviewCard(
                        Icons.Default.Schedule, "$pendingCount", "Pending",
                        MaterialTheme.colorScheme.secondaryContainer,
                        MaterialTheme.colorScheme.onSecondaryContainer,
                        Modifier.weight(1f)
                    )
                }
            }

            // Alert for missed medicines
            if (missedCount > 0) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    "No box access recorded",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    if (missedCount == 1) "One scheduled access was missed today. Check in with the patient."
                                    else "$missedCount scheduled accesses were missed today. Check in with the patient.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }

            // Due alert
            if (dueCount > 0) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Box access due", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text(
                                    if (dueCount == 1) "One medicine is due now. Check that the patient can reach the correct box."
                                    else "$dueCount medicines are due now. Check that the patient can reach the correct boxes.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }

            // Patient Medication Status
            item {
                Text("Today's schedule", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }

            if (todayRecords.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No medicines scheduled today.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(todayRecords.sortedBy { it.scheduledTime }) { record ->
                    CaregiverRecordRow(
                        record,
                        medicines.firstOrNull { it.medicineId == record.medicineId }?.compartment
                    )
                }
            }

            // Recent History
            if (allRecords.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                    Text("Recent box activity", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                val cutoffDate = LocalDate.now().minusDays(6)
                val recentHistory = allRecords
                    .filter { runCatching { LocalDate.parse(it.scheduledDate) >= cutoffDate }.getOrDefault(false) }
                    .sortedByDescending { it.scheduledDate + it.scheduledTime }
                items(recentHistory) { record ->
                    CaregiverHistoryRow(record)
                }
            }
        }
    }
}

@Composable
private fun OverviewCard(
    icon: ImageVector,
    count: String,
    label: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = contentColor)
            Text(count, style = MaterialTheme.typography.headlineSmall, color = contentColor)
            Text(label, style = MaterialTheme.typography.labelMedium, color = contentColor)
        }
    }
}

@Composable
private fun CaregiverRecordRow(record: MedicationRecord, compartment: Int?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(record.medicineName, fontWeight = FontWeight.SemiBold)
                    Text(
                        buildString {
                            append("${record.scheduledTime}  •  ${record.dosage}")
                            compartment?.let { append("  •  Box $it") }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    record.confirmationTime?.let {
                        Text("Box accessed at $it", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
                    }
                }
            }
            MedicationStatusBadge(record.status)
        }
    }
}

@Composable
private fun CaregiverHistoryRow(record: MedicationRecord) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(record.medicineName, style = MaterialTheme.typography.bodyMedium)
        Text(
            "${formatCaregiverDate(record.scheduledDate)}  ${record.scheduledTime}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatCaregiverDate(date: String): String = runCatching {
    LocalDate.parse(date).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
}.getOrDefault(date)
