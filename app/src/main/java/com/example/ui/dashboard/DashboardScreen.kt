package com.example.ui.dashboard

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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.dao.DoseWithMedication
import com.example.data.local.entity.MedicationEntity
import com.example.data.local.entity.UserProfileEntity
import com.example.data.model.DoseStatus
import com.example.data.model.UserRole
import com.example.ui.components.DoseStatusBadge
import com.example.ui.components.MetricSummaryCard
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
fun DashboardScreen(
    userProfile: UserProfileEntity?,
    todayDoses: List<DoseWithMedication>,
    lowStockMedications: List<MedicationEntity>,
    onTakeDose: (Long) -> Unit,
    onSnoozeDose: (Long) -> Unit,
    onSkipDose: (Long) -> Unit,
    onNavigateToAddMedication: () -> Unit,
    onNavigateToScanOcr: () -> Unit,
    onNavigateToAiAdvice: () -> Unit,
    onNavigateToStockAlerts: () -> Unit,
    onNavigateToEmergencyId: () -> Unit = {},
    onNavigateToSchedule: () -> Unit,
    onSwitchMode: () -> Unit
) {
    val role = userProfile?.role ?: UserRole.PATIENT
    val isCaregiver = role == UserRole.CAREGIVER

    val totalDoses = todayDoses.size
    val takenCount = todayDoses.count { it.doseEvent.status == DoseStatus.TAKEN }
    val missedCount = todayDoses.count { it.doseEvent.status == DoseStatus.MISSED }
    val pendingCount = todayDoses.count { it.doseEvent.status == DoseStatus.SCHEDULED || it.doseEvent.status == DoseStatus.SNOOZED }
    val adherenceScore = if (takenCount + missedCount > 0) {
        (takenCount.toDouble() / (takenCount + missedCount)) * 100.0
    } else if (totalDoses > 0) {
        100.0
    } else {
        100.0
    }

    val nextUpcomingDose = remember(todayDoses) {
        val currentTime = System.currentTimeMillis()
        todayDoses.firstOrNull {
            (it.doseEvent.status == DoseStatus.SCHEDULED || it.doseEvent.status == DoseStatus.SNOOZED)
        }
    }

    val dateFormat = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
    val todayDateStr = dateFormat.format(Date())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))

            // Header Greeting with Active Mode Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = todayDateStr,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (isCaregiver) {
                            "Managing for ${userProfile?.managedPatientName?.ifBlank { "Patient" }}"
                        } else {
                            "Hello, ${userProfile?.name?.ifBlank { "Friend" }}"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Mode switch chip
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .clickable { onSwitchMode() }
                        .testTag("mode_switch_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isCaregiver) Icons.Default.FamilyRestroom else Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isCaregiver) "Caregiver" else "Patient",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Caregiver Context Banner (if in Caregiver Mode)
        if (isCaregiver) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FamilyRestroom,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Caregiver Visibility Active",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "Monitoring routine & missed-dose escalations for ${userProfile?.managedPatientName ?: "Patient"}.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        // Low-stock alert banner (if any)
        if (lowStockMedications.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToStockAlerts() }
                        .testTag("low_stock_banner")
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Low Stock Alert (${lowStockMedications.size} item${if (lowStockMedications.size > 1) "s" else ""})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100)
                            )
                            Text(
                                text = "${lowStockMedications.joinToString(", ") { it.name }} are running low. Tap to review.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF5D4037)
                            )
                        }
                    }
                }
            }
        }

        // Quick Actions Row
        item {
            Column {
                SectionHeader(title = "Quick Actions")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionCard(
                        title = "Add Pill",
                        subtitle = "Manual entry",
                        icon = Icons.Default.Add,
                        bgColor = MaterialTheme.colorScheme.primaryContainer,
                        iconColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_action_add_pill"),
                        onClick = onNavigateToAddMedication
                    )
                    QuickActionCard(
                        title = "Scan Rx",
                        subtitle = "Camera OCR",
                        icon = Icons.Default.CameraAlt,
                        bgColor = Color(0xFFE0F7FA),
                        iconColor = Color(0xFF00838F),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_action_scan_rx"),
                        onClick = onNavigateToScanOcr
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionCard(
                        title = "AI Advice",
                        subtitle = "Routine Q&A",
                        icon = Icons.Default.AutoAwesome,
                        bgColor = Color(0xFFEDE7F6),
                        iconColor = Color(0xFF512DA8),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_action_ai_advice"),
                        onClick = onNavigateToAiAdvice
                    )
                    QuickActionCard(
                        title = "Stock Alert",
                        subtitle = "${lowStockMedications.size} Warning${if (lowStockMedications.size != 1) "s" else ""}",
                        icon = Icons.Default.Inventory2,
                        bgColor = if (lowStockMedications.isNotEmpty()) Color(0xFFFFEBEE) else Color(0xFFE8F5E9),
                        iconColor = if (lowStockMedications.isNotEmpty()) StatusMissed else StatusOnTrack,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_action_stock_alert"),
                        onClick = onNavigateToStockAlerts
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                // Emergency Medical ID Quick Access Banner
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToEmergencyId() }
                        .testTag("dashboard_emergency_id_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.error, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Emergency,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onError,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Emergency Medical ID & First Responder QR",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "Allergies, conditions, blood group & lock-screen card",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        // Next Upcoming Dose Hero Card
        if (nextUpcomingDose != null) {
            item {
                SectionHeader(title = "Next Upcoming Dose")
                NextDoseHeroCard(
                    doseWithMed = nextUpcomingDose,
                    onTake = { onTakeDose(nextUpcomingDose.doseEvent.id) },
                    onSnooze = { onSnoozeDose(nextUpcomingDose.doseEvent.id) },
                    onSkip = { onSkipDose(nextUpcomingDose.doseEvent.id) }
                )
            }
        }

        // Today's Adherence Stats
        item {
            SectionHeader(title = "Today's Adherence")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricSummaryCard(
                    title = "Adherence",
                    value = String.format(Locale.US, "%.0f%%", adherenceScore),
                    subtitle = "$takenCount of $totalDoses taken",
                    icon = Icons.Default.CheckCircle,
                    accentColor = if (adherenceScore >= 80.0) StatusOnTrack else StatusAttention,
                    modifier = Modifier.weight(1f)
                )
                MetricSummaryCard(
                    title = "Pending",
                    value = "$pendingCount",
                    subtitle = "$missedCount missed",
                    icon = Icons.Default.Schedule,
                    accentColor = if (missedCount > 0) StatusMissed else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Today's Medication Timeline Header & List
        item {
            SectionHeader(
                title = "Today's Medication Timeline",
                actionText = "View Full Schedule",
                onActionClick = onNavigateToSchedule
            )
        }

        if (todayDoses.isEmpty()) {
            item {
                EmptyTodayDosesCard(onAddMedication = onNavigateToAddMedication, onScanOcr = onNavigateToScanOcr)
            }
        } else {
            items(todayDoses, key = { it.doseEvent.id }) { doseWithMed ->
                TodayDoseTimelineCard(
                    doseWithMed = doseWithMed,
                    onTake = { onTakeDose(doseWithMed.doseEvent.id) },
                    onSnooze = { onSnoozeDose(doseWithMed.doseEvent.id) },
                    onSkip = { onSkipDose(doseWithMed.doseEvent.id) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    bgColor: Color,
    iconColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun NextDoseHeroCard(
    doseWithMed: DoseWithMedication,
    onTake: () -> Unit,
    onSnooze: () -> Unit,
    onSkip: () -> Unit
) {
    val dose = doseWithMed.doseEvent
    val med = doseWithMed.medication
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val scheduledTimeStr = timeFormat.format(Date(dose.scheduledAt))

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Medication,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = med.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        if (med.strength.isNotBlank()) {
                            Text(
                                text = "${med.strength} • ${med.doseAmount} ${med.doseUnit.symbol} (${med.form.displayName})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = scheduledTimeStr,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            if (med.instructions.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Instructions: ${med.instructions}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onTake,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("hero_take_button")
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Take Now", fontWeight = FontWeight.Bold)
                }

                FilledTonalButton(
                    onClick = onSnooze,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("hero_snooze_button")
                ) {
                    Icon(imageVector = Icons.Default.HourglassEmpty, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Snooze", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onSkip,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(0.8f)
                        .testTag("hero_skip_button")
                ) {
                    Text("Skip", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun TodayDoseTimelineCard(
    doseWithMed: DoseWithMedication,
    onTake: () -> Unit,
    onSnooze: () -> Unit,
    onSkip: () -> Unit
) {
    val dose = doseWithMed.doseEvent
    val med = doseWithMed.medication
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val timeStr = timeFormat.format(Date(dose.scheduledAt))

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF006874).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Medication,
                            contentDescription = null,
                            tint = Color(0xFF006874),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = med.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${med.strength.ifBlank { "Standard dose" }} • ${med.doseAmount} ${med.doseUnit.symbol} • $timeStr",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                DoseStatusBadge(status = dose.status)
            }

            if (med.instructions.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "• ${med.instructions}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 46.dp)
                )
            }

            // If dose is still pending or snoozed, show action buttons
            if (dose.status == DoseStatus.SCHEDULED || dose.status == DoseStatus.SNOOZED) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onSkip,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Text("Skip", fontSize = 12.sp)
                    }

                    FilledTonalButton(
                        onClick = onSnooze,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Text("Snooze (15m)", fontSize = 12.sp)
                    }

                    Button(
                        onClick = onTake,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Take", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else if (dose.status == DoseStatus.TAKEN && dose.takenAt != null) {
                val takenTimeStr = timeFormat.format(Date(dose.takenAt))
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "✓ Recorded as taken at $takenTimeStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = StatusOnTrack,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 46.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyTodayDosesCard(
    onAddMedication: () -> Unit,
    onScanOcr: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No doses scheduled for today",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Scan a doctor's prescription or add your medicines manually to build your daily routine.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onScanOcr,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Scan Rx")
                }
                OutlinedButton(
                    onClick = onAddMedication,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Add Manually")
                }
            }
        }
    }
}
