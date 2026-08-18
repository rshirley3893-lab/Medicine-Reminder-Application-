package com.example.report

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.model.EmergencySnapshot
import com.example.util.QrCodeGenerator
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object EmergencyCardPdfGenerator {

    fun generateEmergencyIdCardPdf(
        context: Context,
        snapshot: EmergencySnapshot
    ): File {
        val reportsDir = File(context.cacheDir, "passport")
        if (!reportsDir.exists()) {
            reportsDir.mkdirs()
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val sanitizedName = snapshot.patientName.replace("[^a-zA-Z0-9]".toRegex(), "_")
        val file = File(reportsDir, "Emergency_Medical_ID_${sanitizedName}_$timestamp.pdf")

        val pdfDoc = PdfDocument()
        // Standard A4 Size: 595 x 842 pt
        val pageWidth = 595
        val pageHeight = 842
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDoc.startPage(pageInfo)
        val canvas = page.canvas

        // Paints
        val primaryPaint = Paint().apply {
            color = Color.rgb(186, 26, 26) // Deep Emergency Red
            isAntiAlias = true
        }

        val navyPaint = Paint().apply {
            color = Color.rgb(20, 35, 60) // Deep Navy
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = Color.rgb(33, 33, 33)
            textSize = 12f
            isAntiAlias = true
        }

        val boldTextPaint = Paint().apply {
            color = Color.rgb(20, 20, 20)
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val subtlePaint = Paint().apply {
            color = Color.rgb(100, 100, 100)
            textSize = 10f
            isAntiAlias = true
        }

        val cardBgPaint = Paint().apply {
            color = Color.rgb(248, 249, 252)
            isAntiAlias = true
        }

        val redBgPaint = Paint().apply {
            color = Color.rgb(255, 235, 235)
            isAntiAlias = true
        }

        val borderPaint = Paint().apply {
            color = Color.rgb(220, 225, 235)
            style = Paint.Style.STROKE
            strokeWidth = 1f
            isAntiAlias = true
        }

        val redBorderPaint = Paint().apply {
            color = Color.rgb(230, 100, 100)
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            isAntiAlias = true
        }

        // Header Background
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 90f, primaryPaint)

        // Header Text
        val headerTitlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val headerSubPaint = Paint().apply {
            color = Color.rgb(255, 220, 220)
            textSize = 11f
            isAntiAlias = true
        }

        canvas.drawText("EMERGENCY MEDICAL ID", 36f, 42f, headerTitlePaint)
        canvas.drawText("CRITICAL PATIENT INFORMATION • FIRST RESPONDER SNAPSHOT", 36f, 64f, headerSubPaint)

        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val lastUpdatedStr = "Last Updated: " + dateFormat.format(Date(snapshot.lastUpdated))
        canvas.drawText(lastUpdatedStr, pageWidth - 36f - headerSubPaint.measureText(lastUpdatedStr), 64f, headerSubPaint)

        var y = 115f

        // Top Row: Patient Info Card (Left) & Emergency QR Code Card (Right)
        val leftCardRect = RectF(36f, y, 380f, y + 155f)
        canvas.drawRoundRect(leftCardRect, 8f, 8f, cardBgPaint)
        canvas.drawRoundRect(leftCardRect, 8f, 8f, borderPaint)

        canvas.drawText("PATIENT IDENTITY", 52f, y + 24f, boldTextPaint)

        var py = y + 46f
        canvas.drawText("Full Name: ${snapshot.patientName.ifBlank { "Not provided" }}", 52f, py, textPaint)
        py += 20f
        if (snapshot.preferredName.isNotBlank()) {
            canvas.drawText("Preferred Name: ${snapshot.preferredName}", 52f, py, textPaint)
            py += 20f
        }
        val ageDobStr = buildString {
            if (snapshot.age.isNotBlank()) append("Age: ${snapshot.age} yrs   ")
            if (snapshot.dob.isNotBlank()) append("DOB: ${snapshot.dob}   ")
            if (snapshot.gender.isNotBlank()) append("Gender: ${snapshot.gender}")
        }
        canvas.drawText(ageDobStr.ifBlank { "Age / DOB: Not provided" }, 52f, py, textPaint)
        py += 20f

        val bloodGroupStr = "Blood Group: " + (if (snapshot.bloodGroup.isNotBlank()) snapshot.bloodGroup else "Unknown")
        val bgPaint = if (snapshot.bloodGroup.isNotBlank() && snapshot.bloodGroup != "Unknown") boldTextPaint else textPaint
        canvas.drawText(bloodGroupStr, 52f, py, bgPaint)
        py += 20f

        if (snapshot.organDonor) {
            canvas.drawText("Organ Donor: YES", 52f, py, boldTextPaint)
        }

        // QR Code Card (Right)
        val rightCardRect = RectF(396f, y, pageWidth - 36f, y + 155f)
        canvas.drawRoundRect(rightCardRect, 8f, 8f, cardBgPaint)
        canvas.drawRoundRect(rightCardRect, 8f, 8f, borderPaint)

        val qrContent = if (snapshot.emergencyIdentifier.isNotBlank()) {
            "https://emergency.medremind.app/id/${snapshot.emergencyIdentifier}"
        } else {
            "EMERGENCY_ID:${snapshot.patientName}"
        }
        try {
            val qrBitmap = QrCodeGenerator.encodeToBitmap(qrContent, 105, 105)
            canvas.drawBitmap(qrBitmap, 430f, y + 15f, null)
            val qrLabelPaint = Paint().apply {
                color = Color.rgb(80, 80, 80)
                textSize = 8.5f
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText("Scan for Emergency Info", 482f, y + 135f, qrLabelPaint)
            canvas.drawText("Ref: ${snapshot.emergencyIdentifier.take(8)}", 482f, y + 147f, subtlePaint.apply { textAlign = Paint.Align.CENTER })
        } catch (_: Exception) {}

        y += 170f

        // ALLERGIES SECTION (High Priority Warning Banner)
        val allergyCardRect = RectF(36f, y, pageWidth - 36f, y + (if (snapshot.allergies.isNotEmpty()) (40f + snapshot.allergies.size * 22f) else 50f))
        if (snapshot.allergies.isNotEmpty()) {
            canvas.drawRoundRect(allergyCardRect, 8f, 8f, redBgPaint)
            canvas.drawRoundRect(allergyCardRect, 8f, 8f, redBorderPaint)
        } else {
            canvas.drawRoundRect(allergyCardRect, 8f, 8f, cardBgPaint)
            canvas.drawRoundRect(allergyCardRect, 8f, 8f, borderPaint)
        }

        val allergyTitlePaint = Paint().apply {
            color = if (snapshot.allergies.isNotEmpty()) Color.rgb(186, 26, 26) else Color.rgb(30, 30, 30)
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("⚠️ ALLERGIES & ADVERSE REACTIONS", 52f, y + 24f, allergyTitlePaint)

        var ay = y + 44f
        if (snapshot.allergies.isEmpty()) {
            canvas.drawText("No known drug or environmental allergies recorded by patient.", 52f, ay, textPaint)
            y += 65f
        } else {
            for (allergy in snapshot.allergies) {
                val detail = buildString {
                    append("•  ${allergy.allergen}")
                    if (allergy.reaction.isNotBlank()) append(" — Reaction: ${allergy.reaction}")
                    append(" [${allergy.severity}]")
                }
                canvas.drawText(detail, 52f, ay, boldTextPaint)
                ay += 22f
            }
            y = ay + 12f
        }

        // MEDICAL CONDITIONS SECTION
        val condCardRect = RectF(36f, y, pageWidth - 36f, y + (if (snapshot.medicalConditions.isNotEmpty()) (40f + snapshot.medicalConditions.size * 18f) else 50f))
        canvas.drawRoundRect(condCardRect, 8f, 8f, cardBgPaint)
        canvas.drawRoundRect(condCardRect, 8f, 8f, borderPaint)

        canvas.drawText("MEDICAL CONDITIONS", 52f, y + 24f, boldTextPaint)
        var cy = y + 44f
        if (snapshot.medicalConditions.isEmpty()) {
            canvas.drawText("No medical conditions recorded.", 52f, cy, textPaint)
            y += 65f
        } else {
            for (cond in snapshot.medicalConditions) {
                canvas.drawText("•  $cond", 52f, cy, textPaint)
                cy += 18f
            }
            y = cy + 12f
        }

        // CURRENT MEDICATIONS SECTION
        val medsHeight = (45f + (snapshot.currentMedications.size.coerceAtLeast(1) * 22f))
        val medsCardRect = RectF(36f, y, pageWidth - 36f, y + medsHeight)
        canvas.drawRoundRect(medsCardRect, 8f, 8f, cardBgPaint)
        canvas.drawRoundRect(medsCardRect, 8f, 8f, borderPaint)

        canvas.drawText("CURRENT VERIFIED MEDICATIONS (Active Plan)", 52f, y + 24f, boldTextPaint)
        var my = y + 44f
        if (snapshot.currentMedications.isEmpty()) {
            canvas.drawText("No active medications in current verified plan.", 52f, my, textPaint)
            y += 65f
        } else {
            for (med in snapshot.currentMedications) {
                val medDesc = buildString {
                    append("•  ${med.name} ${med.strength} (${med.form})")
                    if (med.instructions.isNotBlank()) append(" — ${med.instructions}")
                    if (med.frequency.isNotBlank()) append(" [${med.frequency}]")
                }
                canvas.drawText(medDesc, 52f, my, textPaint)
                my += 22f
            }
            y = my + 12f
        }

        // EMERGENCY CONTACTS SECTION
        val contactsHeight = (45f + (snapshot.emergencyContacts.size.coerceAtLeast(1) * 22f))
        val contactCardRect = RectF(36f, y, pageWidth - 36f, y + contactsHeight)
        canvas.drawRoundRect(contactCardRect, 8f, 8f, cardBgPaint)
        canvas.drawRoundRect(contactCardRect, 8f, 8f, borderPaint)

        canvas.drawText("EMERGENCY CONTACTS", 52f, y + 24f, boldTextPaint)
        var cty = y + 44f
        if (snapshot.emergencyContacts.isEmpty()) {
            canvas.drawText("No emergency contacts specified.", 52f, cty, textPaint)
            y += 65f
        } else {
            for (contact in snapshot.emergencyContacts) {
                val contactStr = "•  ${contact.name} (${contact.relationship}) — Phone: ${contact.phone} ${if (contact.isPrimary) "[Primary Contact]" else ""}"
                canvas.drawText(contactStr, 52f, cty, boldTextPaint)
                cty += 22f
            }
            y = cty + 12f
        }

        // DOCTOR & NOTES SECTION
        if (snapshot.primaryDoctorName.isNotBlank() || snapshot.importantNotes.isNotBlank() || snapshot.hospitalClinicName.isNotBlank()) {
            val docHeight = 70f
            val docCardRect = RectF(36f, y, pageWidth - 36f, y + docHeight)
            canvas.drawRoundRect(docCardRect, 8f, 8f, cardBgPaint)
            canvas.drawRoundRect(docCardRect, 8f, 8f, borderPaint)

            canvas.drawText("HEALTHCARE PROVIDER & EMERGENCY NOTES", 52f, y + 22f, boldTextPaint)
            var dy = y + 40f
            if (snapshot.primaryDoctorName.isNotBlank()) {
                val docStr = "Doctor: ${snapshot.primaryDoctorName} ${if (snapshot.hospitalClinicName.isNotBlank()) "(${snapshot.hospitalClinicName})" else ""} ${if (snapshot.primaryDoctorPhone.isNotBlank()) "• Tel: ${snapshot.primaryDoctorPhone}" else ""}"
                canvas.drawText(docStr, 52f, dy, textPaint)
                dy += 18f
            }
            if (snapshot.importantNotes.isNotBlank()) {
                canvas.drawText("Note: ${snapshot.importantNotes}", 52f, dy, textPaint)
            }
            y += docHeight + 15f
        }

        // FOOTER & DISCLAIMER
        val footerPaint = Paint().apply {
            color = Color.rgb(130, 130, 130)
            textSize = 8.5f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(
            "DISCLAIMER: This Medical ID is an assistive emergency summary provided by the patient. In life-threatening emergencies, call official services (112).",
            pageWidth / 2f,
            pageHeight - 35f,
            footerPaint
        )
        canvas.drawText(
            "Generated securely via Medicine Reminder • Offline Medical ID & Emergency Snapshot v1.0",
            pageWidth / 2f,
            pageHeight - 22f,
            footerPaint
        )

        pdfDoc.finishPage(page)

        FileOutputStream(file).use { out ->
            pdfDoc.writeTo(out)
        }
        pdfDoc.close()

        return file
    }
}
