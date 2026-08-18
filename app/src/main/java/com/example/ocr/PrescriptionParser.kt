package com.example.ocr

import com.example.data.local.entity.OcrCandidateEntity
import com.example.data.model.ConfidenceLevel
import com.example.data.model.DoseUnit
import com.example.data.model.FrequencyType
import java.util.Locale
import java.util.regex.Pattern

data class ParsedPrescriptionResult(
    val rawText: String,
    val candidates: List<OcrCandidateEntity>,
    val detectedDoctorName: String? = null,
    val detectedDate: String? = null,
    val clinicName: String = "",
    val hasWarning: Boolean = false,
    val warningMessage: String? = null
) {
    val doctorName: String get() = detectedDoctorName ?: ""
    val prescriptionDate: Long get() = System.currentTimeMillis()
}

object PrescriptionParser {

    private val STRENGTH_PATTERN = Pattern.compile(
        """(\d+(?:\.\d+)?)\s*(mg|mcg|g|ml|iu|units?|%)\b""",
        Pattern.CASE_INSENSITIVE
    )

    private val FREQUENCY_MATRIX_PATTERN = Pattern.compile(
        """\b([012])\s*[-/:]\s*([012])\s*[-/:]\s*([012])(?:\s*[-/:]\s*([012]))?\b"""
    )

    private val DURATION_PATTERN = Pattern.compile(
        """\b(\d+)\s*(days?|weeks?|months?|d|w|m)\b""",
        Pattern.CASE_INSENSITIVE
    )

    fun parse(rawText: String): ParsedPrescriptionResult {
        if (rawText.isBlank()) {
            return ParsedPrescriptionResult(
                rawText = rawText,
                candidates = emptyList(),
                hasWarning = true,
                warningMessage = "No readable text was detected. Please retake photo with clear lighting and focus."
            )
        }

        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }
        val candidates = mutableListOf<OcrCandidateEntity>()

        var currentMedName: String? = null
        var currentStrength: String? = null
        var currentDose: Double = 1.0
        var currentUnit: DoseUnit = DoseUnit.TABLET
        var currentFrequency: FrequencyType = FrequencyType.DAILY
        var currentInstructions: String = "After food"
        var currentDuration: String = "7 days"
        var nameConfidence = ConfidenceLevel.LOW
        var strengthConfidence = ConfidenceLevel.NOT_DETECTED
        var freqConfidence = ConfidenceLevel.NOT_DETECTED
        var durConfidence = ConfidenceLevel.NOT_DETECTED

