package com.example.data.reconciliation

import com.example.data.local.dao.MedicationWithSchedules
import com.example.data.local.entity.MedicationEntity
import com.example.data.local.entity.MedicationScheduleEntity
import com.example.data.local.entity.OcrCandidateEntity
import com.example.data.model.ConfidenceLevel
import com.example.data.model.DetailedChangeType
import com.example.data.model.DoseUnit
import com.example.data.model.FrequencyType
import com.example.data.model.MedicationChangeType
import com.example.data.model.MedicineForm
import java.util.Locale

object MedicationReconciliationService {

    /**
     * Compares the user's active medication plan with newly extracted or entered prescription candidates.
     */
    fun compare(
        activeMedications: List<MedicationWithSchedules>,
        incomingCandidates: List<OcrCandidateEntity>,
        prescriptionId: Long = 0L,
        doctorName: String = "",
        clinicName: String = "",
        prescriptionDate: Long = System.currentTimeMillis()
    ): ReconciliationResult {
        val matchedExistingIds = mutableSetOf<Long>()
        val items = mutableListOf<ReconciliationItem>()

        for (candidate in incomingCandidates) {
            val normCandidateName = normalizeName(candidate.medicineName)
            if (normCandidateName.isBlank()) continue

            // Find best matching active medication
            val match = findBestMatch(candidate, activeMedications, matchedExistingIds)

            if (match != null) {
                val (matchedMedWithSched, confidence) = match
                matchedExistingIds.add(matchedMedWithSched.medication.id)

                val existing = matchedMedWithSched.medication
                val existingSchedules = matchedMedWithSched.schedules

                if (confidence == ConfidenceLevel.LOW) {
                    // Uncertain match requiring user verification
                    items.add(
                        ReconciliationItem(
                            category = MedicationChangeType.UNCERTAIN,
                            existingMedication = existing,
                            existingSchedules = existingSchedules,
                            proposedCandidate = candidate,
                            medicineName = candidate.medicineName.ifBlank { existing.name },
                            genericName = existing.genericName,
                            brandName = existing.brandName,
                            strengthDiff = DiffField(existing.strength, candidate.strength),
                            doseDiff = DiffField("${existing.doseAmount} ${existing.doseUnit.symbol}", "${candidate.dose} ${candidate.doseUnit.symbol}"),
                            formDiff = DiffField(existing.form.displayName, candidate.doseUnit.symbol),
                            frequencyDiff = DiffField(existingSchedules.firstOrNull()?.frequencyType?.displayName ?: "Daily", candidate.frequency.displayName),
                            instructionsDiff = DiffField(existing.instructions, candidate.instructions),
                            confidence = ConfidenceLevel.LOW
                        )
                    )
                } else {
                    // Compare fields
                    val detailedChanges = mutableListOf<DetailedChangeType>()

                    val strengthChanged = isStrengthDifferent(existing.strength, candidate.strength)
                    if (strengthChanged) detailedChanges.add(DetailedChangeType.STRENGTH_CHANGED)

                    val doseChanged = existing.doseAmount != candidate.dose || existing.doseUnit != candidate.doseUnit
                    if (doseChanged) detailedChanges.add(DetailedChangeType.DOSE_CHANGED)

                    val existingFreq = existingSchedules.firstOrNull()?.frequencyType ?: FrequencyType.DAILY
                    val freqChanged = existingFreq != candidate.frequency
                    if (freqChanged) detailedChanges.add(DetailedChangeType.FREQUENCY_CHANGED)

                    val instructionsChanged = normalizeInstructions(existing.instructions) != normalizeInstructions(candidate.instructions) && candidate.instructions.isNotBlank()
                    if (instructionsChanged) detailedChanges.add(DetailedChangeType.TIMING_CHANGED)

                    val routeChanged = normalizeText(existing.route) != normalizeText(candidate.route) && candidate.route.isNotBlank()
                    if (routeChanged) detailedChanges.add(DetailedChangeType.ROUTE_CHANGED)

                    val category = if (detailedChanges.isNotEmpty()) {
                        MedicationChangeType.CHANGED
                    } else {
                        MedicationChangeType.UNCHANGED
                    }

                    items.add(
                        ReconciliationItem(
                            category = category,
                            detailedChanges = detailedChanges,
                            existingMedication = existing,
                            existingSchedules = existingSchedules,
                            proposedCandidate = candidate,
                            medicineName = candidate.medicineName.ifBlank { existing.name },
                            genericName = existing.genericName,
                            brandName = existing.brandName,
                            strengthDiff = DiffField(existing.strength, candidate.strength, strengthChanged),
                            doseDiff = DiffField("${existing.doseAmount} ${existing.doseUnit.symbol}", "${candidate.dose} ${candidate.doseUnit.symbol}", doseChanged),
                            formDiff = DiffField(existing.form.displayName, candidate.doseUnit.symbol, false),
                            frequencyDiff = DiffField(existingFreq.displayName, candidate.frequency.displayName, freqChanged),
                            instructionsDiff = DiffField(existing.instructions, candidate.instructions, instructionsChanged),
                            durationDiff = DiffField("-", candidate.duration, false),
                            confidence = confidence
                        )
                    )
                }
            } else {
                // New medication not found in current plan
                items.add(
                    ReconciliationItem(
                        category = MedicationChangeType.NEW,
                        proposedCandidate = candidate,
                        medicineName = candidate.medicineName,
                        strengthDiff = DiffField(null, candidate.strength),
                        doseDiff = DiffField(null, "${candidate.dose} ${candidate.doseUnit.symbol}"),
                        formDiff = DiffField(null, candidate.doseUnit.symbol),
                        frequencyDiff = DiffField(null, candidate.frequency.displayName),
                        instructionsDiff = DiffField(null, candidate.instructions),
                        durationDiff = DiffField(null, candidate.duration),
                        confidence = ConfidenceLevel.HIGH
                    )
                )
            }
        }

        // CRITICAL RULE: Absence is not discontinuation.
        // Check for active medicines in the patient's plan that were NOT listed on the new prescription.
        for (medWithSched in activeMedications) {
            if (!matchedExistingIds.contains(medWithSched.medication.id) && medWithSched.medication.active) {
                val existing = medWithSched.medication
                val sched = medWithSched.schedules.firstOrNull()

                items.add(
                    ReconciliationItem(
                        category = MedicationChangeType.NOT_FOUND,
                        existingMedication = existing,
                        existingSchedules = medWithSched.schedules,
                        medicineName = existing.name,
                        genericName = existing.genericName,
                        brandName = existing.brandName,
                        strengthDiff = DiffField(existing.strength, "Not in new Rx"),
                        doseDiff = DiffField("${existing.doseAmount} ${existing.doseUnit.symbol}", "Not listed"),
                        frequencyDiff = DiffField(sched?.frequencyType?.displayName ?: "Daily", "-"),
                        instructionsDiff = DiffField(existing.instructions, "-"),
                        confidence = ConfidenceLevel.HIGH
                    )
                )
            }
        }

        return ReconciliationResult(
            prescriptionId = prescriptionId,
            doctorName = doctorName,
            clinicName = clinicName,
            prescriptionDate = prescriptionDate,
            items = items
        )
    }

