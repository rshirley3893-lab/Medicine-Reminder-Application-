package com.example.ui.reports

import android.content.Context
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.data.local.dao.DoseWithMedication
import com.example.data.model.DoseStatus
import com.example.report.PdfReportGenerator
import com.example.ui.components.MetricSummaryCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.StatusMissed
import com.example.ui.theme.StatusOnTrack
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusSnoozed
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    doseHistory: List<DoseWithMedication>,
    onGeneratePdf: suspend (Int) -> File
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedDaysRange by remember { mutableStateOf(7) }
    var isGeneratingPdf by remember { mutableStateOf(false) }
    var generatedFile by remember { mutableStateOf<File?>(null) }

    val cutoffTime = remember(selectedDaysRange) {
        System.currentTimeMillis() - (selectedDaysRange * 24 * 60 * 60 * 1000L)
    }

    val filteredHistory = remember(doseHistory, cutoffTime) {
        doseHistory.filter { it.doseEvent.scheduledAt >= cutoffTime }
    }

    val totalDoses = filteredHistory.size
    val takenDoses = filteredHistory.count { it.doseEvent.status == DoseStatus.TAKEN }
    val missedDoses = filteredHistory.count { it.doseEvent.status == DoseStatus.MISSED }
    val skippedDoses = filteredHistory.count { it.doseEvent.status == DoseStatus.SKIPPED }
    val snoozedDoses = filteredHistory.count { it.doseEvent.status == DoseStatus.SNOOZED }

    val adherencePercentage = if (takenDoses + missedDoses > 0) {
        (takenDoses.toDouble() / (takenDoses + missedDoses)) * 100.0
    } else if (totalDoses > 0) {
        100.0
    } else {
        100.0
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Time Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(7 to "Last 7 Days", 14 to "Last 14 Days", 30 to "Last 30 Days").forEach { (days, label) ->
                FilterChip(
                    selected = selectedDaysRange == days,
                    onClick = { selectedDaysRange = days },
                    label = { Text(label) }
                )
            }
        }

        // Overall Score Hero Banner
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (adherencePercentage >= 80.0) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Adherence Score",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format(Locale.US, "%.1f%%", adherencePercentage),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (adherencePercentage >= 80.0) StatusOnTrack else Color(0xFFE65100)
                    )
                    Text(
                        text = if (adherencePercentage >= 80.0) "Excellent compliance! Keep it up." else "Attention needed: doses missed recently.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(if (adherencePercentage >= 80.0) StatusOnTrack.copy(alpha = 0.2f) else Color(0xFFE65100).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (adherencePercentage >= 80.0) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (adherencePercentage >= 80.0) StatusOnTrack else Color(0xFFE65100),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        // Detailed Metric Breakdown Cards
        SectionHeader(title = "Dose Statistics")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricSummaryCard(
                title = "Total Taken",
                value = "$takenDoses",
                subtitle = "On time",
                icon = Icons.Default.CheckCircle,
                accentColor = StatusOnTrack,
                modifier = Modifier.weight(1f)
            )
            MetricSummaryCard(
                title = "Missed",
                value = "$missedDoses",
                subtitle = "Past grace period",
                icon = Icons.Default.Warning,
                accentColor = StatusMissed,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricSummaryCard(
                title = "Snoozed",
                value = "$snoozedDoses",
                subtitle = "Pushed back",
                icon = Icons.Default.HourglassEmpty,
                accentColor = StatusSnoozed,
                modifier = Modifier.weight(1f)
            )
            MetricSummaryCard(
                title = "Skipped",
                value = "$skippedDoses",
                subtitle = "Intentional skip",
                icon = Icons.Default.Schedule,
                accentColor = Color.Gray,
                modifier = Modifier.weight(1f)
            )
        }

        // 7-Day Visual Day Breakdown Bar Graph
        SectionHeader(title = "Daily Performance Trend")
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val daysTrend = remember(filteredHistory) {
                        compute7DaysTrend(filteredHistory)
                    }

                    daysTrend.forEach { dayStat ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(36.dp)
                        ) {
                            val barHeight = (dayStat.complianceRate * 80).coerceIn(8.0, 80.0).dp

                            Box(
                                modifier = Modifier
                                    .width(16.dp)
                                    .height(barHeight)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (dayStat.total == 0) Color(0xFFEEEEEE)
                                        else if (dayStat.complianceRate >= 0.8) StatusOnTrack
                                        else if (dayStat.complianceRate >= 0.5) StatusPending
                                        else StatusMissed
                                    )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = dayStat.dayLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Export PDF Clinician Report Card
        SectionHeader(title = "Clinical Share & Export")
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Clinician Summary Report",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Download or share comprehensive adherence logs and stock summaries for doctor visits.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        coroutineScope.launch {
                            isGeneratingPdf = true
                            try {
                                val file = onGeneratePdf(selectedDaysRange)
                                generatedFile = file
                                val shareIntent = PdfReportGenerator.getShareIntent(context, file)
                                context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Adherence Report PDF"))
                            } catch (e: Exception) {
                                e.printStackTrace()
                            } finally {
                                isGeneratingPdf = false
                            }
                        }
                    },
                    enabled = !isGeneratingPdf,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("export_pdf_button")
                ) {
                    if (isGeneratingPdf) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generating PDF...")
                    } else {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate & Share PDF Report", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

private data class DayStat(val dayLabel: String, val taken: Int, val total: Int, val complianceRate: Double)

private fun compute7DaysTrend(doses: List<DoseWithMedication>): List<DayStat> {
    val result = mutableListOf<DayStat>()
    val cal = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -6)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    for (i in 0..6) {
        val start = cal.timeInMillis
        val end = start + (24 * 60 * 60 * 1000L) - 1
        val dayLabel = SimpleDateFormat("EEE", Locale.getDefault()).format(cal.time)

        val dayDoses = doses.filter { it.doseEvent.scheduledAt in start..end }
        val taken = dayDoses.count { it.doseEvent.status == DoseStatus.TAKEN }
        val total = dayDoses.size
        val rate = if (total > 0) taken.toDouble() / total else 1.0

        result.add(DayStat(dayLabel, taken, total, rate))
        cal.add(Calendar.DAY_OF_YEAR, 1)
    }
    return result
}