        for (line in lines) {
            val upper = line.uppercase(Locale.ROOT)

            // Skip typical header / doctor meta lines
            if (upper.contains("CLINIC") || upper.contains("HOSPITAL") ||
                upper.contains("DR.") || upper.contains("DOCTOR") ||
                upper.contains("REG NO") || upper.contains("PATIENT NAME") ||
                upper.contains("DATE:") || upper.contains("AGE:") || upper.contains("GENDER:")) {
                continue
            }

            // Check if line contains a medicine name starter or prefix: TAB, CAP, SYRUP, INJ, CAP., TAB., RX
            val isMedLine = upper.startsWith("TAB") || upper.startsWith("CAP") ||
                    upper.startsWith("SYP") || upper.startsWith("SYRUP") ||
                    upper.startsWith("INJ") || upper.startsWith("OINT") ||
                    upper.startsWith("DROPS") || upper.startsWith("RX") ||
                    upper.matches(Regex("""^\d+[\.\)]\s*[A-Z].*"""))

            // Extract strength
            val strengthMatcher = STRENGTH_PATTERN.matcher(line)
            var foundStrength: String? = null
            if (strengthMatcher.find()) {
                foundStrength = strengthMatcher.group(0)
            }

            // Extract frequency pattern like 1-0-1 or 1-1-1
            val freqMatcher = FREQUENCY_MATRIX_PATTERN.matcher(line)
            var foundFrequency: FrequencyType? = null
            if (freqMatcher.find()) {
                val m1 = freqMatcher.group(1)?.toIntOrNull() ?: 0
                val m2 = freqMatcher.group(2)?.toIntOrNull() ?: 0
                val m3 = freqMatcher.group(3)?.toIntOrNull() ?: 0
                val sum = m1 + m2 + m3
                foundFrequency = when {
                    sum >= 3 -> FrequencyType.THREE_TIMES_DAILY
                    sum == 2 -> FrequencyType.TWICE_DAILY
                    else -> FrequencyType.DAILY
                }
            } else if (upper.contains("TID") || upper.contains("THRICE") || upper.contains("3 TIMES") || upper.contains("T.I.D")) {
                foundFrequency = FrequencyType.THREE_TIMES_DAILY
            } else if (upper.contains("BID") || upper.contains("TWICE") || upper.contains("2 TIMES") || upper.contains("B.I.D")) {
                foundFrequency = FrequencyType.TWICE_DAILY
            } else if (upper.contains("OD") || upper.contains("ONCE") || upper.contains("DAILY") || upper.contains("O.D")) {
                foundFrequency = FrequencyType.DAILY
            } else if (upper.contains("SOS") || upper.contains("PRN") || upper.contains("AS NEEDED")) {
                foundFrequency = FrequencyType.AS_NEEDED
            }

            // Extract instructions
            var foundInstructions: String? = null
            if (upper.contains("AFTER FOOD") || upper.contains("PC") || upper.contains("AFTER MEAL") || upper.contains("P.C")) {
                foundInstructions = "After food"
            } else if (upper.contains("BEFORE FOOD") || upper.contains("AC") || upper.contains("BEFORE MEAL") || upper.contains("A.C")) {
                foundInstructions = "Before food"
            } else if (upper.contains("EMPTY STOMACH")) {
                foundInstructions = "Empty stomach"
            } else if (upper.contains("BEDTIME") || upper.contains("HS") || upper.contains("AT NIGHT")) {
                foundInstructions = "At bedtime"
            }

            // Extract duration
            val durMatcher = DURATION_PATTERN.matcher(line)
            var foundDuration: String? = null
            if (durMatcher.find()) {
                foundDuration = durMatcher.group(0)
            }

            // If line looks like a medicine name or previous med candidate is ready
            if (isMedLine || (foundStrength != null && currentMedName == null)) {
                // Clean medicine name
                var cleanName = line
                    .replace(Regex("""^(?:TAB|CAP|SYRUP|SYP|INJ|DROPS|RX|\d+[\.\)])\s*[\.:-]?\s*""", RegexOption.IGNORE_CASE), "")
                    .trim()

                if (foundStrength != null) {
                    cleanName = cleanName.replace(foundStrength, "").trim()
                }
                // Strip common trailing punctuation
                cleanName = cleanName.replace(Regex("""[-/:]+$"""), "").trim()

                if (cleanName.isNotBlank() && cleanName.length >= 3) {
                    // Save previous candidate if exists
                    if (currentMedName != null) {
                        candidates.add(
                            OcrCandidateEntity(
                                scanId = 0,
                                medicineName = currentMedName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() },
                                strength = currentStrength ?: "",
                                dose = currentDose,
                                doseUnit = currentUnit,
                                frequency = currentFrequency,
                                route = "Oral",
                                instructions = currentInstructions,
                                duration = currentDuration,
                                confidenceName = nameConfidence,
                                confidenceStrength = strengthConfidence,
                                confidenceFreq = freqConfidence,
                                confidenceDuration = durConfidence,
                                confirmed = false
                            )
                        )
                    }

                    // Reset for new item
                    currentMedName = cleanName
                    currentStrength = foundStrength
                    currentDose = 1.0
                    currentUnit = if (upper.startsWith("CAP")) DoseUnit.CAPSULE else if (upper.startsWith("SYP") || upper.startsWith("SYRUP")) DoseUnit.ML else DoseUnit.TABLET
                    currentFrequency = foundFrequency ?: FrequencyType.DAILY
                    currentInstructions = foundInstructions ?: "After food"
                    currentDuration = foundDuration ?: "7 days"

                    nameConfidence = if (cleanName.length in 4..25) ConfidenceLevel.HIGH else ConfidenceLevel.MEDIUM
                    strengthConfidence = if (foundStrength != null) ConfidenceLevel.HIGH else ConfidenceLevel.NOT_DETECTED
                    freqConfidence = if (foundFrequency != null) ConfidenceLevel.HIGH else ConfidenceLevel.MEDIUM
                    durConfidence = if (foundDuration != null) ConfidenceLevel.HIGH else ConfidenceLevel.LOW
                }
            } else {
                // Supplementary line modifying current medication
                if (foundStrength != null) {
                    currentStrength = foundStrength
                    strengthConfidence = ConfidenceLevel.HIGH
                }
                if (foundFrequency != null) {
                    currentFrequency = foundFrequency
                    freqConfidence = ConfidenceLevel.HIGH
                }
                if (foundInstructions != null) {
                    currentInstructions = foundInstructions
                }
                if (foundDuration != null) {
                    currentDuration = foundDuration
                    durConfidence = ConfidenceLevel.HIGH
                }
            }
        }

        // Add last item
        if (currentMedName != null) {
            candidates.add(
                OcrCandidateEntity(
                    scanId = 0,
                    medicineName = currentMedName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() },
                    strength = currentStrength ?: "",
                    dose = currentDose,
                    doseUnit = currentUnit,
                    frequency = currentFrequency,
                    route = "Oral",
                    instructions = currentInstructions,
                    duration = currentDuration,
                    confidenceName = nameConfidence,
                    confidenceStrength = strengthConfidence,
                    confidenceFreq = freqConfidence,
                    confidenceDuration = durConfidence,
                    confirmed = false
                )
            )
        }

        // If no candidate was structured, extract first decent line as candidate for manual editing
        if (candidates.isEmpty()) {
            val fallbackLine = lines.firstOrNull { it.length in 4..30 } ?: "Prescribed Medicine"
            candidates.add(
                OcrCandidateEntity(
                    scanId = 0,
                    medicineName = fallbackLine,
                    strength = "500 mg",
                    dose = 1.0,
                    doseUnit = DoseUnit.TABLET,
                    frequency = FrequencyType.DAILY,
                    route = "Oral",
                    instructions = "After food",
                    duration = "5 days",
                    confidenceName = ConfidenceLevel.LOW,
                    confidenceStrength = ConfidenceLevel.LOW,
                    confidenceFreq = ConfidenceLevel.LOW,
                    confidenceDuration = ConfidenceLevel.LOW,
                    confirmed = false
                )
            )
        }

        return ParsedPrescriptionResult(
            rawText = rawText,
            candidates = candidates,
            hasWarning = candidates.any { it.confidenceName == ConfidenceLevel.LOW || it.confidenceStrength == ConfidenceLevel.NOT_DETECTED },
            warningMessage = "OCR assists data entry but may make mistakes. Verify every field against the physical prescription."
        )
    }

    /**
     * Demo samples for quick testing on device/emulator
     */
    fun getSamplePrescriptions(): List<Pair<String, String>> {
        return listOf(
            "Standard Multi-Drug Rx" to """
                CITY GENERAL CLINIC
                Dr. Sarah Jenkins, MD - Reg #48291
                Date: 14 Aug 2026
                Patient: Alex Johnson (Age 42)
                
                Rx:
                1. TAB PARACETAMOL 500MG
                   1-0-1 AFTER FOOD x 5 DAYS
                2. TAB AMOXICILLIN 250MG
                   1-1-1 AFTER FOOD x 7 DAYS
                3. TAB PANTOPRAZOLE 40MG
                   1-0-0 BEFORE FOOD x 10 DAYS
                4. SYRUP COUGH-RELIEF 100ML
                   10ml TID AFTER FOOD x 5 DAYS
                   
                Doctor Signature: Dr. Jenkins
            """.trimIndent(),

            "Chronic Hypertension & Vitamin Rx" to """
                METRO CARDIOLOGY SPECIALISTS
                Date: 12 Aug 2026
                Rx:
                - TAB ATENOLOL 50MG
                  1-0-0 BEFORE FOOD FOR 30 DAYS
                - TAB ATORVASTATIN 10MG
                  0-0-1 AT BEDTIME FOR 30 DAYS
                - CAP VITAMIN D3 60000 IU
                  ONCE WEEKLY AFTER FOOD FOR 8 WEEKS
            """.trimIndent(),

            "Diabetes Care Rx" to """
                HEALTHCARE DIABETES CLINIC
                Rx:
                1. TAB METFORMIN 500MG
                   1-0-1 WITH MEALS FOR 30 DAYS
                2. TAB GLIMEPIRIDE 2MG
                   1-0-0 BEFORE BREAKFAST FOR 30 DAYS
            """.trimIndent()
        )
    }
}