    // -------------------------------------------------------------------------
    // Matching & Normalization Engine
    // -------------------------------------------------------------------------

    private fun findBestMatch(
        candidate: OcrCandidateEntity,
        activeMedications: List<MedicationWithSchedules>,
        alreadyMatchedIds: Set<Long>
    ): Pair<MedicationWithSchedules, ConfidenceLevel>? {
        val candNorm = normalizeName(candidate.medicineName)
        if (candNorm.isBlank()) return null

        val available = activeMedications.filter { !alreadyMatchedIds.contains(it.medication.id) && it.medication.active }

        // 1. Exact Match
        for (med in available) {
            val existNorm = normalizeName(med.medication.name)
            if (candNorm == existNorm) {
                return Pair(med, ConfidenceLevel.HIGH)
            }
        }

        // 2. Generic Name or Brand Name Match
        for (med in available) {
            val genNorm = normalizeName(med.medication.genericName)
            val brandNorm = normalizeName(med.medication.brandName)
            if (genNorm.isNotBlank() && (candNorm == genNorm || candNorm.contains(genNorm) || genNorm.contains(candNorm))) {
                return Pair(med, ConfidenceLevel.HIGH)
            }
            if (brandNorm.isNotBlank() && (candNorm == brandNorm || candNorm.contains(brandNorm) || brandNorm.contains(candNorm))) {
                return Pair(med, ConfidenceLevel.HIGH)
            }
        }

        // 3. Substring / Prefix Match (e.g. "Metformin" in "Metformin HCl 500mg" or "Atorvastatin Calcium")
        for (med in available) {
            val existNorm = normalizeName(med.medication.name)
            val cleanExist = removeDosageFromText(existNorm)
            val cleanCand = removeDosageFromText(candNorm)

            if (cleanExist.isNotBlank() && cleanCand.isNotBlank()) {
                if (cleanCand == cleanExist) {
                    return Pair(med, ConfidenceLevel.HIGH)
                }
                if (cleanCand.startsWith(cleanExist) || cleanExist.startsWith(cleanCand)) {
                    val minLen = minOf(cleanCand.length, cleanExist.length)
                    if (minLen >= 4) {
                        return Pair(med, ConfidenceLevel.MEDIUM)
                    }
                }
            }
        }

        // 4. Token Overlap (e.g. "Amlo 5" vs "Amlodipine 5mg" or common 3-letter prefix)
        for (med in available) {
            val existNorm = removeDosageFromText(normalizeName(med.medication.name))
            val cleanCand = removeDosageFromText(candNorm)
            if (cleanCand.length >= 3 && existNorm.length >= 3) {
                if (existNorm.startsWith(cleanCand.take(4)) || cleanCand.startsWith(existNorm.take(4))) {
                    return Pair(med, ConfidenceLevel.LOW) // Low confidence -> mark UNCERTAIN
                }
            }
        }

        return null
    }

    fun normalizeName(raw: String): String {
        return raw.trim()
            .lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun normalizeText(raw: String): String {
        return raw.trim().lowercase(Locale.ROOT)
    }

    private fun normalizeInstructions(raw: String): String {
        val cleaned = normalizeText(raw)
        return when {
            cleaned.contains("after") -> "after food"
            cleaned.contains("before") -> "before food"
            cleaned.contains("with") -> "with food"
            cleaned.contains("empty") -> "empty stomach"
            cleaned.contains("bedtime") || cleaned.contains("night") -> "at bedtime"
            else -> cleaned
        }
    }

    private fun removeDosageFromText(text: String): String {
        return text.replace(Regex("\\d+\\s*(mg|g|ml|mcg|tab|tablets?|caps?|capsules?|drops?)"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun isStrengthDifferent(oldStr: String, newStr: String): Boolean {
        val normOld = normalizeStrength(oldStr)
        val normNew = normalizeStrength(newStr)
        if (normOld.isBlank() || normNew.isBlank()) return false
        return normOld != normNew
    }

    fun normalizeStrength(raw: String): String {
        val clean = raw.trim().lowercase(Locale.ROOT).replace(" ", "")
        // Example: "500mg" vs "500 mg" -> "500mg"
        return clean
    }
}
