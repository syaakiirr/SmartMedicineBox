package com.example.smartmedicinebox.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartmedicinebox.data.model.Medicine
import com.example.smartmedicinebox.ui.viewmodel.MedicineViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditMedicineScreen(
    viewModel: MedicineViewModel,
    existingMedicine: Medicine? = null,
    onNavigateBack: () -> Unit
) {
    val isEditMode = existingMedicine != null

    var medicineName by remember { mutableStateOf(existingMedicine?.medicineName ?: "") }
    var dosage by remember { mutableStateOf(existingMedicine?.dosage ?: "") }
    var timeHour by remember { mutableStateOf(existingMedicine?.scheduledTime?.split(":")?.getOrElse(0) { "08" } ?: "08") }
    var timeMinute by remember { mutableStateOf(existingMedicine?.scheduledTime?.split(":")?.getOrElse(1) { "00" } ?: "00") }

    var nameError by remember { mutableStateOf(false) }
    var dosageError by remember { mutableStateOf(false) }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Medicine") },
            text = { Text("Are you sure you want to delete \"${existingMedicine?.medicineName}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    existingMedicine?.let { viewModel.deleteMedicine(it) }
                    showDeleteDialog = false
                    onNavigateBack()
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = timeHour.toIntOrNull() ?: 8,
            initialMinute = timeMinute.toIntOrNull() ?: 0,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Enter Medication Time") },
            text = {
                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()) {
                    TimeInput(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    timeHour = timePickerState.hour.toString().padStart(2, '0')
                    timeMinute = timePickerState.minute.toString().padStart(2, '0')
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditMode) "Edit Medicine" else "Add Medicine",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Medicine Name
            OutlinedTextField(
                value = medicineName,
                onValueChange = { medicineName = it; nameError = false },
                label = { Text("Medicine Name *") },
                placeholder = { Text("e.g. Paracetamol") },
                isError = nameError,
                supportingText = if (nameError) {{ Text("Medicine name is required") }} else null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Dosage
            OutlinedTextField(
                value = dosage,
                onValueChange = { dosage = it; dosageError = false },
                label = { Text("Dosage *") },
                placeholder = { Text("e.g. 1 Tablet, 5ml") },
                isError = dosageError,
                supportingText = if (dosageError) {{ Text("Dosage is required") }} else null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Medication Time
            Text("Medication Time", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            OutlinedButton(
                onClick = { showTimePicker = true },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Default.Schedule, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${timeHour.padStart(2, '0')}:${timeMinute.padStart(2, '0')}",
                    fontSize = 18.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            // Save Button
            Button(
                onClick = {
                    var valid = true
                    if (medicineName.isBlank()) { nameError = true; valid = false }
                    if (dosage.isBlank()) { dosageError = true; valid = false }
                    if (!valid) return@Button

                    val scheduledTime = "${timeHour.padStart(2, '0')}:${timeMinute.padStart(2, '0')}"
                    if (existingMedicine != null) {
                        viewModel.updateMedicine(
                            existingMedicine.copy(
                                medicineName = medicineName.trim(),
                                dosage = dosage.trim(),
                                scheduledTime = scheduledTime,
                                compartment = 1
                            )
                        )
                    } else {
                        viewModel.insertMedicine(
                            Medicine(
                                medicineName = medicineName.trim(),
                                dosage = dosage.trim(),
                                scheduledTime = scheduledTime
                            )
                        )
                    }
                    onNavigateBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    if (isEditMode) "Save Changes" else "Add Medicine",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Delete Button (only in edit mode)
            if (isEditMode) {
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete Medicine", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
