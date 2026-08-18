package com.example.report

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.local.dao.DoseWithMedication
import com.example.data.local.entity.MedicationEntity
import com.example.data.local.entity.UserProfileEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AdherenceReportData(
    val userProfile: UserProfileEntity,
    val startDate: Long,
    val endDate: Long,
    val totalScheduled: Int,
    val totalTaken: Int,
    val totalMissed: Int,
    val totalSkipped: Int,
    val totalSnoozed: Int,
    val adherencePercentage: Double,
    val medications: List<MedicationEntity>,
    val doseLogs: List<DoseWithMedication>
)

object PdfReportGenerator {

    fun generateAdherencePdf(context: Context, reportData: AdherenceReportData): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 (72 dpi)
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.rgb(0, 104, 116) // Teal
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val subtitlePaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val headerPaint = Paint().apply {
            color = Color.rgb(0, 79, 88)
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val bodyPaint = Paint().apply {
            color = Color.BLACK
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val boldPaint = Paint().apply {
            color = Color.BLACK
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val borderPaint = Paint().apply {
            color = Color.rgb(219, 228, 230)
            strokeWidth = 1f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        val boxBgPaint = Paint().apply {
            color = Color.rgb(240, 248, 250)
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault())

        // Header Background Banner
        val bannerPaint = Paint().apply {
            color = Color.rgb(230, 244, 246)
            style = Paint.Style.FILL
        }
        canvas.drawRect(30f, 30f, 565f, 95f, bannerPaint)

        // App Logo & Title
        canvas.drawText("MEDICINE REMINDER — ADHERENCE REPORT", 45f, 60f, titlePaint)
        canvas.drawText("Patient: ${reportData.userProfile.name} • Range: ${dateFormat.format(Date(reportData.startDate))} - ${dateFormat.format(Date(reportData.endDate))}", 45f, 80f, subtitlePaint)

        var y = 120f

        // Metric Summary Cards
        canvas.drawText("EXECUTIVE SUMMARY", 35f, y, headerPaint)
        y += 15f

        // Draw 4 Metric Boxes
        val boxWidth = 120f
        val boxHeight = 45f
        val startX = 35f

        // Box 1: Adherence %
        canvas.drawRoundRect(startX, y, startX + boxWidth, y + boxHeight, 8f, 8f, boxBgPaint)
        canvas.drawRoundRect(startX, y, startX + boxWidth, y + boxHeight, 8f, 8f, borderPaint)
        canvas.drawText("Adherence Rate", startX + 10f, y + 16f, subtitlePaint)
        val adhColor = if (reportData.adherencePercentage >= 80.0) Color.rgb(46, 125, 50) else Color.rgb(198, 40, 40)
        val adhPaint = Paint(boldPaint).apply { color = adhColor; textSize = 14f }
        canvas.drawText(String.format(Locale.US, "%.1f%%", reportData.adherencePercentage), startX + 10f, y + 36f, adhPaint)

        // Box 2: Taken
        val b2X = startX + boxWidth + 10f
        canvas.drawRoundRect(b2X, y, b2X + boxWidth, y + boxHeight, 8f, 8f, boxBgPaint)
        canvas.drawRoundRect(b2X, y, b2X + boxWidth, y + boxHeight, 8f, 8f, borderPaint)
        canvas.drawText("Taken Doses", b2X + 10f, y + 16f, subtitlePaint)
        canvas.drawText("${reportData.totalTaken} / ${reportData.totalScheduled}", b2X + 10f, y + 36f, Paint(boldPaint).apply { textSize = 13f })

        // Box 3: Missed
        val b3X = b2X + boxWidth + 10f
        canvas.drawRoundRect(b3X, y, b3X + boxWidth, y + boxHeight, 8f, 8f, boxBgPaint)
        canvas.drawRoundRect(b3X, y, b3X + boxWidth, y + boxHeight, 8f, 8f, borderPaint)
        canvas.drawText("Missed Doses", b3X + 10f, y + 16f, subtitlePaint)
        canvas.drawText("${reportData.totalMissed}", b3X + 10f, y + 36f, Paint(boldPaint).apply { color = Color.rgb(198, 40, 40); textSize = 13f })

        // Box 4: Snoozed / Skipped
        val b4X = b3X + boxWidth + 10f
        canvas.drawRoundRect(b4X, y, b4X + boxWidth, y + boxHeight, 8f, 8f, boxBgPaint)
        canvas.drawRoundRect(b4X, y, b4X + boxWidth, y + boxHeight, 8f, 8f, borderPaint)
        canvas.drawText("Snoozed / Skipped", b4X + 10f, y + 16f, subtitlePaint)
        canvas.drawText("${reportData.totalSnoozed} / ${reportData.totalSkipped}", b4X + 10f, y + 36f, Paint(boldPaint).apply { textSize = 13f })

        y += boxHeight + 25f

        // Medication List Section
        canvas.drawText("ACTIVE MEDICATIONS & STOCK STATUS", 35f, y, headerPaint)
        y += 15f

        // Table Header
        canvas.drawRect(35f, y, 560f, y + 20f, boxBgPaint)
        canvas.drawText("Medicine Name", 40f, y + 14f, boldPaint)
        canvas.drawText("Strength", 180f, y + 14f, boldPaint)
        canvas.drawText("Form & Route", 270f, y + 14f, boldPaint)
        canvas.drawText("Instructions", 380f, y + 14f, boldPaint)
        canvas.drawText("Stock", 490f, y + 14f, boldPaint)
        y += 20f

        for (med in reportData.medications.take(8)) {
            canvas.drawLine(35f, y, 560f, y, borderPaint)
            y += 15f
            canvas.drawText(med.name.take(20), 40f, y, bodyPaint)
            canvas.drawText(med.strength.ifBlank { "-" }, 180f, y, bodyPaint)
            canvas.drawText("${med.form.displayName}, ${med.route}", 270f, y, bodyPaint)
            canvas.drawText(med.instructions.take(18), 380f, y, bodyPaint)
            val stockText = "${med.stockQuantity.toInt()} (${med.doseUnit.symbol})"
            canvas.drawText(stockText, 490f, y, bodyPaint)
            y += 5f
        }

        y += 20f

        // Recent Dose History
        canvas.drawText("RECENT DOSE TIMELINE LOGS", 35f, y, headerPaint)
        y += 15f

        canvas.drawRect(35f, y, 560f, y + 20f, boxBgPaint)
        canvas.drawText("Scheduled Time", 40f, y + 14f, boldPaint)
        canvas.drawText("Medication", 170f, y + 14f, boldPaint)
        canvas.drawText("Dose", 310f, y + 14f, boldPaint)
        canvas.drawText("Status", 400f, y + 14f, boldPaint)
        canvas.drawText("Recorded Time", 470f, y + 14f, boldPaint)
        y += 20f

        for (dose in reportData.doseLogs.take(14)) {
            canvas.drawLine(35f, y, 560f, y, borderPaint)
            y += 14f
            canvas.drawText(timeFormat.format(Date(dose.doseEvent.scheduledAt)), 40f, y, bodyPaint)
            canvas.drawText(dose.medication.name.take(18), 170f, y, bodyPaint)
            canvas.drawText("${dose.medication.doseAmount} ${dose.medication.doseUnit.symbol}", 310f, y, bodyPaint)
            
            val statusColor = when (dose.doseEvent.status.name) {
                "TAKEN" -> Color.rgb(46, 125, 50)
                "MISSED" -> Color.rgb(198, 40, 40)
                "SNOOZED" -> Color.rgb(2, 136, 209)
                else -> Color.GRAY
            }
            canvas.drawText(dose.doseEvent.status.name, 400f, y, Paint(boldPaint).apply { color = statusColor })
            val recorded = if (dose.doseEvent.takenAt != null) timeFormat.format(Date(dose.doseEvent.takenAt)) else "-"
            canvas.drawText(recorded, 470f, y, bodyPaint)
            y += 4f
        }

        // Footer & Medical Disclaimer
        val footerY = 800f
        canvas.drawLine(35f, footerY - 15f, 560f, footerY - 15f, borderPaint)
        val disclaimerPaint = Paint().apply {
            color = Color.GRAY
            textSize = 7.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }
        canvas.drawText("Disclaimer: This report is generated automatically by Medicine Reminder app for personal routine tracking and clinician review.", 35f, footerY, disclaimerPaint)
        canvas.drawText("Generated on: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())} • Page 1 of 1", 35f, footerY + 12f, disclaimerPaint)

        document.finishPage(page)

        // Save PDF to cache directory
        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val pdfFile = File(reportsDir, "Medication_Adherence_Report_${System.currentTimeMillis()}.pdf")
        FileOutputStream(pdfFile).use { out ->
            document.writeTo(out)
        }
        document.close()

        return pdfFile
    }

    fun getShareIntent(context: Context, pdfFile: File): Intent {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            pdfFile
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Medication Adherence Report")
            putExtra(Intent.EXTRA_TEXT, "Attached is the medication adherence and routine summary report.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
