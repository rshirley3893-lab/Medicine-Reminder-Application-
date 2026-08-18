package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.EmergencyAllergyItem
import com.example.data.model.EmergencyContactItem
import com.example.data.model.EmergencyMedicationItem
import com.example.data.model.EmergencySnapshot
import com.example.data.repository.MedicineRepository
import com.example.util.QrCodeGenerator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class EmergencyMedicalIdTest {

    private lateinit var context: Context
    private lateinit var repository: MedicineRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        repository = MedicineRepository(context)
    }

    @Test
    fun testQrCodeGeneratorProducesBitmap() {
        val testPayload = "MEDICINE_APP_EMERGENCY:UUID-12345"
        val bitmap = QrCodeGenerator.encodeToBitmap(testPayload, 256, 256)
        assertNotNull(bitmap)
        assertEquals(256, bitmap.width)
        assertEquals(256, bitmap.height)
    }

    @Test
    fun testEmergencySnapshotIntegrity() = runBlocking {
        val snapshot = EmergencySnapshot(
            patientName = "Alex Rivera",
            preferredName = "Alex",
            age = "34",
            dob = "1990-05-12",
            gender = "Male",
            bloodGroup = "O+",
            medicalConditions = listOf("Asthma", "Hypertension"),
            allergies = listOf(
                EmergencyAllergyItem(allergen = "Penicillin", reaction = "Anaphylaxis", severity = "Severe")
            ),
            currentMedications = listOf(
                EmergencyMedicationItem(
                    id = 1L,
                    name = "Albuterol Inhaler",
                    strength = "90 mcg",
                    form = "Inhaler",
                    route = "Inhalation",
                    instructions = "2 puffs as needed",
                    frequency = "As needed",
                    doseAmount = 2.0,
                    doseUnit = "puffs"
                )
            ),
            emergencyContacts = listOf(
                EmergencyContactItem(name = "Sarah Rivera", relationship = "Spouse", phone = "+1 (555) 234-5678", isPrimary = true)
            ),
            primaryDoctorName = "Dr. Robert Chen",
            primaryDoctorPhone = "+1 (555) 987-6543",
            hospitalClinicName = "Metro Health Medical Center",
            importantNotes = "Carries EpiPen in backpack",
            organDonor = true,
            emergencyIdentifier = "EMERGENCY-TEST-ID",
            qrEnabled = true,
            isEnabled = true
        )

        assertEquals("Alex Rivera", snapshot.patientName)
        assertEquals("O+", snapshot.bloodGroup)
        assertTrue(snapshot.organDonor)
        assertEquals(1, snapshot.allergies.size)
        assertEquals("Penicillin", snapshot.allergies.first().allergen)
        assertEquals(1, snapshot.currentMedications.size)
        assertEquals("Albuterol Inhaler", snapshot.currentMedications.first().name)
        assertEquals(1, snapshot.emergencyContacts.size)
        assertEquals("Sarah Rivera", snapshot.emergencyContacts.first().name)
    }

    @Test
    fun testRepositoryEmergencyFlows() = runBlocking {
        // Add condition
        val condId = repository.addEmergencyCondition("Type 1 Diabetes", "Managed with insulin", "2015")
        assertTrue(condId > 0)

        // Add allergy
        val allergyId = repository.addEmergencyAllergy("Peanuts", "Swelling and hives", "Severe")
        assertTrue(allergyId > 0)

        // Add contact
        val contactId = repository.addEmergencyContact(
            name = "Jane Doe",
            relationship = "Sister",
            phone = "+1 (555) 345-6789",
            email = "jane@example.com",
            priority = 1,
            isPrimary = true
        )
        assertTrue(contactId > 0)

        val conditions = repository.emergencyConditionsFlow.first()
        assertTrue(conditions.any { it.name == "Type 1 Diabetes" })

        val allergies = repository.emergencyAllergiesFlow.first()
        assertTrue(allergies.any { it.allergen == "Peanuts" })

        val contacts = repository.emergencyContactsFlow.first()
        assertTrue(contacts.any { it.name == "Jane Doe" })
    }
}
