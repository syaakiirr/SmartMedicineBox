package com.example.smartmedicinebox.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.smartmedicinebox.data.model.Medicine
import com.example.smartmedicinebox.data.model.MedicationRecord
import com.example.smartmedicinebox.data.model.MedicationStatus

@Composable
fun MedicineCard(
    medicine: Medicine,
    onEdit: (Medicine) -> Unit,
    onDelete: (Medicine) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "BOX",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = medicine.compartment.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(medicine.medicineName, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${medicine.dosage} at ${medicine.scheduledTime}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { onEdit(medicine) }) {
                Icon(Icons.Default.Edit, contentDescription = "Edit ${medicine.medicineName}")
            }
            IconButton(onClick = { onDelete(medicine) }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete ${medicine.medicineName}",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun RecordStatusCard(
    record: MedicationRecord,
    compartment: Int? = null,
    onConfirm: ((MedicationRecord) -> Unit)? = null,
    onMissed: ((MedicationRecord) -> Unit)? = null,
    showActions: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(record.medicineName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = buildString {
                            append("${record.dosage} at ${record.scheduledTime}")
                            compartment?.let { append("  •  Box $it") }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    record.confirmationTime?.let {
                        Text(
                            text = "Box accessed at $it",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
                MedicationStatusBadge(record.status)
            }

            if (showActions && (record.status == MedicationStatus.DUE || record.status == MedicationStatus.PENDING)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onConfirm?.invoke(record) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary
                        )
                    ) {
                        Text("Record access", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = { onMissed?.invoke(record) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Mark missed", maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
fun MedicationStatusBadge(status: MedicationStatus) {
    val visual = when (status) {
        MedicationStatus.PENDING -> StatusVisual(
            "Pending", Icons.Default.Schedule,
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )
        MedicationStatus.DUE -> StatusVisual(
            "Due now", Icons.Default.Warning,
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
        MedicationStatus.CONFIRMED -> StatusVisual(
            "Box accessed", Icons.Default.CheckCircle,
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
        MedicationStatus.MISSED -> StatusVisual(
            "Missed", Icons.Default.Cancel,
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
    }

    Surface(shape = MaterialTheme.shapes.small, color = visual.containerColor) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                visual.icon,
                contentDescription = null,
                modifier = Modifier.size(15.dp),
                tint = visual.contentColor
            )
            Text(
                visual.label,
                style = MaterialTheme.typography.labelMedium,
                color = visual.contentColor
            )
        }
    }
}

private data class StatusVisual(
    val label: String,
    val icon: ImageVector,
    val containerColor: Color,
    val contentColor: Color
)
