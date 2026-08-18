package com.example.ui.onboarding.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ConfidenceLevel
import com.example.data.model.DoseUnit
import com.example.data.model.FrequencyType
import com.example.data.model.MedicineForm
import com.example.ui.onboarding.OnboardingMedicationDraft
import com.example.ui.onboarding.OnboardingState
import com.example.ui.onboarding.OnboardingStep
import com.example.ui.onboarding.PrescriptionSourceType
import com.example.ui.onboarding.components.OnboardingProgressHeader
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualMedicineEntryScreen(
    state: OnboardingState,
    onSaveDraft: (OnboardingMedicationDraft) -> Unit,
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val existingDraft = state.draftMedicines.firstOrNull { it.id == state.editingMedicationId }

    var name by remember(existingDraft) { mutableStateOf(existingDraft?.name ?: "") }
    var strength by remember(existingDraft) { mutableStateOf(existingDraft?.strength ?: "") }
    var doseAmount by remember(existingDraft) { mutableStateOf(existingDraft?.doseAmount?.toString() ?: "1.0") }
    var selectedUnit by remember(existingDraft) { mutableStateOf(existingDraft?.doseUnit ?: DoseUnit.TABLET) }
    var selectedForm by remember(existingDraft) { mutableStateOf(existingDraft?.form ?: MedicineForm.TABLET) }
    var selectedFreq by remember(existingDraft) { mutableStateOf(existingDraft?.frequency ?: FrequencyType.DAILY) }
    var timeString by remember(existingDraft) { mutableStateOf(existingDraft?.timeString ?: "08:00") }
    var route by remember(existingDraft) { mutableStateOf(existingDraft?.route ?: "Oral") }
    var instructions by remember(existingDraft) { mutableStateOf(existingDraft?.instructions ?: "After food") }
    var duration by remember(existingDraft) { mutableStateOf(existingDraft?.durationDays ?: "7 days") }
    var validationError by remember { mutableStateOf<String?>(null) }

    var unitMenuExpanded by remember { mutableStateOf(false) }
    var freqMenuExpanded by remember { mutableStateOf(false) }
    var routeMenuExpanded by remember { mutableStateOf(false) }

    val instructionPresets = listOf("After food", "Before food", "With water", "At bedtime", "Empty stomach")
    val durationPresets = listOf("5 days", "7 days", "14 days", "30 days", "Ongoing")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Step 4 Header
        OnboardingProgressHeader(
            currentStep = OnboardingStep.MANUAL_ENTRY,
            onBackClicked = onBackClicked
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (existingDraft != null) "Edit Medicine" else "Enter Medicine Details",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Fill in the dosage, frequency, and instructions.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Error banner
            AnimatedVisibility(visible = validationError != null) {
                validationError?.let { error ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = error, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }

            // Medicine Name
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    validationError = null
                },
                label = { Text("Medicine Name *") },
                placeholder = { Text("e.g. Paracetamol, Amoxicillin, Metformin") },
                leadingIcon = { Icon(Icons.Default.Medication, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("manual_med_name_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Strength (e.g. 500 mg)
            OutlinedTextField(
                value = strength,
                onValueChange = { strength = it },
                label = { Text("Strength (e.g. 500 mg, 10 ml, 25 mcg)") },
                placeholder = { Text("500 mg") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("manual_med_strength_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Dose Amount & Unit Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = doseAmount,
                    onValueChange = { doseAmount = it },
                    label = { Text("Dose Qty *") },
                    placeholder = { Text("1") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("manual_med_dose_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                ExposedDropdownMenuBox(
                    expanded = unitMenuExpanded,
                    onExpandedChange = { unitMenuExpanded = it },
                    modifier = Modifier.weight(1.3f)
                ) {
                    OutlinedTextField(
                        value = selectedUnit.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Unit") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitMenuExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("manual_med_unit_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = unitMenuExpanded,
                        onDismissRequest = { unitMenuExpanded = false }
                    ) {
                        DoseUnit.values().forEach { unit ->
                            DropdownMenuItem(
                                text = { Text(unit.displayName) },
                                onClick = {
                                    selectedUnit = unit
                                    unitMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Frequency Dropdown
            ExposedDropdownMenuBox(
                expanded = freqMenuExpanded,
                onExpandedChange = { freqMenuExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedFreq.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Frequency *") },
                    leadingIcon = { Icon(Icons.Default.Repeat, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = freqMenuExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                        .testTag("manual_med_freq_input"),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = freqMenuExpanded,
                    onDismissRequest = { freqMenuExpanded = false }
                ) {
                    FrequencyType.values().forEach { freq ->
                        DropdownMenuItem(
                            text = { Text(freq.displayName) },
                            onClick = {
                                selectedFreq = freq
                                freqMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Preferred Time
            OutlinedTextField(
                value = timeString,
                onValueChange = { timeString = it },
                label = { Text("First Daily Time (HH:mm)") },
                placeholder = { Text("08:00") },
                leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("manual_med_time_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Instructions / Timing
            Text(
                text = "Instructions",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                instructionPresets.take(3).forEach { preset ->
                    FilterChip(
                        selected = instructions == preset,
                        onClick = { instructions = preset },
                        label = { Text(preset, fontSize = 12.sp) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                instructionPresets.drop(3).forEach { preset ->
                    FilterChip(
                        selected = instructions == preset,
                        onClick = { instructions = preset },
                        label = { Text(preset, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Duration
            Text(
                text = "Duration / Course Length",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                durationPresets.forEach { preset ->
                    FilterChip(
                        selected = duration == preset,
                        onClick = { duration = preset },
                        label = { Text(preset, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Save / Add to Prescription Button
            Button(
                onClick = {
                    if (name.trim().isBlank()) {
                        validationError = "Please enter a medicine name."
                        return@Button
                    }
                    val doseVal = doseAmount.toDoubleOrNull() ?: 1.0
                    val draft = OnboardingMedicationDraft(
                        id = existingDraft?.id ?: UUID.randomUUID().toString(),
                        name = name.trim(),
                        strength = strength.trim(),
                        doseAmount = doseVal,
                        doseUnit = selectedUnit,
                        form = selectedForm,
                        frequency = selectedFreq,
                        timeString = if (timeString.isBlank()) "08:00" else timeString.trim(),
                        route = route,
                        instructions = instructions,
                        durationDays = duration,
                        source = existingDraft?.source ?: PrescriptionSourceType.MANUAL,
                        ocrConfidence = existingDraft?.ocrConfidence ?: ConfidenceLevel.HIGH,
                        confirmed = true
                    )
                    onSaveDraft(draft)
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("manual_med_save_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = if (existingDraft != null) "Update Medicine" else "Add to Prescription",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
