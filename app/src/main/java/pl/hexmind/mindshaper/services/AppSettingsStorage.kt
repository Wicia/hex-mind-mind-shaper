package pl.hexmind.mindshaper.services

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.common.ui.views.values.ThoughtValueSystem
import pl.hexmind.mindshaper.services.dto.DefaultCaptureForm
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

        private const val PARAM_THOUGHTS_VALUES_SYSTEM = "param_thoughts_values_system"

        // Permissions
        private const val PARAM_VOICE_RECORDING_ENABLED = "param_voice_recording_enabled"

        // Default capture form
        private const val PARAM_DEFAULT_CAPTURE_FORM = "param_default_capture_form"

        private const val PARAM_PHOTO_FEATURE_ENABLED = "photo_feature_enabled"

        private const val PARAM_CALENDAR_REMINDERS_ENABLED = "calendar_reminders_enabled"

        private const val PARAM_CALENDAR_TARGET_ID = "calendar_target_id"

        private const val NO_CALENDAR_SELECTED = -1L // "nothing chosen yet"

        private const val PARAM_BACKUP_ENABLED = "param_backup_enabled"

        // Slow mode
        private const val PARAM_SLOW_MODE_ENABLED = "param_slow_mode_enabled"
        private const val PARAM_SLOW_MODE_HOURS   = "param_slow_mode_hours"

        const val SLOW_MODE_HOURS_MIN = 1 // TODO: Move to validator when there will be more business logic related to validating settings
        const val SLOW_MODE_HOURS_MAX = 72

        // Dormant mode
        private const val PARAM_DORMANT_MODE_ENABLED    = "param_dormant_mode_enabled"
        private const val PARAM_DORMANT_VALUE_THRESHOLD = "param_dormant_value_threshold"
        private const val PARAM_DORMANT_DAYS_THRESHOLD  = "param_dormant_days_threshold"

        const val DORMANT_VALUE_DEFAULT = 1
        const val DORMANT_VALUE_MIN     = 1

        const val DORMANT_DAYS_DEFAULT  = 7
        const val DORMANT_DAYS_MIN      = 3
        const val DORMANT_DAYS_MAX      = 90
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

    // Removing the key instead of writing false keeps the "never shown" and "reset" states identical
    fun clearOnboardingTooltipShown(stepKey: String) {
        sharedPreferences.edit {
            remove(stepKey)
        }
    }

    fun isPhotoFeatureEnabled(): Boolean {
        return sharedPreferences.getBoolean(PARAM_PHOTO_FEATURE_ENABLED, true)
    }

    fun setPhotoFeatureEnabled(enabled: Boolean) {
        sharedPreferences.edit {
            putBoolean(PARAM_PHOTO_FEATURE_ENABLED, enabled)
        }
    }

    // === CALENDAR REMINDERS ===

    fun isCalendarRemindersEnabled(): Boolean {
        return sharedPreferences.getBoolean(PARAM_CALENDAR_REMINDERS_ENABLED, false)
    }

    fun setCalendarRemindersEnabled(enabled: Boolean) {
        sharedPreferences.edit {
            putBoolean(PARAM_CALENDAR_REMINDERS_ENABLED, enabled)
        }
    }


    fun getCalendarTargetId(): Long? {
        val stored = sharedPreferences.getLong(PARAM_CALENDAR_TARGET_ID, NO_CALENDAR_SELECTED)

        // null = no choice stored yet
        return if (stored == NO_CALENDAR_SELECTED) null else stored
    }

    fun setCalendarTargetId(calendarId: Long) {
        sharedPreferences.edit {
            putLong(PARAM_CALENDAR_TARGET_ID, calendarId)
        }
    }

    // === BACKUP ===

    fun isBackupEnabled(): Boolean {
        return sharedPreferences.getBoolean(PARAM_BACKUP_ENABLED, false)
    }

    fun setBackupEnabled(enabled: Boolean) {
        sharedPreferences.edit {
            putBoolean(PARAM_BACKUP_ENABLED, enabled)
        }
    }

    // === DEFAULT CAPTURE FORM ===

    fun setDefaultCaptureForm(form: DefaultCaptureForm) {
        sharedPreferences.edit {
            putString(PARAM_DEFAULT_CAPTURE_FORM, form.name)
        }
    }

    fun getDefaultCaptureForm(): DefaultCaptureForm {
        val value = sharedPreferences.getString(PARAM_DEFAULT_CAPTURE_FORM, "")
        return if (!value.isNullOrBlank()) DefaultCaptureForm.valueOf(value) else DefaultCaptureForm.TEXT
    }

    // === SLOW MODE ===

    fun isSlowModeEnabled(): Boolean =
        sharedPreferences.getBoolean(PARAM_SLOW_MODE_ENABLED, false)

    fun setSlowModeEnabled(enabled: Boolean) {
        sharedPreferences.edit { putBoolean(PARAM_SLOW_MODE_ENABLED, enabled) }
    }

    fun getSlowModeHours(): Int =
        sharedPreferences.getInt(PARAM_SLOW_MODE_HOURS, SLOW_MODE_HOURS_MIN)

    fun setSlowModeHours(hours: Int) {
        sharedPreferences.edit { putInt(PARAM_SLOW_MODE_HOURS, hours) }
    }

    // === DORMANT MODE ===

    fun isDormantModeEnabled(): Boolean =
        sharedPreferences.getBoolean(PARAM_DORMANT_MODE_ENABLED, false)

    fun setDormantModeEnabled(enabled: Boolean) {
        sharedPreferences.edit { putBoolean(PARAM_DORMANT_MODE_ENABLED, enabled) }
    }

    fun getDormantValueThreshold(): Int =
        sharedPreferences.getInt(PARAM_DORMANT_VALUE_THRESHOLD, DORMANT_VALUE_DEFAULT)

    fun setDormantValueThreshold(value: Int) {
        sharedPreferences.edit { putInt(PARAM_DORMANT_VALUE_THRESHOLD, value) }
    }

    fun getDormantDaysThreshold(): Int =
        sharedPreferences.getInt(PARAM_DORMANT_DAYS_THRESHOLD, DORMANT_DAYS_DEFAULT)

    fun setDormantDaysThreshold(days: Int) {
        sharedPreferences.edit { putInt(PARAM_DORMANT_DAYS_THRESHOLD, days) }
    }

    /**
     * Max dormant value threshold - mirrors current ThoughtValueSystem.maxValue
     */
    fun getDormantValueMax(): Int = getThoughtValueSystem().maxValue
}