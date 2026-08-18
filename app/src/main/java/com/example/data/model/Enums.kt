package com.example.data.model

enum class UserRole(val displayName: String) {
    PATIENT("Patient"),
    CAREGIVER("Caregiver")
}

enum class DoseStatus(val displayName: String) {
    SCHEDULED("Scheduled"),
    REMINDER_SENT("Reminder Sent"),
    PENDING("Due / Pending"),
    SNOOZED("Snoozed"),
    TAKEN("Taken"),
    SKIPPED("Skipped"),
    MISSED("Missed")
}

enum class AttentionState(val displayName: String) {
    ON_TRACK("On Track"),        // 🟢
    PENDING("Pending"),          // 🟡
    NEEDS_ATTENTION("Attention"),// 🟠
    ESCALATION("Escalation")     // 🔴
}

enum class FrequencyType(val displayName: String) {
    ONCE("Once only"),
    DAILY("Once daily"),
    TWICE_DAILY("Twice daily (12h)"),
    THREE_TIMES_DAILY("Three times daily (8h)"),
    FOUR_TIMES_DAILY("Four times daily (6h)"),
    SPECIFIC_DAYS("Specific days of week"),
    AS_NEEDED("As needed (PRN)")
}

enum class MedicineForm(val displayName: String) {
    TABLET("Tablet"),
    CAPSULE("Capsule"),
    LIQUID("Liquid / Syrup"),
    INJECTION("Injection"),
    INHALER("Inhaler"),
    DROPS("Drops"),
    CREAM("Cream / Ointment"),
    PATCH("Patch"),
    OTHER("Other")
}

enum class DoseUnit(val symbol: String, val displayName: String = symbol) {
    TABLET("tab", "Tablets"),
    CAPSULE("cap", "Capsules"),
    MG("mg", "Milligrams (mg)"),
    ML("ml", "Milliliters (ml)"),
    DROPS("drops", "Drops"),
    PUFFS("puffs", "Puffs"),
    SACHET("sachet", "Sachets"),
    UNITS("units", "Units")
}

enum class ConfidenceLevel(val displayName: String) {
    HIGH("High"),
    MEDIUM("Medium"),
    LOW("Low"),
    NOT_DETECTED("Not Detected")
}

enum class AlertChannel(val displayName: String) {
    LOCAL_NOTIFICATION("Local Notification"),
    PATIENT_MISSED_ALERT("Patient Missed Alert"),
    CAREGIVER_ALERT("Caregiver Alert"),
    REPEATED_MISSED_ALERT("Multiple Missed Escalation"),
    EMAIL("Email"),
    WHATSAPP("WhatsApp")
}

enum class StockTransactionType(val displayName: String) {
    DOSE_TAKEN("Dose Taken"),
    MANUAL_ADJUSTMENT("Manual Adjustment"),
    REFILL("Refill"),
    INITIAL_STOCK("Initial Stock")
}

enum class PrescriptionStatus(val displayName: String) {
    DRAFT("Draft"),
    UNDER_REVIEW("Under Review"),
    CONFIRMED("Confirmed"),
    SUPERSEDED("Superseded"),
    ARCHIVED("Archived")
}

enum class MedicationChangeType(val displayName: String) {
    NEW("New Medicine"),
    UNCHANGED("Unchanged"),
    CHANGED("Change Detected"),
    NOT_FOUND("Not Found in New Rx"),
    POSSIBLE_DUPLICATE("Possible Duplicate"),
    UNCERTAIN("Uncertain Match")
}

enum class DetailedChangeType(val displayName: String) {
    STRENGTH_CHANGED("Strength Changed"),
    DOSE_CHANGED("Dose Amount Changed"),
    FREQUENCY_CHANGED("Frequency Changed"),
    TIMING_CHANGED("Timing / Instructions Changed"),
    DURATION_CHANGED("Duration Changed"),
    ROUTE_CHANGED("Route Changed"),
    FORM_CHANGED("Form Changed")
}

enum class ChangeReviewStatus(val displayName: String) {
    PENDING("Pending Review"),
    ACCEPTED("Accepted"),
    REJECTED("Rejected"),
    KEPT("Kept Previous"),
    EDITED("Edited & Approved"),
    DEFERRED("Reviewed Later")
}

enum class MedicationStatus(val displayName: String) {
    ACTIVE("Active"),
    PAUSED("Paused"),
    DISCONTINUED("Discontinued"),
    ARCHIVED("Archived")
}

