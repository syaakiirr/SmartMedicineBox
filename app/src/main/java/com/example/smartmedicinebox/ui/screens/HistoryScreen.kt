package com.example.smartmedicinebox.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartmedicinebox.data.model.MedicationRecord
import com.example.smartmedicinebox.data.model.MedicationStatus
import com.example.smartmedicinebox.ui.components.MedicationStatusBadge
import com.example.smartmedicinebox.ui.viewmodel.MedicineViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: MedicineViewModel) {
    val allRecords by viewModel.allRecords.collectAsState()
    val groupedRecords = allRecords
        .sortedByDescending { it.scheduledDate }
        .groupBy { it.scheduledDate }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Box Access History", fontWeight = FontWeight.Bold) }
            )
        }
    ) { paddingValues ->
        if (allRecords.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("No box activity yet.", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Scheduled, accessed, and missed box events will appear here.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                groupedRecords.forEach { (date, records) ->
                    item {
                        // Date header
                        Text(
                            text = formatDate(date),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    // Summary row
                    item {
                        HistorySummaryRow(records)
                        Spacer(Modifier.height(4.dp))
                    }
                    items(records.sortedBy { it.scheduledTime }) { record ->
                        HistoryRecordRow(record)
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
        }
    }
}

@Composable
private fun HistorySummaryRow(records: List<MedicationRecord>) {
    val confirmed = records.count { it.status == MedicationStatus.CONFIRMED }
    val missed    = records.count { it.status == MedicationStatus.MISSED }
    val pending   = records.count { it.status == MedicationStatus.PENDING || it.status == MedicationStatus.DUE }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        SummaryChip("$confirmed", "Accessed", Icons.Default.CheckCircle, Modifier.weight(1f))
        SummaryChip("$missed", "Missed", Icons.Default.Cancel, Modifier.weight(1f))
        SummaryChip("$pending", "Pending", Icons.Default.Schedule, Modifier.weight(1f))
    }
}

@Composable
private fun SummaryChip(count: String, label: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(count, style = MaterialTheme.typography.titleMedium)
            Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}

@Composable
private fun HistoryRecordRow(record: MedicationRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(record.medicineName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "${record.scheduledTime}" +
                           (record.confirmationTime?.let { "  •  Accessed $it" } ?: ""),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(record.dosage, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            MedicationStatusBadge(record.status)
        }
    }
}

private fun formatDate(dateStr: String): String {
    return try {
        val parts = dateStr.split("-")
        if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}" else dateStr
    } catch (_: Exception) { dateStr }
}
