package com.example.data.model

import com.example.data.local.dao.MedicationWithSchedules
import com.example.data.local.entity.MedicationEntity
import com.example.data.local.entity.MedicationScheduleEntity

/**
 * Domain model representing a Dosage definition for a medication.
 */
data class Dosage(
    val amount: Double = 1.0,
    val unit: DoseUnit = DoseUnit.TABLET,
    val form: MedicineForm = MedicineForm.TABLET,
    val route: String = "Oral",
    val instructions: String = "After food"
) {
    val displayFormatted: String
        get() = "${if (amount % 1.0 == 0.0) amount.toInt().toString() else amount.toString()} ${unit.symbol}"
}

/**
 * Domain model representing Schedule and timing configuration for a medication.
 */
data class MedicineSchedule(
    val id: Long = 0,
    val frequency: FrequencyType = FrequencyType.DAILY,
    val timesOfDay: List<String> = listOf("08:00"), // e.g. ["08:00", "20:00"]
    val daysOfWeek: List<String> = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"),
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long? = null,
    val isEnabled: Boolean = true,
    val isReminderEnabled: Boolean = true,
    val snoozeDurationMinutes: Int = 10,
    val gracePeriodMinutes: Int = 60
) {
    val daysOfWeekString: String
        get() = daysOfWeek.joinToString(",")
}

/**
 * Primary Domain Model representing a complete Medicine record.
 */
data class Medicine(
    val id: Long = 0,
    val userId: String = "default_user",
    val name: String,
    val genericName: String = "",
    val brandName: String = "",
    val strength: String = "", // e.g., "500 mg", "10 ml"
    val dosage: Dosage = Dosage(),
    val schedule: MedicineSchedule = MedicineSchedule(),
    val stockQuantity: Double = 30.0,
    val lowStockThreshold: Double = 5.0,
    val isActive: Boolean = true,
    val source: String = "MANUAL", // "MANUAL" or "OCR"
    val notes: String = "",
    val colorHex: String = "#006874",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isLowStock: Boolean
        get() = stockQuantity <= lowStockThreshold

    val fullDisplayName: String
        get() = if (strength.isNotBlank()) "$name ($strength)" else name

    val subtitleInfo: String
        get() = listOfNotNull(
            dosage.displayFormatted,
            dosage.form.displayName,
            dosage.instructions.takeIf { it.isNotBlank() }
        ).joinToString(" • ")
}

// -----------------------------------------------------------------------------
// Mapping Extensions between Room Entities and Domain Models
// -----------------------------------------------------------------------------

fun MedicationEntity.toDomain(schedules: List<MedicationScheduleEntity> = emptyList()): Medicine {
    val primarySchedule = schedules.firstOrNull()
    val scheduleTimes = if (schedules.isNotEmpty()) {
        schedules.map { it.timeString }
    } else {
        listOf("08:00")
    }

    val daysList = primarySchedule?.daysOfWeek?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
        ?: listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")

    val scheduleDomain = MedicineSchedule(
        id = primarySchedule?.id ?: 0L,
        frequency = primarySchedule?.frequencyType ?: FrequencyType.DAILY,
        timesOfDay = scheduleTimes,
        daysOfWeek = daysList,
        startDate = primarySchedule?.startDate ?: startDate,
        endDate = primarySchedule?.endDate ?: endDate,
        isEnabled = primarySchedule?.enabled ?: true,
        isReminderEnabled = primarySchedule?.reminderEnabled ?: true,
        snoozeDurationMinutes = primarySchedule?.snoozeDurationMinutes ?: 10,
        gracePeriodMinutes = primarySchedule?.gracePeriodMinutes ?: 60
    )

    val dosageDomain = Dosage(
        amount = doseAmount,
        unit = doseUnit,
        form = form,
        route = route,
        instructions = instructions
    )

    return Medicine(
        id = id,
        userId = userId,
        name = name,
        genericName = genericName,
        brandName = brandName,
        strength = strength,
        dosage = dosageDomain,
        schedule = scheduleDomain,
        stockQuantity = stockQuantity,
        lowStockThreshold = lowStockThreshold,
        isActive = active,
        source = source,
        notes = notes,
        colorHex = colorHex,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun MedicationWithSchedules.toDomain(): Medicine {
    return medication.toDomain(schedules)
}

fun Medicine.toEntity(): MedicationEntity {
    return MedicationEntity(
        id = id,
        userId = userId,
        name = name,
        genericName = genericName,
        brandName = brandName,
        strength = strength,
        doseAmount = dosage.amount,
        doseUnit = dosage.unit,
        form = dosage.form,
        route = dosage.route,
        instructions = dosage.instructions,
        notes = notes,
        startDate = schedule.startDate,
        endDate = schedule.endDate,
        stockQuantity = stockQuantity,
        lowStockThreshold = lowStockThreshold,
        active = isActive,
        source = source,
        colorHex = colorHex,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Medicine.toScheduleEntities(): List<MedicationScheduleEntity> {
    val times = if (schedule.timesOfDay.isEmpty()) listOf("08:00") else schedule.timesOfDay
    val daysJoined = schedule.daysOfWeek.joinToString(",")

    return times.mapIndexed { index, timeStr ->
        MedicationScheduleEntity(
            id = if (index == 0 && schedule.id > 0) schedule.id else 0L,
            medicationId = id,
            userId = userId,
            frequencyType = schedule.frequency,
            timeString = timeStr,
            daysOfWeek = daysJoined,
            startDate = schedule.startDate,
            endDate = schedule.endDate,
            doseAmount = dosage.amount,
            enabled = schedule.isEnabled,
            reminderEnabled = schedule.isReminderEnabled,
            snoozeDurationMinutes = schedule.snoozeDurationMinutes,
            gracePeriodMinutes = schedule.gracePeriodMinutes
        )
    }
}
