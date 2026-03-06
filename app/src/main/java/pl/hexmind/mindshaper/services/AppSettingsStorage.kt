package pl.hexmind.mindshaper.services

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.common.ui.views.values.ThoughtValueSystem
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service class for managing application settings and preferences
 */
@Singleton
class AppSettingsStorage @Inject constructor(
    @ApplicationContext
    private val context : Context
) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        SETTINGS, Context.MODE_PRIVATE
    )

    companion object {
        private const val SETTINGS = "settings"

        // Main app settings
        private const val PARAM_YOUR_NAME = "param_your_name"
        private const val PARAM_DB_CURRENT_VERSION = "param_db_current_version"

        // Navigation bar settings
        private const val PARAM_NAV_IS_EXPANDED = "param_nav_is_expanded"

        private const val PARAM_THOUGHTS_VALUES_SYSTEM = "param_thoughts_values_system"

        // Permissions
        private const val PARAM_VOICE_RECORDING_ENABLED = "param_voice_recording_enabled"

        private const val PARAM_PHOTO_FEATURE_ENABLED = "photo_feature_enabled"
    }

    fun getApplicationContext() : Context {
        return context
    }

    fun setCurrentDBVersion(currentDBVersion: Int) {
        sharedPreferences.edit {
            putInt(PARAM_DB_CURRENT_VERSION, currentDBVersion)
        }
    }

    fun getCurrentDBVersion(): Int {
        return sharedPreferences.getInt(PARAM_DB_CURRENT_VERSION, -1)
    }

    // === YOUR NAME ===

    fun setYourName(yourName: String) {
        sharedPreferences.edit {
            putString(PARAM_YOUR_NAME, yourName.trim())
        }
    }

    fun getYourName(): String {
        val defaultName = context.getString(R.string.settings_your_name_default)
        return sharedPreferences.getString(PARAM_YOUR_NAME, defaultName) ?: defaultName
    }

    // === NAVIGATION BAR STATE ===

    fun setNavigationExpanded(isExpanded: Boolean) {
        sharedPreferences.edit {
            putBoolean(PARAM_NAV_IS_EXPANDED, isExpanded)
        }
    }

    fun isNavigationExpanded(): Boolean {
        return sharedPreferences.getBoolean(PARAM_NAV_IS_EXPANDED, false)
    }

    // === THOUGHT VALUES SYSTEM ===

    fun setThoughtValueSystemId(system : ThoughtValueSystem){
        sharedPreferences.edit {
            putString(PARAM_THOUGHTS_VALUES_SYSTEM, system.name)
        }
    }

    fun getThoughtValueSystem() : ThoughtValueSystem {
        val value = sharedPreferences.getString(PARAM_THOUGHTS_VALUES_SYSTEM, "")
        return if(!value.isNullOrBlank()) ThoughtValueSystem.valueOf(value) else ThoughtValueSystem.STANDARD_10
    }

    fun setVoiceRecordingEnabled(enabled: Boolean) {
        sharedPreferences.edit {
            putBoolean(PARAM_VOICE_RECORDING_ENABLED, enabled)
        }
    }

    fun isVoiceRecordingEnabled(): Boolean {
        return sharedPreferences.getBoolean(PARAM_VOICE_RECORDING_ENABLED, false)
    }

    // ========================================
    // === ONBOARDING METHODS ===
    // ========================================

    fun wasOnboardingTooltipShown(stepKey: String): Boolean {
        return sharedPreferences.getBoolean(stepKey, false)
    }

    fun markOnboardingTooltipShown(stepKey: String) {
        sharedPreferences.edit {
            putBoolean(stepKey, true)
        }
    }

    fun isPhotoFeatureEnabled(): Boolean {
        return sharedPreferences.getBoolean(PARAM_PHOTO_FEATURE_ENABLED, true)
    }

    fun setPhotoFeatureEnabled(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(PARAM_PHOTO_FEATURE_ENABLED, enabled)
            .apply()
    }
}