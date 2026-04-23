package com.example.postureguard

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class SensitivityLevel { LOW, MEDIUM, HIGH }
enum class AlertLanguage { ZH, EN }

data class SettingsProfile(
    val onboardingCompleted: Boolean = false,
    val alertIntervalSeconds: Int = 5,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = false,
    val sensitivityLevel: SensitivityLevel = SensitivityLevel.MEDIUM,
    val alertLanguage: AlertLanguage = AlertLanguage.ZH,
    val autoResumeMinutes: Int = 10
) {
    val sensitivityMultiplier: Double
        get() = when (sensitivityLevel) {
            SensitivityLevel.LOW -> 1.5
            SensitivityLevel.MEDIUM -> 1.0
            SensitivityLevel.HIGH -> 0.7
        }
}

class SettingsStore(private val context: Context) {

    companion object {
        private val KEY_ONBOARDING = booleanPreferencesKey("onboarding_completed")
        private val KEY_ALERT_INTERVAL = intPreferencesKey("alert_interval_seconds")
        private val KEY_SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        private val KEY_VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        private val KEY_SENSITIVITY = stringPreferencesKey("sensitivity_level")
        private val KEY_LANGUAGE = stringPreferencesKey("alert_language")
        private val KEY_AUTO_RESUME = intPreferencesKey("auto_resume_minutes")
    }

    suspend fun load(): SettingsProfile {
        return context.settingsDataStore.data.map { prefs ->
            val interval = prefs[KEY_ALERT_INTERVAL] ?: 5
            val validIntervals = setOf(5, 10, 30, 60)
            val autoResume = prefs[KEY_AUTO_RESUME] ?: 10
            val validAutoResume = setOf(5, 10, 15, 20)
            SettingsProfile(
                onboardingCompleted = prefs[KEY_ONBOARDING] ?: false,
                alertIntervalSeconds = if (interval in validIntervals) interval else 5,
                soundEnabled = prefs[KEY_SOUND_ENABLED] ?: true,
                vibrationEnabled = prefs[KEY_VIBRATION_ENABLED] ?: true,
                sensitivityLevel = try {
                    SensitivityLevel.valueOf(prefs[KEY_SENSITIVITY] ?: "MEDIUM")
                } catch (_: Exception) { SensitivityLevel.MEDIUM },
                alertLanguage = try {
                    AlertLanguage.valueOf(prefs[KEY_LANGUAGE] ?: "ZH")
                } catch (_: Exception) { AlertLanguage.ZH },
                autoResumeMinutes = if (autoResume in validAutoResume) autoResume else 10
            )
        }.first()
    }

    suspend fun saveOnboarding(completed: Boolean) {
        context.settingsDataStore.edit { it[KEY_ONBOARDING] = completed }
    }

    suspend fun saveAlertInterval(seconds: Int) {
        context.settingsDataStore.edit { it[KEY_ALERT_INTERVAL] = seconds }
    }

    suspend fun saveSoundEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEY_SOUND_ENABLED] = enabled }
    }

    suspend fun saveVibrationEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEY_VIBRATION_ENABLED] = enabled }
    }

    suspend fun saveSensitivity(level: SensitivityLevel) {
        context.settingsDataStore.edit { it[KEY_SENSITIVITY] = level.name }
    }

    suspend fun saveLanguage(language: AlertLanguage) {
        context.settingsDataStore.edit { it[KEY_LANGUAGE] = language.name }
    }

    suspend fun saveAutoResumeMinutes(minutes: Int) {
        context.settingsDataStore.edit { it[KEY_AUTO_RESUME] = minutes }
    }
}
