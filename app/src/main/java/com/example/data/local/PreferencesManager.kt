package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.NotificationPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "truecheck_prefs")

class PreferencesManager(private val context: Context) {
    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val ACTIVE_USER_ID = stringPreferencesKey("active_user_id")
        val NOTIF_VERIFICATION_COMPLETED = booleanPreferencesKey("notif_verification_completed")
        val NOTIF_SUSPICIOUS_ALERTS = booleanPreferencesKey("notif_suspicious_alerts")
        val NOTIF_CODE_REUSE_ALERTS = booleanPreferencesKey("notif_code_reuse_alerts")
        val NOTIF_SAFETY_RECALLS = booleanPreferencesKey("notif_safety_recalls")
    }

    val onboardingCompleted: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.ONBOARDING_COMPLETED] ?: false
    }

    val activeUserId: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.ACTIVE_USER_ID]?.takeIf { it.isNotBlank() }
    }

    val notificationPreferences: Flow<NotificationPreferences> = context.dataStore.data.map { prefs ->
        NotificationPreferences(
            verificationCompleted = prefs[Keys.NOTIF_VERIFICATION_COMPLETED] ?: true,
            suspiciousAlerts = prefs[Keys.NOTIF_SUSPICIOUS_ALERTS] ?: true,
            codeReuseAlerts = prefs[Keys.NOTIF_CODE_REUSE_ALERTS] ?: true,
            safetyRecalls = prefs[Keys.NOTIF_SAFETY_RECALLS] ?: true
        )
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setActiveUserId(userId: String?) {
        context.dataStore.edit { prefs ->
            if (userId.isNullOrBlank()) {
                prefs.remove(Keys.ACTIVE_USER_ID)
            } else {
                prefs[Keys.ACTIVE_USER_ID] = userId
            }
        }
    }

    suspend fun updateNotificationPreferences(updated: NotificationPreferences) {
        context.dataStore.edit { prefs ->
            prefs[Keys.NOTIF_VERIFICATION_COMPLETED] = updated.verificationCompleted
            prefs[Keys.NOTIF_SUSPICIOUS_ALERTS] = updated.suspiciousAlerts
            prefs[Keys.NOTIF_CODE_REUSE_ALERTS] = updated.codeReuseAlerts
            prefs[Keys.NOTIF_SAFETY_RECALLS] = updated.safetyRecalls
        }
    }
}
