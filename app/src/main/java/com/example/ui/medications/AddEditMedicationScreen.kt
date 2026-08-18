package com.example.ui.medications

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.MedicationEntity
import com.example.data.local.entity.MedicationScheduleEntity
import com.example.data.model.DoseUnit
import com.example.data.model.FrequencyType
import com.example.data.model.MedicineForm

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditMedicationScreen(
    existingMedication: MedicationEntity? = null,
    existingSchedules: List<MedicationScheduleEntity> = emptyList(),
    onSave: (MedicationEntity, List<MedicationScheduleEntity>) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf(existingMedication?.name ?: "") }
    var genericName by remember { mutableStateOf(existingMedication?.genericName ?: "") }
    var strength by remember { mutableStateOf(existingMedication?.strength ?: "500 mg") }
    var doseAmountStr by remember { mutableStateOf((existingMedication?.doseAmount ?: 1.0).toString()) }
    var doseUnit by remember { mutableStateOf(existingMedication?.doseUnit ?: DoseUnit.TABLET) }
    var form by remember { mutableStateOf(existingMedication?.form ?: MedicineForm.TABLET) }
    var instructions by remember { mutableStateOf(existingMedication?.instructions ?: "After food") }
    var frequencyType by remember { mutableStateOf(existingSchedules.firstOrNull()?.frequencyType ?: FrequencyType.DAILY) }
    var stockQuantityStr by remember { mutableStateOf((existingMedication?.stockQuantity ?: 30.0).toInt().toString()) }
    var lowStockThresholdStr by remember { mutableStateOf((existingMedication?.lowStockThreshold ?: 5.0).toInt().toString()) }

    // Schedule Times
    val scheduleTimes = remember {
        mutableStateListOf<String>().apply {
            if (existingSchedules.isNotEmpty()) {
                addAll(existingSchedules.map { it.timeString })
            } else {
                add("08:00")
            }
        }
    }

    var selectedDays by remember {
        mutableStateOf(
            existingSchedules.firstOrNull()?.daysOfWeek?.split(",")?.toSet() ?: setOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
        )
    }

    var isFormExpanded by remember { mutableStateOf(false) }
    var isUnitExpanded by remember { mutableStateOf(false) }
    var isFreqExpanded by remember { mutableStateOf(false) }
    var newTimeInput by remember { mutableStateOf("12:00") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existingMedication != null) "Edit Medicine" else "Add New Medicine") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Medicine Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Medicine Name *") },
                placeholder = { Text("e.g. Paracetamol, Metformin") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("medicine_name_input")
            )

            // Generic / Brand Name & Strength
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = genericName,
                    onValueChange = { genericName = it },
                    label = { Text("Generic Name (Optional)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = strength,
                    onValueChange = { strength = it },
                    label = { Text("Strength") },
                    placeholder = { Text("500 mg") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("medicine_strength_input")
                )
            }

            // Medicine Form & Unit Dropdowns
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = isFormExpanded,
                    onExpandedChange = { isFormExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = form.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Form") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isFormExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = isFormExpanded,
                        onDismissRequest = { isFormExpanded = false }
                    ) {
                        MedicineForm.values().forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item.displayName) },
                                onClick = {
                                    form = item
                                    isFormExpanded = false
                                }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = isUnitExpanded,
                    onExpandedChange = { isUnitExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = doseUnit.symbol,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Unit") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isUnitExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = isUnitExpanded,
                        onDismissRequest = { isUnitExpanded = false }
                    ) {
                        DoseUnit.values().forEach { item ->
                            DropdownMenuItem(
                                text = { Text("${item.displayName} (${item.symbol})") },
                                onClick = {
                                    doseUnit = item
                                    isUnitExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Dose Amount & Instructions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = doseAmountStr,
                    onValueChange = { doseAmountStr = it },
                    label = { Text("Dose per intake") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = instructions,
                    onValueChange = { instructions = it },
                    label = { Text("Instructions") },
                    placeholder = { Text("After food, with water") },
                    singleLine = true,
                    modifier = Modifier.weight(1.5f)
                )
            }

            // Frequency Type Dropdown
            ExposedDropdownMenuBox(
                expanded = isFreqExpanded,
                onExpandedChange = { isFreqExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = frequencyType.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Frequency") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isFreqExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = isFreqExpanded,
                    onDismissRequest = { isFreqExpanded = false }
                ) {
                    FrequencyType.values().forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item.displayName) },
                            onClick = {
                                frequencyType = item
                                scheduleTimes.clear()
                                when (item) {
                                    FrequencyType.DAILY -> scheduleTimes.addAll(listOf("08:00"))
                                    FrequencyType.TWICE_DAILY -> scheduleTimes.addAll(listOf("08:00", "20:00"))
                                    FrequencyType.THREE_TIMES_DAILY -> scheduleTimes.addAll(listOf("08:00", "14:00", "20:00"))
                                    FrequencyType.FOUR_TIMES_DAILY -> scheduleTimes.addAll(listOf("08:00", "12:00", "16:00", "20:00"))
                                    else -> scheduleTimes.addAll(listOf("09:00"))
                                }
                                isFreqExpanded = false
                            }
                        )
                    }
                }
            }

            // Schedule Times Management
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Scheduled Dose Times",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        scheduleTimes.forEachIndexed { index, time ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(time, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    if (scheduleTimes.size > 1) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove Time",
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable { scheduleTimes.removeAt(index) },
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Add Custom Time row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newTimeInput,
                            onValueChange = { newTimeInput = it },
                            label = { Text("Add Time (HH:mm)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                if (newTimeInput.isNotBlank() && !scheduleTimes.contains(newTimeInput)) {
                                    scheduleTimes.add(newTimeInput)
                                }
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Text("Add")
                        }
                    }
                }
            }

            // Days of the week selector
            Column {
                Text(
                    text = "Active Days",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val days = listOf("MON" to "M", "TUE" to "T", "WED" to "W", "THU" to "T", "FRI" to "F", "SAT" to "S", "SUN" to "S")
                    days.forEach { (key, label) ->
                        val isSelected = selectedDays.contains(key)
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    selectedDays = if (isSelected) {
                                        if (selectedDays.size > 1) selectedDays - key else selectedDays
                                    } else {
                                        selectedDays + key
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Inventory & Stock Tracking
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = stockQuantityStr,
                    onValueChange = { stockQuantityStr = it },
                    label = { Text("Initial Stock") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = lowStockThresholdStr,
                    onValueChange = { lowStockThresholdStr = it },
                    label = { Text("Low Stock Threshold") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Save & Cancel Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            val parsedDoseAmount = doseAmountStr.toDoubleOrNull() ?: 1.0
                            val parsedStock = stockQuantityStr.toDoubleOrNull() ?: 30.0
                            val parsedThreshold = lowStockThresholdStr.toDoubleOrNull() ?: 5.0

                            val med = (existingMedication ?: MedicationEntity(name = name)).copy(
                                name = name.trim(),
                                genericName = genericName.trim(),
                                strength = strength.trim(),
                                doseAmount = parsedDoseAmount,
                                doseUnit = doseUnit,
                                form = form,
                                instructions = instructions.trim(),
                                stockQuantity = parsedStock,
                                lowStockThreshold = parsedThreshold,
                                updatedAt = System.currentTimeMillis()
                            )

                            val schedules = scheduleTimes.map { timeStr ->
                                MedicationScheduleEntity(
                                    medicationId = med.id,
                                    frequencyType = frequencyType,
                                    timeString = timeStr,
                                    daysOfWeek = selectedDays.joinToString(","),
                                    doseAmount = parsedDoseAmount
                                )
                            }
                            onSave(med, schedules)
                        }
                    },
                    enabled = name.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .weight(1.5f)
                        .height(50.dp)
                        .testTag("save_medication_button")
                ) {
                    Text("Save Medicine", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
