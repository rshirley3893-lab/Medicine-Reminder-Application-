package com.example.ui.ocr

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.OcrCandidateEntity
import com.example.data.model.ConfidenceLevel
import com.example.data.model.DoseUnit
import com.example.data.model.FrequencyType
import com.example.ocr.ParsedPrescriptionResult
import com.example.ui.components.ConfidenceBadge
import com.example.ui.components.SectionHeader
import com.example.ui.theme.StatusAttention
import com.example.ui.theme.StatusMissed
import com.example.ui.theme.StatusOnTrack
import com.example.ui.theme.StatusPending

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrReviewScreen(
    ocrResult: ParsedPrescriptionResult,
    onReconcile: (List<OcrCandidateEntity>, String, String, Long) -> Unit = { _, _, _, _ -> },
    onConfirmAndSave: (List<OcrCandidateEntity>) -> Unit,
    onNavigateBack: () -> Unit
) {
    var isRawTextExpanded by remember { mutableStateOf(false) }
    var doctorName by remember { mutableStateOf<String>(ocrResult.doctorName) }
    var clinicName by remember { mutableStateOf<String>(ocrResult.clinicName) }
    var prescriptionDate by remember { mutableStateOf<Long>(ocrResult.prescriptionDate) }

    // Mutable list of candidates for user verification and editing
    val candidatesList = remember {
        mutableStateListOf<OcrCandidateEntity>().apply {
            addAll(ocrResult.candidates)
        }
    }

    val selectedStates = remember {
        mutableStateListOf<Boolean>().apply {
            repeat(ocrResult.candidates.size) { add(true) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Prescription Review") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))

                // Safety Warning Banner
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = StatusPending,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Human Verification Required",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF57F17)
                            )
                            Text(
                                text = "OCR assists data entry. Please review and confirm names, strengths, and times against your original prescription before adding.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF5D4037)
                            )
                        }
                    }
                }
            }

            // Raw OCR Extracted Text Collapsible
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isRawTextExpanded = !isRawTextExpanded }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Extracted Raw Text (${ocrResult.rawText.lines().size} lines)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                imageVector = if (isRawTextExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null
                            )
                        }

                        AnimatedVisibility(visible = isRawTextExpanded) {
                            Column(modifier = Modifier.padding(top = 10.dp)) {
                                Text(
                                    text = ocrResult.rawText.ifBlank { "No text recognized" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item {
                SectionHeader(title = "Detected Medicines (${candidatesList.size})")
            }

            // Candidate Medicine Cards
            itemsIndexed(candidatesList) { index, candidate ->
                val isSelected = selectedStates.getOrElse(index) { true }
                OcrCandidateEditableCard(
                    candidate = candidate,
                    isSelected = isSelected,
                    onToggleSelect = { selectedStates[index] = !isSelected },
                    onUpdate = { updated -> candidatesList[index] = updated }
                )
            }

            // Confirm & Reconcile Buttons
            item {
                val selectedCandidates = candidatesList.filterIndexed { index, _ -> selectedStates.getOrElse(index) { true } }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            onReconcile(selectedCandidates, doctorName, clinicName, prescriptionDate)
                        },
                        enabled = selectedCandidates.isNotEmpty(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("reconcile_ocr_button")
                    ) {
                        Icon(Icons.Default.Medication, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reconcile with Current Plan", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            onConfirmAndSave(selectedCandidates)
                        },
                        enabled = selectedCandidates.isNotEmpty(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("quick_add_ocr_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Quick Add as New Medicines (${selectedCandidates.size})")
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrCandidateEditableCard(
    candidate: OcrCandidateEntity,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onUpdate: (OcrCandidateEntity) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() }
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = candidate.medicineName.ifBlank { "Unidentified Drug" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Confidence: ${candidate.confidenceName.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                ConfidenceBadge(level = candidate.confidenceName, label = "Name")
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Editable Name & Strength
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = candidate.medicineName,
                    onValueChange = { onUpdate(candidate.copy(medicineName = it)) },
                    label = { Text("Medicine Name") },
                    singleLine = true,
                    modifier = Modifier.weight(1.3f)
                )

                OutlinedTextField(
                    value = candidate.strength,
                    onValueChange = { onUpdate(candidate.copy(strength = it)) },
                    label = { Text("Strength") },
                    singleLine = true,
                    modifier = Modifier.weight(0.9f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Editable Instructions & Frequency
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = candidate.instructions,
                    onValueChange = { onUpdate(candidate.copy(instructions = it)) },
                    label = { Text("Instructions") },
                    singleLine = true,
                    modifier = Modifier.weight(1.2f)
                )

                OutlinedTextField(
                    value = candidate.duration,
                    onValueChange = { onUpdate(candidate.copy(duration = it)) },
                    label = { Text("Duration") },
                    singleLine = true,
                    modifier = Modifier.weight(0.8f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Confidence tags breakdown
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ConfidenceBadge(level = candidate.confidenceStrength, label = "Strength")
                ConfidenceBadge(level = candidate.confidenceFreq, label = "Freq (${candidate.frequency.displayName})")
            }
        }
    }
}
