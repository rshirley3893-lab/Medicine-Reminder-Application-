package com.example.data.reconciliation

import com.example.data.local.entity.MedicationEntity
import com.example.data.local.entity.MedicationScheduleEntity
import com.example.data.local.entity.OcrCandidateEntity
import com.example.data.model.ChangeReviewStatus
import com.example.data.model.ConfidenceLevel
import com.example.data.model.DetailedChangeType
import com.example.data.model.DoseUnit
import com.example.data.model.FrequencyType
import com.example.data.model.MedicationChangeType
import com.example.data.model.MedicineForm
import java.util.UUID

/**
 * Encapsulates before-and-after values for an individual medication field.
 */
data class DiffField<T>(
    val oldValue: T?,
    val newValue: T?,
    val isChanged: Boolean = oldValue != newValue && oldValue != null && newValue != null
)

/**
 * Represents a single reconciled medication candidate compared against the patient's current plan.
 */
data class ReconciliationItem(
    val id: String = UUID.randomUUID().toString(),
    val category: MedicationChangeType,
    val detailedChanges: List<DetailedChangeType> = emptyList(),
    val existingMedication: MedicationEntity? = null,
    val existingSchedules: List<MedicationScheduleEntity> = emptyList(),
    val proposedCandidate: OcrCandidateEntity? = null,
    val medicineName: String,
    val genericName: String = "",
    val brandName: String = "",
    val strengthDiff: DiffField<String> = DiffField(null, null),
    val doseDiff: DiffField<String> = DiffField(null, null),
    val formDiff: DiffField<String> = DiffField(null, null),
    val frequencyDiff: DiffField<String> = DiffField(null, null),
    val routeDiff: DiffField<String> = DiffField(null, null),
    val instructionsDiff: DiffField<String> = DiffField(null, null),
    val durationDiff: DiffField<String> = DiffField(null, null),
    val confidence: ConfidenceLevel = ConfidenceLevel.HIGH,
    val reviewDecision: ChangeReviewStatus = when (category) {
        MedicationChangeType.UNCHANGED -> ChangeReviewStatus.ACCEPTED
        MedicationChangeType.NEW -> ChangeReviewStatus.ACCEPTED
        MedicationChangeType.CHANGED -> ChangeReviewStatus.ACCEPTED
        MedicationChangeType.NOT_FOUND -> ChangeReviewStatus.KEPT // Default: Keep active (Absence is not discontinuation)
        MedicationChangeType.UNCERTAIN -> ChangeReviewStatus.PENDING
        MedicationChangeType.POSSIBLE_DUPLICATE -> ChangeReviewStatus.PENDING
    },
    val markDiscontinued: Boolean = false,
    // Custom user edits when user clicks "Edit" on a candidate
    val customMedicineName: String? = null,
    val customStrength: String? = null,
    val customDoseAmount: Double? = null,
    val customDoseUnit: DoseUnit? = null,
    val customForm: MedicineForm? = null,
    val customFrequency: FrequencyType? = null,
    val customInstructions: String? = null
) {
    val displayMedicineName: String
        get() = customMedicineName ?: medicineName

    val displayStrength: String
        get() = customStrength ?: proposedCandidate?.strength ?: existingMedication?.strength ?: ""

    val displayDoseAmount: Double
        get() = customDoseAmount ?: proposedCandidate?.dose ?: existingMedication?.doseAmount ?: 1.0

    val displayDoseUnit: DoseUnit
        get() = customDoseUnit ?: proposedCandidate?.doseUnit ?: existingMedication?.doseUnit ?: DoseUnit.TABLET

    val displayForm: MedicineForm
        get() = customForm ?: proposedCandidate?.doseUnit?.let { unitToForm(it) } ?: existingMedication?.form ?: MedicineForm.TABLET

    val displayFrequency: FrequencyType
        get() = customFrequency ?: proposedCandidate?.frequency ?: existingSchedules.firstOrNull()?.frequencyType ?: FrequencyType.DAILY

    val displayInstructions: String
        get() = customInstructions ?: proposedCandidate?.instructions ?: existingMedication?.instructions ?: "After food"

    private fun unitToForm(unit: DoseUnit): MedicineForm = when (unit) {
        DoseUnit.TABLET -> MedicineForm.TABLET
        DoseUnit.CAPSULE -> MedicineForm.CAPSULE
        DoseUnit.ML -> MedicineForm.LIQUID
        DoseUnit.DROPS -> MedicineForm.DROPS
        DoseUnit.PUFFS -> MedicineForm.INHALER
        DoseUnit.SACHET -> MedicineForm.OTHER
        DoseUnit.UNITS -> MedicineForm.INJECTION
        else -> MedicineForm.TABLET
    }
}

/**
 * Result structure aggregating all items from a reconciliation analysis.
 */
data class ReconciliationResult(
    val prescriptionId: Long = 0L,
    val doctorName: String = "",
    val clinicName: String = "",
    val prescriptionDate: Long = System.currentTimeMillis(),
    val items: List<ReconciliationItem> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
) {
    val newItems: List<ReconciliationItem>
        get() = items.filter { it.category == MedicationChangeType.NEW }

    val changedItems: List<ReconciliationItem>
        get() = items.filter { it.category == MedicationChangeType.CHANGED }

    val unchangedItems: List<ReconciliationItem>
        get() = items.filter { it.category == MedicationChangeType.UNCHANGED }

    val notFoundItems: List<ReconciliationItem>
        get() = items.filter { it.category == MedicationChangeType.NOT_FOUND }

    val uncertainItems: List<ReconciliationItem>
        get() = items.filter { it.category == MedicationChangeType.UNCERTAIN || it.category == MedicationChangeType.POSSIBLE_DUPLICATE }

    val totalChangesCount: Int
        get() = newItems.size + changedItems.size + notFoundItems.size

    val pendingReviewCount: Int
        get() = items.count { it.reviewDecision == ChangeReviewStatus.PENDING }

    val isAllReviewed: Boolean
        get() = items.none { it.reviewDecision == ChangeReviewStatus.PENDING }
}
