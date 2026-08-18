package com.example.ui.schedule

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.dao.DoseWithMedication
import com.example.data.model.DoseStatus
import com.example.ui.components.DoseStatusBadge
import com.example.ui.components.SectionHeader
import com.example.ui.theme.StatusMissed
import com.example.ui.theme.StatusOnTrack
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleTimelineScreen(
    selectedDateMillis: Long,
    dosesForDay: List<DoseWithMedication>,
    onSelectDate: (Long) -> Unit,
    onTakeDose: (Long) -> Unit,
    onSnoozeDose: (Long) -> Unit,
    onSkipDose: (Long) -> Unit,
    onRescheduleDose: (Long, Long) -> Unit
) {
    var showRescheduleDialogForDose by remember { mutableStateOf<DoseWithMedication?>(null) }
    var rescheduleTimeInput by remember { mutableStateOf("09:00") }

    val daysStrip = remember {
        val list = mutableListOf<Long>()
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -2) // Show 2 days in past
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        for (i in 0..9) { // 10 days total
            list.add(cal.timeInMillis)
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    val totalDoses = dosesForDay.size
    val takenDoses = dosesForDay.count { it.doseEvent.status == DoseStatus.TAKEN }
    val missedDoses = dosesForDay.count { it.doseEvent.status == DoseStatus.MISSED }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Horizontal Date Selector Strip
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(daysStrip) { dateMillis ->
                val isSelected = isSameDay(dateMillis, selectedDateMillis)
                val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
                val dayOfWeek = SimpleDateFormat("EEE", Locale.getDefault()).format(cal.time)
                val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH).toString()

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp),
                    modifier = Modifier
                        .clickable { onSelectDate(dateMillis) }
                        .testTag("date_chip_$dayOfMonth")
                ) {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = dayOfWeek,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = dayOfMonth,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Day Summary Banner
        val cal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
        val dateHeaderStr = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(cal.time)

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = dateHeaderStr,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (totalDoses > 0) "$takenDoses of $totalDoses doses completed ($missedDoses missed)" else "No doses scheduled for this date",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (totalDoses > 0) {
                    val rate = (takenDoses.toDouble() / totalDoses) * 100.0
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (rate >= 80.0) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                    ) {
                        Text(
                            text = String.format(Locale.US, "%.0f%%", rate),
                            fontWeight = FontWeight.Bold,
                            color = if (rate >= 80.0) StatusOnTrack else Color(0xFFE65100),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        SectionHeader(title = "Scheduled Intakes")

        if (dosesForDay.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(60.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No doses on this day",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Your medicines are scheduled based on your active frequency rules.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(dosesForDay, key = { it.doseEvent.id }) { doseWithMed ->
                    ScheduleDoseDetailCard(
                        doseWithMed = doseWithMed,
                        onTake = { onTakeDose(doseWithMed.doseEvent.id) },
                        onSnooze = { onSnoozeDose(doseWithMed.doseEvent.id) },
                        onSkip = { onSkipDose(doseWithMed.doseEvent.id) },
                        onReschedule = {
                            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                            rescheduleTimeInput = timeFormat.format(Date(doseWithMed.doseEvent.scheduledAt))
                            showRescheduleDialogForDose = doseWithMed
                        }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }
    }

    // Reschedule Time Dialog
    if (showRescheduleDialogForDose != null) {
        val target = showRescheduleDialogForDose!!
        AlertDialog(
            onDismissRequest = { showRescheduleDialogForDose = null },
            title = { Text("Reschedule Dose Time") },
            text = {
                Column {
                    Text(
                        text = "Adjust scheduled time for ${target.medication.name}:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = rescheduleTimeInput,
                        onValueChange = { rescheduleTimeInput = it },
                        label = { Text("Time (HH:mm)") },
                        placeholder = { Text("08:30") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parts = rescheduleTimeInput.split(":")
                        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 8
                        val min = parts.getOrNull(1)?.toIntOrNull() ?: 0
                        val cal = Calendar.getInstance().apply {
                            timeInMillis = target.doseEvent.scheduledAt
                            set(Calendar.HOUR_OF_DAY, hour)
                            set(Calendar.MINUTE, min)
                            set(Calendar.SECOND, 0)
                        }
                        onRescheduleDose(target.doseEvent.id, cal.timeInMillis)
                        showRescheduleDialogForDose = null
                    }
                ) {
                    Text("Save Time")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRescheduleDialogForDose = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ScheduleDoseDetailCard(
    doseWithMed: DoseWithMedication,
    onTake: () -> Unit,
    onSnooze: () -> Unit,
    onSkip: () -> Unit,
    onReschedule: () -> Unit
) {
    val dose = doseWithMed.doseEvent
    val med = doseWithMed.medication
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val scheduledTimeStr = timeFormat.format(Date(dose.scheduledAt))

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF006874).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Medication,
                            contentDescription = null,
                            tint = Color(0xFF006874),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = med.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${med.strength} • ${med.doseAmount} ${med.doseUnit.symbol} • Scheduled for $scheduledTimeStr",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                DoseStatusBadge(status = dose.status)
            }

            if (med.instructions.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Instructions: ${med.instructions}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 52.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onReschedule) {
                    Icon(
                        imageVector = Icons.Default.EditCalendar,
                        contentDescription = "Reschedule",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (dose.status == DoseStatus.SCHEDULED || dose.status == DoseStatus.SNOOZED) {
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
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Take", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun isSameDay(time1: Long, time2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = time1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = time2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
