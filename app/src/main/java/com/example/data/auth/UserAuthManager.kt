package com.example.data.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_auth_preferences")

data class UserAuthState(
    val isLoggedIn: Boolean = false,
    val userId: String? = null,
    val email: String? = null,
    val name: String? = null,
    val role: UserRole = UserRole.PATIENT,
    val onboardingCompleted: Boolean = false
) {
    val canAccessDashboard: Boolean
        get() = isLoggedIn && onboardingCompleted && !userId.isNullOrBlank()
}

class UserAuthManager(private val context: Context) {

    companion object {
        val KEY_IS_LOGGED_IN = booleanPreferencesKey("auth_is_logged_in")
        val KEY_USER_ID = stringPreferencesKey("auth_user_id")
        val KEY_USER_EMAIL = stringPreferencesKey("auth_user_email")
        val KEY_USER_NAME = stringPreferencesKey("auth_user_name")
        val KEY_USER_ROLE = stringPreferencesKey("auth_user_role")
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("auth_onboarding_completed")

        @Volatile
        private var INSTANCE: UserAuthManager? = null

        fun getInstance(context: Context): UserAuthManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserAuthManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    val authState: Flow<UserAuthState> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val isLoggedIn = preferences[KEY_IS_LOGGED_IN] ?: false
            val userId = preferences[KEY_USER_ID]
            val email = preferences[KEY_USER_EMAIL]
            val name = preferences[KEY_USER_NAME]
            val roleStr = preferences[KEY_USER_ROLE] ?: UserRole.PATIENT.name
            val role = try {
                UserRole.valueOf(roleStr)
            } catch (e: Exception) {
                UserRole.PATIENT
            }
            val onboardingCompleted = preferences[KEY_ONBOARDING_COMPLETED] ?: false

            UserAuthState(
                isLoggedIn = isLoggedIn && !userId.isNullOrBlank(),
                userId = userId,
                email = email,
                name = name,
                role = role,
                onboardingCompleted = onboardingCompleted
            )
        }
        .distinctUntilChanged()

    val currentUserId: Flow<String?> = authState.map { it.userId }.distinctUntilChanged()

    val isOnboardingCompleted: Flow<Boolean> = authState.map { it.onboardingCompleted }.distinctUntilChanged()

    val isLoggedIn: Flow<Boolean> = authState.map { it.isLoggedIn }.distinctUntilChanged()

    val canAccessDashboard: Flow<Boolean> = authState.map { it.canAccessDashboard }.distinctUntilChanged()

    suspend fun getAuthStateDirect(): UserAuthState = authState.first()

    suspend fun setAuthenticatedUser(
        userId: String,
        email: String,
        name: String,
        role: UserRole,
        onboardingCompleted: Boolean
    ) {
        context.dataStore.edit { preferences ->
            preferences[KEY_IS_LOGGED_IN] = true
            preferences[KEY_USER_ID] = userId
            preferences[KEY_USER_EMAIL] = email
            preferences[KEY_USER_NAME] = name
            preferences[KEY_USER_ROLE] = role.name
            preferences[KEY_ONBOARDING_COMPLETED] = onboardingCompleted
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean, role: UserRole? = null) {
        context.dataStore.edit { preferences ->
            preferences[KEY_ONBOARDING_COMPLETED] = completed
            if (role != null) {
                preferences[KEY_USER_ROLE] = role.name
            }
        }
    }

    suspend fun logout() {
        context.dataStore.edit { preferences ->
            preferences[KEY_IS_LOGGED_IN] = false
            preferences[KEY_USER_ID] = ""
            preferences[KEY_ONBOARDING_COMPLETED] = false
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
