package pl.hexmind.mindshaper.services

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.core.content.edit
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service class for managing FastNote application settings and preferences
 */
@Singleton
class AppSettingsStorage @Inject constructor(
    @ApplicationContext private val context : Context
) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        SETTINGS, Context.MODE_PRIVATE
    )

    companion object {
        private const val SETTINGS = "settings"

        // Main app settings
        private const val PARAM_YOUR_NAME = "param_your_name"
        private const val PARAM_WELCOME_AUDIO_FILE = "param_greeting_audio_path"

        // Phase names
        private const val PARAM_PHASE_GATHERING_ALIAS = "param_phase_gathering_alias"
        private const val PARAM_PHASE_CHOOSING_ALIAS = "param_phase_choosing_alias"
        private const val PARAM_PHASE_SILENT_ALIAS = "param_phase_silent_alias"

        private const val PARAM_DB_CURRENT_VERSION = "param_db_current_version"

        private const val PARAM_APP_LAUNCH_DATES = "param_app_launch_dates"
    }

    fun getApplicationContext() : Context {
        return context
    }

    fun setAppLaunchDate(lastDateAppLaunch: LocalDate) {
        val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
        val today = lastDateAppLaunch.format(formatter)
        val launches = sharedPreferences.getStringSet(PARAM_APP_LAUNCH_DATES, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        launches.add(today)
        sharedPreferences.edit { putStringSet(PARAM_APP_LAUNCH_DATES, launches) }
    }

    fun getAppLaunchDates(): Set<LocalDate>? {
        val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
        val launches = sharedPreferences.getStringSet(PARAM_APP_LAUNCH_DATES, emptySet()) ?: emptySet()
        return launches.map { LocalDate.parse(it, formatter) }.toSet()
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

    /**
     * Saves custom application name
     */
    fun setYourName(yourName: String) {
        sharedPreferences.edit {
            putString(PARAM_YOUR_NAME, yourName.trim())
        }
    }

    /**
     * Retrieves custom application name
     */
    fun getYourName(): String {
        return sharedPreferences.getString(PARAM_YOUR_NAME, "") ?: ""
    }

    // === WELCOME AUDIO FILE ===

    /**
     * Saves welcome audio file URI
     */
    fun setWelcomeAudioUri(uri: Uri?) {
        sharedPreferences.edit {
            if (uri != null) {
                putString(PARAM_WELCOME_AUDIO_FILE, uri.toString())
            } else {
                remove(PARAM_WELCOME_AUDIO_FILE)
            }
        }
    }

    /**
     * Retrieves welcome audio file URI
     */
    fun getWelcomeAudioUri(): Uri? {
        val uriString = sharedPreferences.getString(PARAM_WELCOME_AUDIO_FILE, "")
        return if (uriString?.isNotEmpty() == true) {
            try {
                uriString.toUri()
            } catch (e: Exception) {
                null
            }
        } else null
    }

    /**
     * Checks if welcome audio is set
     */
    fun hasWelcomeAudio(): Boolean {
        return getWelcomeAudioUri() != null
    }

    /**
     * Removes welcome audio setting
     */
    fun clearWelcomeAudio() {
        setWelcomeAudioUri(null)
    }

    // === PHASE NAMES ===

    /**
     * Retrieves phase 1 name with fallback to default
     */
    fun getPhaseGatheringAlias(defaultName: String = "Faza 1"): String {
        return sharedPreferences.getString(PARAM_PHASE_GATHERING_ALIAS, defaultName) ?: defaultName
    }

    /**
     * Retrieves phase 2 name with fallback to default
     */
    fun getPhaseChoosingAlias(defaultName: String = "Faza 2"): String {
        return sharedPreferences.getString(PARAM_PHASE_CHOOSING_ALIAS, defaultName) ?: defaultName
    }

    /**
     * Retrieves phase 3 name with fallback to default
     */
    fun getPhaseSilentAlias(defaultName: String = "Faza 3"): String {
        return sharedPreferences.getString(PARAM_PHASE_SILENT_ALIAS, defaultName) ?: defaultName
    }

    /**
     * Saves all phase names at once
     */
    fun setPhaseNames(phase1: String, phase2: String, phase3: String) {
        sharedPreferences.edit {
            putString(PARAM_PHASE_GATHERING_ALIAS, phase1.trim())
            putString(PARAM_PHASE_CHOOSING_ALIAS, phase2.trim())
            putString(PARAM_PHASE_SILENT_ALIAS, phase3.trim())
        }
    }
}