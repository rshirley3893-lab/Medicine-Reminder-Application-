package com.example.data.model

import com.example.data.local.entity.EmergencyAllergyEntity
import com.example.data.local.entity.EmergencyContactEntity

data class EmergencyMedicationItem(
    val id: Long = 0L,
    val name: String,
    val genericName: String = "",
    val strength: String,
    val form: String,
    val route: String = "Oral",
    val instructions: String = "",
    val frequency: String = "",
    val doseAmount: Double = 1.0,
    val doseUnit: String = "tab"
)

data class EmergencySnapshot(
    val patientName: String = "",
    val preferredName: String = "",
    val age: String = "",
    val dob: String = "",
    val gender: String = "",
    val bloodGroup: String = "Unknown",
    val medicalConditions: List<String> = emptyList(),
    val allergies: List<EmergencyAllergyEntity> = emptyList(),
    val currentMedications: List<EmergencyMedicationItem> = emptyList(),
    val emergencyContacts: List<EmergencyContactEntity> = emptyList(),
    val primaryDoctorName: String = "",
    val primaryDoctorPhone: String = "",
    val hospitalClinicName: String = "",
    val importantNotes: String = "",
    val organDonor: Boolean = false,
    val emergencyIdentifier: String = "",
    val qrEnabled: Boolean = false,
    val isEnabled: Boolean = true,
    val lastUpdated: Long = System.currentTimeMillis(),
    val lastReviewedAt: Long = System.currentTimeMillis(),
    val qrCreatedAt: Long? = null,
    val qrRevokedAt: Long? = null,
    val updatedBy: String = "Patient"
) {
    // Completeness check indicators
    val hasName: Boolean get() = patientName.isNotBlank()
    val hasMedications: Boolean get() = currentMedications.isNotEmpty()
    val hasEmergencyContacts: Boolean get() = emergencyContacts.isNotEmpty()
    val hasAllergies: Boolean get() = allergies.isNotEmpty()
    val hasConditions: Boolean get() = medicalConditions.isNotEmpty()
    val hasBloodGroup: Boolean get() = bloodGroup.isNotBlank() && bloodGroup != "Unknown"
    val hasDoctorContact: Boolean get() = primaryDoctorPhone.isNotBlank() || primaryDoctorName.isNotBlank()
    
    // Freshness check: if last reviewed > 90 days ago (or > 60 days)
    val isFreshnessWarning: Boolean get() {
        val daysSinceReview = (System.currentTimeMillis() - lastReviewedAt) / (1000 * 60 * 60 * 24)
        return daysSinceReview >= 60
    }

    val completedItemsCount: Int get() {
        var count = 0
        if (hasName) count++
        if (hasMedications) count++
        if (hasEmergencyContacts) count++
        if (hasAllergies) count++
        if (hasConditions) count++
        if (hasBloodGroup) count++
        if (hasDoctorContact) count++
        return count
    }

    val totalItemsCount: Int get() = 7
}

object EmergencyConstants {
    val BLOOD_GROUPS = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-", "Unknown")
    
    val COMMON_CONDITIONS = listOf(
        "Type 2 Diabetes",
        "Hypertension",
        "Asthma",
        "Epilepsy / Seizure Disorder",
        "Coronary Artery Disease",
        "Chronic Kidney Disease (CKD)",
        "Hypothyroidism",
        "COPD",
        "Heart Failure",
        "Stroke History"
    )

    val COMMON_ALLERGIES = listOf(
        "Penicillin",
        "Sulfa Drugs (Sulfonamides)",
        "Aspirin / NSAIDs",
        "Cephalosporins",
        "Latex",
        "Peanuts",
        "Shellfish",
        "Contrast Dye",
        "Local Anesthetics",
        "Codeine / Opioids"
    )

    val SEVERITY_LEVELS = listOf("Severe (Anaphylaxis Risk)", "Moderate", "Mild")

    val DEFAULT_EMERGENCY_NUMBER = "112" // Standard National Emergency Number
}
