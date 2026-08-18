package com.example.ui.profile

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.local.entity.UserProfileEntity
import com.example.data.model.UserRole
import com.example.notification.NotificationHelper
import com.example.ui.components.SectionHeader
import com.example.ui.theme.StatusOnTrack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSettingsScreen(
    currentProfile: UserProfileEntity?,
    onSaveProfile: (UserProfileEntity) -> Unit,
    onNavigateToCaregivers: () -> Unit,
    onNavigateToEmergencyId: () -> Unit = {},
    onResetDemoData: () -> Unit,
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(currentProfile?.name ?: "") }
    var selectedRole by remember { mutableStateOf(currentProfile?.role ?: UserRole.PATIENT) }
    var managedPatientName by remember { mutableStateOf(currentProfile?.managedPatientName ?: "") }
    var managedRelationship by remember { mutableStateOf(currentProfile?.managedPatientRelationship ?: "") }

    var remindersEnabled by remember { mutableStateOf(currentProfile?.remindersEnabled ?: true) }
    var missedDoseAlertsEnabled by remember { mutableStateOf(currentProfile?.missedDoseAlertsEnabled ?: true) }
    var defaultSnoozeMinutes by remember { mutableStateOf(currentProfile?.defaultSnoozeMinutes ?: 10) }
    var gracePeriodMinutes by remember { mutableStateOf((currentProfile?.gracePeriodMinutes ?: 60).toFloat()) }
    var trustedContactAlertsEnabled by remember { mutableStateOf(currentProfile?.trustedContactAlertsEnabled ?: true) }
    var repeatedMissedAlertsEnabled by remember { mutableStateOf(currentProfile?.repeatedMissedAlertsEnabled ?: true) }

    var userEmail by remember { mutableStateOf(currentProfile?.userEmail ?: "") }
    var userPhone by remember { mutableStateOf(currentProfile?.userPhone ?: "") }

    var testNotificationSent by remember { mutableStateOf(false) }

    LaunchedEffect(currentProfile) {
        currentProfile?.let { profile ->
            name = profile.name
            selectedRole = profile.role
            managedPatientName = profile.managedPatientName
            managedRelationship = profile.managedPatientRelationship
            remindersEnabled = profile.remindersEnabled
            missedDoseAlertsEnabled = profile.missedDoseAlertsEnabled
            defaultSnoozeMinutes = profile.defaultSnoozeMinutes
            gracePeriodMinutes = profile.gracePeriodMinutes.toFloat()
            trustedContactAlertsEnabled = profile.trustedContactAlertsEnabled
            repeatedMissedAlertsEnabled = profile.repeatedMissedAlertsEnabled
            userEmail = profile.userEmail
            userPhone = profile.userPhone
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Profile & Reminder Settings") })
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
            Spacer(modifier = Modifier.height(4.dp))

            // User Avatar Banner
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (selectedRole == UserRole.CAREGIVER) Icons.Default.FamilyRestroom else Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = name.ifBlank { "User Profile" },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = if (selectedRole == UserRole.CAREGIVER) "Mode: Caregiver (${managedPatientName.ifBlank { "Patient" }})" else "Mode: Patient Self-Management",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Mode Selector
            SectionHeader(title = "App Role & Mode")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = selectedRole == UserRole.PATIENT,
                    onClick = { selectedRole = UserRole.PATIENT },
                    label = { Text("👤 Patient Mode") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedRole == UserRole.CAREGIVER,
                    onClick = { selectedRole = UserRole.CAREGIVER },
                    label = { Text("👥 Caregiver Mode") },
                    modifier = Modifier.weight(1f)
                )
            }

            // Profile Fields
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(if (selectedRole == UserRole.PATIENT) "Patient Name" else "Caregiver Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (selectedRole == UserRole.CAREGIVER) {
                OutlinedTextField(
                    value = managedPatientName,
                    onValueChange = { managedPatientName = it },
                    label = { Text("Managed Patient Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = managedRelationship,
                    onValueChange = { managedRelationship = it },
                    label = { Text("Relationship (e.g. Daughter, Parent, Nurse)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Reminder & Alarm Preferences
            SectionHeader(title = "Medication Reminders & Alarms")
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Medication Reminders",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Play sound, vibration, and show notifications when it is time to take a dose.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = remindersEnabled,
                            onCheckedChange = { remindersEnabled = it }
                        )
                    }

                    // Default Snooze Duration
                    Text(
                        text = "Default Snooze Duration: $defaultSnoozeMinutes minutes",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(5, 10, 15, 30).forEach { mins ->
                            FilterChip(
                                selected = defaultSnoozeMinutes == mins,
                                onClick = { defaultSnoozeMinutes = mins },
                                label = { Text("${mins}m") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Missed Dose Alerts & Grace Period
            SectionHeader(title = "Missed-Dose Grace Period & Alerts")
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Missed Dose Notifications",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Notify patient when a scheduled dose has passed without confirmation.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = missedDoseAlertsEnabled,
                            onCheckedChange = { missedDoseAlertsEnabled = it }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Grace Period Window",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${gracePeriodMinutes.toInt()} minutes",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "Time after scheduled intake before a dose is marked Missed and escalated. Note: Active snooze pauses the grace timer.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Slider(
                        value = gracePeriodMinutes,
                        onValueChange = { gracePeriodMinutes = it },
                        valueRange = 15f..120f,
                        steps = 6
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Caregiver / Contact Escalation",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Alert designated caregivers if a dose remains unconfirmed.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = trustedContactAlertsEnabled,
                            onCheckedChange = { trustedContactAlertsEnabled = it }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Repeated Missed Dose Escalation",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Trigger high-priority alert if 2 or more consecutive doses are missed.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = repeatedMissedAlertsEnabled,
                            onCheckedChange = { repeatedMissedAlertsEnabled = it }
                        )
                    }
                }
            }

            // Quick Link to Caregiver Setup
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToCaregivers() }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FamilyRestroom,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Manage Caregivers & Alert Channels",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "Configure caregiver phones, WhatsApp, and delivery channels",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // Emergency Medical ID & Lock Screen Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToEmergencyId() }
                    .testTag("manage_emergency_medical_id_card")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Emergency,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Emergency Medical ID & QR",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "First responder view, allergies, blood group & lock-screen card",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Test Reminder Notification Generator
            SectionHeader(title = "Diagnostics & Testing")
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Verify Reminder & Missed-Dose Notification Channels",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Preview how the medication reminder dialog and missed dose alerts appear on this device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                NotificationHelper.showMedicationReminder(
                                    context = context,
                                    doseEventId = 99999L,
                                    medicationName = "Amoxicillin",
                                    strength = "500 mg",
                                    doseAmount = 1.0,
                                    doseUnit = "capsule",
                                    instructions = "Take with food",
                                    scheduledTime = "08:00 AM",
                                    snoozeDurationMinutes = defaultSnoozeMinutes
                                )
                                testNotificationSent = true
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Test Reminder", fontSize = 11.sp)
                        }

                        FilledTonalButton(
                            onClick = {
                                NotificationHelper.showPatientMissedDoseNotification(
                                    context = context,
                                    doseEventId = 99999L,
                                    medicationName = "Amoxicillin",
                                    strength = "500 mg",
                                    doseDisplay = "1 capsule",
                                    scheduledTime = "08:00 AM"
                                )
                                testNotificationSent = true
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Test Missed Alert", fontSize = 11.sp)
                        }
                    }

                    if (testNotificationSent) {
                        Text(
                            text = "✓ Test notification dispatched to system tray.",
                            style = MaterialTheme.typography.bodySmall,
                            color = StatusOnTrack
                        )
                    }
                }
            }

            // Save Profile Button
            Button(
                onClick = {
                    val updated = (currentProfile ?: UserProfileEntity()).copy(
                        name = name.trim().ifBlank { "User" },
                        role = selectedRole,
                        managedPatientName = managedPatientName.trim(),
                        managedPatientRelationship = managedRelationship.trim(),
                        remindersEnabled = remindersEnabled,
                        missedDoseAlertsEnabled = missedDoseAlertsEnabled,
                        defaultSnoozeMinutes = defaultSnoozeMinutes,
                        gracePeriodMinutes = gracePeriodMinutes.toInt(),
                        trustedContactAlertsEnabled = trustedContactAlertsEnabled,
                        caregiverMissedDoseAlertsEnabled = trustedContactAlertsEnabled,
                        repeatedMissedAlertsEnabled = repeatedMissedAlertsEnabled,
                        escalationEnabled = trustedContactAlertsEnabled,
                        userEmail = userEmail.trim(),
                        userPhone = userPhone.trim(),
                        updatedAt = System.currentTimeMillis()
                    )
                    onSaveProfile(updated)
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_profile_button")
            ) {
                Text("Save Settings", fontWeight = FontWeight.Bold)
            }

            // Log Out Button
            OutlinedButton(
                onClick = onLogout,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("logout_button")
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log Out", fontWeight = FontWeight.Bold)
            }

            // Reset Sample Demo Data
            OutlinedButton(
                onClick = onResetDemoData,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.LockReset, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Reset Sample Demo Medications")
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
