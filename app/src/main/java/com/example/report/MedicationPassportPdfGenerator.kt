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
import com.example.data.local.entity.MedicationChangeEntity
import com.example.data.local.entity.MedicationEntity
import com.example.data.local.entity.MedicationVersionEntity
import com.example.data.local.entity.PrescriptionRecordEntity
import com.example.data.local.entity.UserProfileEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MedicationPassportPdfGenerator {

    fun generatePassportPdf(
        context: Context,
        userProfile: UserProfileEntity,
        activeMedications: List<MedicationEntity>,
        allMedications: List<MedicationEntity>,
        versions: List<MedicationVersionEntity>,
        changes: List<MedicationChangeEntity>,
        prescriptions: List<PrescriptionRecordEntity>,
        adherencePercentage: Double,
        totalTaken: Int,
        totalMissed: Int
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 standard
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.rgb(0, 104, 116)
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
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val bodyPaint = Paint().apply {
            color = Color.BLACK
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val boldPaint = Paint().apply {
            color = Color.BLACK
            textSize = 9f
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

        val bannerPaint = Paint().apply {
            color = Color.rgb(224, 242, 241)
            style = Paint.Style.FILL
        }

        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        // Header Background Banner
        canvas.drawRect(30f, 30f, 565f, 95f, bannerPaint)
        canvas.drawText("MEDICATION PASSPORT — LONGITUDINAL HEALTH RECORD", 45f, 60f, titlePaint)
        canvas.drawText(
            "Patient: ${userProfile.name} • Age: ${userProfile.age.ifBlank { "N/A" }} • Generated: ${dateFormat.format(Date())}",
            45f,
            80f,
            subtitlePaint
        )

        var y = 115f

        // Overview Summary Boxes
        val boxWidth = 120f
        val boxHeight = 40f
        val startX = 35f

        // Box 1: Active Meds
        canvas.drawRoundRect(startX, y, startX + boxWidth, y + boxHeight, 6f, 6f, boxBgPaint)
        canvas.drawRoundRect(startX, y, startX + boxWidth, y + boxHeight, 6f, 6f, borderPaint)
        canvas.drawText("Active Medicines", startX + 8f, y + 14f, subtitlePaint)
        canvas.drawText("${activeMedications.size}", startX + 8f, y + 32f, Paint(boldPaint).apply { textSize = 13f; color = Color.rgb(0, 104, 116) })

        // Box 2: Past Meds
        val b2X = startX + boxWidth + 10f
        val pastMedsCount = allMedications.count { !it.active }
        canvas.drawRoundRect(b2X, y, b2X + boxWidth, y + boxHeight, 6f, 6f, boxBgPaint)
        canvas.drawRoundRect(b2X, y, b2X + boxWidth, y + boxHeight, 6f, 6f, borderPaint)
        canvas.drawText("Past / Inactive", b2X + 8f, y + 14f, subtitlePaint)
        canvas.drawText("$pastMedsCount", b2X + 8f, y + 32f, Paint(boldPaint).apply { textSize = 13f })

        // Box 3: Recent Changes
        val b3X = b2X + boxWidth + 10f
        canvas.drawRoundRect(b3X, y, b3X + boxWidth, y + boxHeight, 6f, 6f, boxBgPaint)
        canvas.drawRoundRect(b3X, y, b3X + boxWidth, y + boxHeight, 6f, 6f, borderPaint)
        canvas.drawText("Reconciled Changes", b3X + 8f, y + 14f, subtitlePaint)
        canvas.drawText("${changes.size}", b3X + 8f, y + 32f, Paint(boldPaint).apply { textSize = 13f; color = Color.rgb(198, 40, 40) })

        // Box 4: 30-Day Adherence
        val b4X = b3X + boxWidth + 10f
        canvas.drawRoundRect(b4X, y, b4X + boxWidth, y + boxHeight, 6f, 6f, boxBgPaint)
        canvas.drawRoundRect(b4X, y, b4X + boxWidth, y + boxHeight, 6f, 6f, borderPaint)
        canvas.drawText("30-Day Adherence", b4X + 8f, y + 14f, subtitlePaint)
        val adhColor = if (adherencePercentage >= 80.0) Color.rgb(46, 125, 50) else Color.rgb(198, 40, 40)
        canvas.drawText(String.format(Locale.US, "%.1f%% (%d/%d)", adherencePercentage, totalTaken, totalTaken + totalMissed), b4X + 8f, y + 32f, Paint(boldPaint).apply { textSize = 11f; color = adhColor })

        y += boxHeight + 20f

        // Section 1: Current Active Medications
        canvas.drawText("CURRENT ACTIVE MEDICATION PLAN", 35f, y, headerPaint)
        y += 12f

        canvas.drawRect(35f, y, 560f, y + 18f, boxBgPaint)
        canvas.drawText("Medicine Name", 40f, y + 12f, boldPaint)
        canvas.drawText("Strength", 180f, y + 12f, boldPaint)
        canvas.drawText("Form / Route", 270f, y + 12f, boldPaint)
        canvas.drawText("Instructions", 380f, y + 12f, boldPaint)
        canvas.drawText("Stock", 495f, y + 12f, boldPaint)
        y += 18f

        if (activeMedications.isEmpty()) {
            canvas.drawText("No active medications in plan.", 40f, y + 14f, bodyPaint)
            y += 20f
        } else {
            for (med in activeMedications.take(6)) {
                canvas.drawLine(35f, y, 560f, y, borderPaint)
                y += 14f
                canvas.drawText(med.name.take(22), 40f, y, boldPaint)
                canvas.drawText(med.strength.ifBlank { "-" }, 180f, y, bodyPaint)
                canvas.drawText("${med.form.displayName}, ${med.route}", 270f, y, bodyPaint)
                canvas.drawText(med.instructions.take(20), 380f, y, bodyPaint)
                canvas.drawText("${med.stockQuantity.toInt()} ${med.doseUnit.symbol}", 495f, y, bodyPaint)
                y += 4f
            }
        }

        y += 18f

        // Section 2: Medication Progression / Version History
        canvas.drawText("MEDICATION VERSION HISTORY (LONGITUDINAL PROGRESSION)", 35f, y, headerPaint)
        y += 12f

        canvas.drawRect(35f, y, 560f, y + 18f, boxBgPaint)
        canvas.drawText("Medicine", 40f, y + 12f, boldPaint)
        canvas.drawText("Version", 170f, y + 12f, boldPaint)
        canvas.drawText("Dosage & Regimen", 240f, y + 12f, boldPaint)
        canvas.drawText("Validity Period", 390f, y + 12f, boldPaint)
        canvas.drawText("Status", 500f, y + 12f, boldPaint)
        y += 18f

        if (versions.isEmpty()) {
            canvas.drawText("Initial versions recorded. Progression history will appear as plans update.", 40f, y + 14f, bodyPaint)
            y += 20f
        } else {
            for (v in versions.take(6)) {
                canvas.drawLine(35f, y, 560f, y, borderPaint)
                y += 14f
                canvas.drawText(v.name.take(20), 40f, y, boldPaint)
                canvas.drawText("Version ${v.versionNumber}", 170f, y, bodyPaint)
                canvas.drawText("${v.strength}, ${v.frequencyType.displayName}", 240f, y, bodyPaint)
                val startStr = dateFormat.format(Date(v.startDate))
                val endStr = if (v.endDate != null) dateFormat.format(Date(v.endDate)) else "Present"
                canvas.drawText("$startStr – $endStr", 390f, y, bodyPaint)
                val statusColor = if (v.status.name == "ACTIVE") Color.rgb(46, 125, 50) else Color.GRAY
                canvas.drawText(v.status.displayName, 500f, y, Paint(boldPaint).apply { color = statusColor })
                y += 4f
            }
        }

        y += 18f

        // Section 3: Medication Changes Timeline & Decision Log
        canvas.drawText("RECONCILIATION AUDIT LOG (RECENT PRESCRIPTION CHANGES)", 35f, y, headerPaint)
        y += 12f

        canvas.drawRect(35f, y, 560f, y + 18f, boxBgPaint)
        canvas.drawText("Date", 40f, y + 12f, boldPaint)
        canvas.drawText("Medicine", 110f, y + 12f, boldPaint)
        canvas.drawText("Change Detected", 220f, y + 12f, boldPaint)
        canvas.drawText("User Action", 400f, y + 12f, boldPaint)
        canvas.drawText("Status", 495f, y + 12f, boldPaint)
        y += 18f

        if (changes.isEmpty()) {
            canvas.drawText("No prescription changes recorded yet.", 40f, y + 14f, bodyPaint)
            y += 20f
        } else {
            for (c in changes.take(6)) {
                canvas.drawLine(35f, y, 560f, y, borderPaint)
                y += 14f
                canvas.drawText(dateFormat.format(Date(c.createdAt)), 40f, y, bodyPaint)
                canvas.drawText(c.medicineName.take(16), 110f, y, boldPaint)
                val changeDesc = if (c.previousValue.isNotBlank() && c.previousValue != "-") {
                    "${c.previousValue.take(10)} → ${c.newValue.take(10)}"
                } else {
                    c.changeType.displayName
                }
                canvas.drawText(changeDesc, 220f, y, bodyPaint)
                canvas.drawText(c.reviewStatus.displayName, 400f, y, bodyPaint)
                canvas.drawText(c.changeType.name.take(10), 495f, y, Paint(bodyPaint).apply { color = Color.rgb(0, 104, 116) })
                y += 4f
            }
        }

        // Footer & Medical Disclaimer
        val footerY = 800f
        canvas.drawLine(35f, footerY - 15f, 560f, footerY - 15f, borderPaint)
        val disclaimerPaint = Paint().apply {
            color = Color.GRAY
            textSize = 7.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }
        canvas.drawText(
            "Medication Passport: Longitudinal clinical and adherence summary for patient & physician review. Verify against original prescriptions.",
            35f,
            footerY,
            disclaimerPaint
        )
        canvas.drawText(
            "Generated on: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())} • Page 1 of 1",
            35f,
            footerY + 12f,
            disclaimerPaint
        )

        document.finishPage(page)

        val reportsDir = File(context.cacheDir, "passport").apply { mkdirs() }
        val pdfFile = File(reportsDir, "Medication_Passport_${System.currentTimeMillis()}.pdf")
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
            putExtra(Intent.EXTRA_SUBJECT, "Medication Passport Report")
            putExtra(Intent.EXTRA_TEXT, "Attached is the comprehensive Medication Passport and longitudinal prescription history.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
