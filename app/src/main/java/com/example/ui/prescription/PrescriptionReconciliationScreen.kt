package com.example.ui.prescription

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChangeReviewStatus
import com.example.data.model.ConfidenceLevel
import com.example.data.model.DetailedChangeType
import com.example.data.model.DoseUnit
import com.example.data.model.FrequencyType
import com.example.data.model.MedicationChangeType
import com.example.data.model.MedicineForm
import com.example.data.reconciliation.DiffField
import com.example.data.reconciliation.ReconciliationItem
import com.example.data.reconciliation.ReconciliationResult
import com.example.ui.MainViewModel
import com.example.ui.components.ConfidenceBadge
import com.example.ui.components.SectionHeader
import com.example.ui.theme.StatusAttention
import com.example.ui.theme.StatusMissed
import com.example.ui.theme.StatusOnTrack
import com.example.ui.theme.StatusPending
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrescriptionReconciliationScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onReconciliationConfirmed: (Long) -> Unit
) {
    val reconciliationResult by viewModel.currentReconciliationResult.collectAsState()
    var itemToEdit by remember { mutableStateOf<ReconciliationItem?>(null) }
    var selectedFilter by remember { mutableStateOf("ALL") }

    val result = reconciliationResult

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Medication Reconciliation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Compare new Rx with current plan", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("reconciliation_back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            if (result != null) {
                Surface(
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        val pendingCount = result.pendingReviewCount
                        if (pendingCount > 0) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = StatusAttention, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "$pendingCount item(s) need review before applying changes",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StatusAttention,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onNavigateBack,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("cancel_reconciliation_button")
                            ) {
                                Text("Discard")
                            }

                            Button(
                                onClick = {
                                    viewModel.confirmReconciliation(
                                        reviewedBy = "Patient",
                                        onSuccess = onReconciliationConfirmed
                                    )
                                },
                                modifier = Modifier
                                    .weight(2f)
                                    .height(48.dp)
                                    .testTag("confirm_reconciliation_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Confirm & Update Plan")
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        if (result == null || result.items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Medication, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No reconciliation data available", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onNavigateBack) {
                        Text("Return to Medications")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    PrescriptionHeaderCard(result)
                }

                // Summary Statistics Filter Row
                item {
                    SummaryStatsRow(
                        result = result,
                        selectedFilter = selectedFilter,
                        onFilterSelected = { selectedFilter = it }
                    )
                }

                // Filtered items list
                val filteredItems = when (selectedFilter) {
                    "CHANGED" -> result.items.filter { it.category == MedicationChangeType.CHANGED }
                    "NEW" -> result.items.filter { it.category == MedicationChangeType.NEW }
                    "NOT_FOUND" -> result.items.filter { it.category == MedicationChangeType.NOT_FOUND }
                    "UNCHANGED" -> result.items.filter { it.category == MedicationChangeType.UNCHANGED }
                    "UNCERTAIN" -> result.items.filter { it.category == MedicationChangeType.UNCERTAIN || it.category == MedicationChangeType.POSSIBLE_DUPLICATE }
                    else -> result.items
                }

                items(filteredItems, key = { it.id }) { item ->
                    ReconciliationItemCard(
                        item = item,
                        onDecisionChanged = { decision, markDiscontinued ->
                            viewModel.updateReconciliationDecision(item.id, decision, markDiscontinued)
                        },
                        onEditClicked = {
                            itemToEdit = item
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    // Edit Candidate Dialog
    itemToEdit?.let { item ->
        EditReconciliationItemDialog(
            item = item,
            onDismiss = { itemToEdit = null },
            onSave = { name, strength, dose, freq, instructions ->
                viewModel.updateReconciliationItemCustomEdit(
                    itemId = item.id,
                    customName = name,
                    customStrength = strength,
                    customDoseAmount = dose,
                    customFrequency = freq,
                    customInstructions = instructions
                )
                itemToEdit = null
            }
        )
    }
}

@Composable
fun PrescriptionHeaderCard(result: ReconciliationResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.LocalHospital, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (result.doctorName.isNotBlank()) "Dr. ${result.doctorName}" else "Prescription Update",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (result.clinicName.isNotBlank()) {
                            Text(
                                text = result.clinicName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(result.prescriptionDate))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(dateStr, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryStatsRow(
    result: ReconciliationResult,
    selectedFilter: String,
    onFilterSelected: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Reconciliation Findings", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedFilter == "ALL",
                onClick = { onFilterSelected("ALL") },
                label = { Text("All (${result.items.size})", fontSize = 11.sp) }
            )
            if (result.changedItems.isNotEmpty()) {
                FilterChip(
                    selected = selectedFilter == "CHANGED",
                    onClick = { onFilterSelected("CHANGED") },
                    label = { Text("Changed (${result.changedItems.size})", fontSize = 11.sp, color = StatusMissed) }
                )
            }
            if (result.newItems.isNotEmpty()) {
                FilterChip(
                    selected = selectedFilter == "NEW",
                    onClick = { onFilterSelected("NEW") },
                    label = { Text("New (${result.newItems.size})", fontSize = 11.sp, color = StatusOnTrack) }
                )
            }
            if (result.notFoundItems.isNotEmpty()) {
                FilterChip(
                    selected = selectedFilter == "NOT_FOUND",
                    onClick = { onFilterSelected("NOT_FOUND") },
                    label = { Text("Not in Rx (${result.notFoundItems.size})", fontSize = 11.sp, color = StatusAttention) }
                )
            }
        }
    }
}

@Composable
fun ReconciliationItemCard(
    item: ReconciliationItem,
    onDecisionChanged: (ChangeReviewStatus, Boolean) -> Unit,
    onEditClicked: () -> Unit
) {
    when (item.category) {
        MedicationChangeType.CHANGED -> ChangedMedicationCard(item, onDecisionChanged, onEditClicked)
        MedicationChangeType.NEW -> NewMedicationCard(item, onDecisionChanged, onEditClicked)
        MedicationChangeType.NOT_FOUND -> NotFoundInPrescriptionCard(item, onDecisionChanged)
        MedicationChangeType.UNCHANGED -> UnchangedMedicationCard(item)
        MedicationChangeType.UNCERTAIN, MedicationChangeType.POSSIBLE_DUPLICATE -> UncertainMedicationCard(item, onDecisionChanged, onEditClicked)
    }
}

@Composable
fun ChangedMedicationCard(
    item: ReconciliationItem,
    onDecisionChanged: (ChangeReviewStatus, Boolean) -> Unit,
    onEditClicked: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (item.reviewDecision == ChangeReviewStatus.ACCEPTED) MaterialTheme.colorScheme.primary else Color.LightGray
            )
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFFFF3E0),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(item.displayMedicineName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Existing plan modified", style = MaterialTheme.typography.labelSmall, color = Color(0xFFE65100))
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when (item.reviewDecision) {
                        ChangeReviewStatus.ACCEPTED -> StatusOnTrack.copy(alpha = 0.15f)
                        ChangeReviewStatus.KEPT -> StatusAttention.copy(alpha = 0.15f)
                        ChangeReviewStatus.EDITED -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else -> Color.LightGray.copy(alpha = 0.2f)
                    }
                ) {
                    Text(
                        text = item.reviewDecision.displayName,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (item.reviewDecision) {
                            ChangeReviewStatus.ACCEPTED -> StatusOnTrack
                            ChangeReviewStatus.KEPT -> StatusAttention
                            ChangeReviewStatus.EDITED -> MaterialTheme.colorScheme.primary
                            else -> Color.DarkGray
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Side-by-side comparison table
            ComparisonDiffTable(item)

            Spacer(modifier = Modifier.height(12.dp))

            // Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = { onDecisionChanged(ChangeReviewStatus.ACCEPTED, false) },
                    modifier = Modifier.weight(1.1f),
                    colors = if (item.reviewDecision == ChangeReviewStatus.ACCEPTED) {
                        ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    } else ButtonDefaults.outlinedButtonColors()
                ) {
                    Text("Accept Change", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = { onDecisionChanged(ChangeReviewStatus.KEPT, false) },
                    modifier = Modifier.weight(0.9f),
                    colors = if (item.reviewDecision == ChangeReviewStatus.KEPT) {
                        ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    } else ButtonDefaults.outlinedButtonColors()
                ) {
                    Text("Keep Old", fontSize = 11.sp)
                }

                IconButton(
                    onClick = onEditClicked,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit details", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun ComparisonDiffTable(item: ReconciliationItem) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Strength Diff
            if (item.strengthDiff.oldValue != null || item.strengthDiff.newValue != null) {
                DiffRow(
                    label = "Strength",
                    oldVal = item.strengthDiff.oldValue ?: "-",
                    newVal = item.displayStrength,
                    isChanged = item.strengthDiff.isChanged
                )
            }

            // Frequency Diff
            if (item.frequencyDiff.oldValue != null || item.frequencyDiff.newValue != null) {
                DiffRow(
                    label = "Frequency",
                    oldVal = item.frequencyDiff.oldValue ?: "-",
                    newVal = item.displayFrequency.displayName,
                    isChanged = item.frequencyDiff.isChanged
                )
            }

            // Instructions Diff
            if (item.instructionsDiff.oldValue != null || item.instructionsDiff.newValue != null) {
                DiffRow(
                    label = "Timing",
                    oldVal = item.instructionsDiff.oldValue ?: "-",
                    newVal = item.displayInstructions,
                    isChanged = item.instructionsDiff.isChanged
                )
            }
        }
    }
}

@Composable
fun DiffRow(label: String, oldVal: String, newVal: String, isChanged: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.8f))
        
        if (isChanged) {
            Row(
                modifier = Modifier.weight(2f),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = oldVal,
                    style = MaterialTheme.typography.bodySmall,
                    textDecoration = TextDecoration.LineThrough,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = newVal,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            Text(
                text = newVal,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(2f),
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )
        }
    }
}

@Composable
fun NewMedicationCard(
    item: ReconciliationItem,
    onDecisionChanged: (ChangeReviewStatus, Boolean) -> Unit,
    onEditClicked: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(StatusOnTrack.copy(alpha = 0.6f))
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = StatusOnTrack.copy(alpha = 0.15f),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = StatusOnTrack, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(item.displayMedicineName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("New Prescription", style = MaterialTheme.typography.labelSmall, color = StatusOnTrack)
                    }
                }

                ConfidenceBadge(level = item.confidence, label = "Confidence")
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Dosage & Form", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${item.displayStrength} • ${item.displayForm.displayName}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    }
                    Column {
                        Text("Regimen", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(item.displayFrequency.displayName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    }
                    Column {
                        Text("Instructions", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(item.displayInstructions, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onDecisionChanged(ChangeReviewStatus.ACCEPTED, false) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (item.reviewDecision == ChangeReviewStatus.ACCEPTED) StatusOnTrack else MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add to Plan", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = { onDecisionChanged(ChangeReviewStatus.REJECTED, false) },
                    modifier = Modifier.weight(0.8f)
                ) {
                    Text("Skip", fontSize = 12.sp)
                }

                IconButton(
                    onClick = onEditClicked,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit details", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun NotFoundInPrescriptionCard(
    item: ReconciliationItem,
    onDecisionChanged: (ChangeReviewStatus, Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFFDE7)
        ),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(StatusAttention.copy(alpha = 0.5f))
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = StatusAttention.copy(alpha = 0.2f),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(item.displayMedicineName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Not listed on new prescription", style = MaterialTheme.typography.labelSmall, color = Color(0xFFE65100))
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (item.markDiscontinued) StatusMissed.copy(alpha = 0.15f) else StatusOnTrack.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (item.markDiscontinued) "Marked Discontinued" else "Kept Active (Default)",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (item.markDiscontinued) StatusMissed else StatusOnTrack
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Clinical rule callout
            Surface(
                color = Color(0xFFFFF8E1),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFF57F17), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Clinical Rule: Absence is not discontinuation. Keep active unless specifically told by your doctor.",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = Color(0xFF424242)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onDecisionChanged(ChangeReviewStatus.KEPT, false) },
                    modifier = Modifier.weight(1.2f),
                    colors = if (!item.markDiscontinued) {
                        ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    } else ButtonDefaults.outlinedButtonColors()
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Keep Active", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = { onDecisionChanged(ChangeReviewStatus.REJECTED, true) },
                    modifier = Modifier.weight(1f),
                    colors = if (item.markDiscontinued) {
                        ButtonDefaults.outlinedButtonColors(containerColor = StatusMissed.copy(alpha = 0.15f))
                    } else ButtonDefaults.outlinedButtonColors()
                ) {
                    Text("Discontinue", fontSize = 11.sp, color = StatusMissed)
                }
            }
        }
    }
}

@Composable
fun UnchangedMedicationCard(item: ReconciliationItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Check, contentDescription = null, tint = StatusOnTrack, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(item.displayMedicineName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("${item.displayStrength} • ${item.displayFrequency.displayName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = StatusOnTrack.copy(alpha = 0.15f)
            ) {
                Text(
                    "Unchanged",
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = StatusOnTrack,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun UncertainMedicationCard(
    item: ReconciliationItem,
    onDecisionChanged: (ChangeReviewStatus, Boolean) -> Unit,
    onEditClicked: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF3E0)
        ),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(StatusAttention)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = StatusAttention)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(item.displayMedicineName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Uncertain Match (Needs Confirmation)", style = MaterialTheme.typography.labelSmall, color = StatusAttention)
                    }
                }

                ConfidenceBadge(level = item.confidence, label = "Match")
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                "Detected candidate matches '${item.existingMedication?.name ?: ""}' with low confidence. Verify whether this is a change or a separate medicine.",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onDecisionChanged(ChangeReviewStatus.ACCEPTED, false) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Confirm Match", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = onEditClicked,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Edit / Clarify", fontSize = 11.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditReconciliationItemDialog(
    item: ReconciliationItem,
    onDismiss: () -> Unit,
    onSave: (String, String, Double, FrequencyType, String) -> Unit
) {
    var name by remember { mutableStateOf(item.displayMedicineName) }
    var strength by remember { mutableStateOf(item.displayStrength) }
    var doseStr by remember { mutableStateOf(item.displayDoseAmount.toString()) }
    var frequency by remember { mutableStateOf(item.displayFrequency) }
    var instructions by remember { mutableStateOf(item.displayInstructions) }
    var freqMenuExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Medicine Details", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Medicine Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = strength,
                        onValueChange = { strength = it },
                        label = { Text("Strength") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = doseStr,
                        onValueChange = { doseStr = it },
                        label = { Text("Dose") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                ExposedDropdownMenuBox(
                    expanded = freqMenuExpanded,
                    onExpandedChange = { freqMenuExpanded = !freqMenuExpanded }
                ) {
                    OutlinedTextField(
                        value = frequency.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Frequency") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = freqMenuExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = freqMenuExpanded,
                        onDismissRequest = { freqMenuExpanded = false }
                    ) {
                        FrequencyType.values().forEach { freq ->
                            DropdownMenuItem(
                                text = { Text(freq.displayName) },
                                onClick = {
                                    frequency = freq
                                    freqMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = instructions,
                    onValueChange = { instructions = it },
                    label = { Text("Instructions") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val dose = doseStr.toDoubleOrNull() ?: 1.0
                    onSave(name, strength, dose, frequency, instructions)
                }
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
