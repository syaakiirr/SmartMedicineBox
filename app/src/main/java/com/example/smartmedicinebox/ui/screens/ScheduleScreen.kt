package com.example.smartmedicinebox.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartmedicinebox.data.model.Medicine
import com.example.smartmedicinebox.ui.components.MedicineCard
import com.example.smartmedicinebox.ui.viewmodel.MedicineViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    viewModel: MedicineViewModel,
    onAddMedicine: () -> Unit,
    onEditMedicine: (Medicine) -> Unit
) {
    val medicines by viewModel.allMedicines.collectAsState()
    var medicineToDelete by remember { mutableStateOf<Medicine?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    if (medicineToDelete != null) {
        AlertDialog(
            onDismissRequest = { medicineToDelete = null },
            title = { Text("Delete Medicine") },
            text = { Text("Delete \"${medicineToDelete?.medicineName}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    medicineToDelete?.let { viewModel.deleteMedicine(it) }
                    medicineToDelete = null
                    scope.launch { snackbarHostState.showSnackbar("Medicine deleted") }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { medicineToDelete = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Medication Schedule", fontWeight = FontWeight.Bold) }
            )
        },
        floatingActionButton = {
            if (medicines.isNotEmpty()) {
                FloatingActionButton(
                    onClick = onAddMedicine,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Medicine")
                }
            }
        }
    ) { paddingValues ->
        if (medicines.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Medication,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "No medicines added yet.",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Use Add medicine to create your first schedule.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(onClick = onAddMedicine) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add medicine")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                item {
                    Text(
                        if (medicines.size == 1) "1 medicine scheduled" else "${medicines.size} medicines scheduled",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                }
                items(medicines.sortedBy { it.scheduledTime }) { medicine ->
                    MedicineCard(
                        medicine = medicine,
                        onEdit = { onEditMedicine(it) },
                        onDelete = { medicineToDelete = it }
                    )
                }
            }
        }
    }
}
