package com.example.ai

import com.example.BuildConfig
import com.example.data.local.dao.MedicationWithSchedules
import com.example.data.local.entity.UserProfileEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body requestBody: okhttp3.RequestBody
    ): ResponseBody
}

object AiAdviceService {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .build()

    private val service = retrofit.create(GeminiApiService::class.java)

    suspend fun askAssistant(
        userQuery: String,
        userProfile: UserProfileEntity?,
        medications: List<MedicationWithSchedules>,
        adherencePct: Double,
        lowStockCount: Int
    ): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        val patientName = userProfile?.name ?: "Patient"
        val role = userProfile?.role?.name ?: "PATIENT"

        val medSummary = medications.joinToString("; ") { item ->
            val med = item.medication
            val times = item.schedules.joinToString(",") { it.timeString }
            "${med.name} ${med.strength} (${med.form.displayName}, ${med.instructions}, scheduled at: $times, stock: ${med.stockQuantity.toInt()})"
        }

        val systemPrompt = """
            You are a helpful, clear, and safety-conscious Medicine Reminder AI Assistant for $patientName (Mode: $role).
            Patient Medication Context:
            - Active medicines: ${if (medSummary.isNotBlank()) medSummary else "None added yet"}
            - Current adherence rate: ${String.format(java.util.Locale.US, "%.1f", adherencePct)}%
            - Low stock medicines: $lowStockCount
            
            Strict Safety Boundaries:
            1. You provide informational explanations (e.g. what 'after food' generally means, daily routine organization, stock reminders, general wellness tips).
            2. NEVER prescribe drugs, alter doses, diagnose illnesses, or suggest stopping prescribed medicines.
            3. Always remind the user to consult their doctor or pharmacist for medical decisions, adverse effects, or dosage modifications.
            4. Keep responses concise, supportive, and formatted in clean markdown bullet points.
        """.trimIndent()

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            // Intelligent local fallback response when offline or key not supplied
            return@withContext generateLocalAdvice(userQuery, medications, adherencePct, lowStockCount)
        }

        try {
            val jsonRequest = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    put(JSONObject().apply {
                        val partsArray = JSONArray().apply {
                            put(JSONObject().put("text", userQuery))
                        }
                        put("parts", partsArray)
                    })
                }
                put("contents", contentsArray)

                val systemInstructionObj = JSONObject().apply {
                    val partsArray = JSONArray().apply {
                        put(JSONObject().put("text", systemPrompt))
                    }
                    put("parts", partsArray)
                }
                put("systemInstruction", systemInstructionObj)
            }

            val requestBody = jsonRequest.toString().toRequestBody("application/json".toMediaType())
            val responseBody = service.generateContent(apiKey, requestBody)
            val responseString = responseBody.string()

            val jsonResponse = JSONObject(responseString)
            val candidates = jsonResponse.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text")

            text?.trim() ?: "No response generated. Please try again."
        } catch (e: Exception) {
            // Graceful fallback to local context-aware advice
            generateLocalAdvice(userQuery, medications, adherencePct, lowStockCount)
        }
    }

    private fun generateLocalAdvice(
        query: String,
        medications: List<MedicationWithSchedules>,
        adherencePct: Double,
        lowStockCount: Int
    ): String {
        val q = query.lowercase(java.util.Locale.ROOT)
        return when {
            q.contains("stock") || q.contains("refill") -> {
                if (lowStockCount > 0) {
                    "⚠️ You have $lowStockCount medication(s) currently low in stock. Please check the Stock Alerts tab and contact your pharmacy for refills."
                } else {
                    "✅ All your active medication stocks are currently above their warning thresholds."
                }
            }
            q.contains("adherence") || q.contains("compliance") || q.contains("missed") -> {
                "📊 Your current adherence score is **${String.format(java.util.Locale.US, "%.1f", adherencePct)}%**.\n\nTip: To maintain higher adherence, keep alarms enabled and use the 15-minute Snooze button if you're temporarily busy."
            }
            q.contains("food") || q.contains("meal") || q.contains("empty stomach") -> {
                "🍽️ **Taking Medicines with Meals**:\n- **After food**: Take within 15-30 minutes after eating to protect the stomach lining and reduce nausea.\n- **Before food / Empty stomach**: Take at least 30-60 minutes before eating or 2 hours after with a full glass of water for maximum absorption."
            }
            q.contains("today") || q.contains("schedule") || q.contains("what") -> {
                val medNames = medications.map { it.medication.name }.joinToString(", ")
                "📅 You currently have **${medications.size} active medications**: $medNames.\nCheck the Schedule tab to view your complete time-ordered daily routine."
            }
            else -> {
                "💡 **Medication Routine Guidance**:\n- Take your doses at the same time each day to maintain consistent levels in your bloodstream.\n- Never take a double dose to make up for a missed one.\n- Consult your prescribing physician or pharmacist before changing any schedule."
            }
        }
    }
}
