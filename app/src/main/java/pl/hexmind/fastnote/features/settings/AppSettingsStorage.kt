package pl.hexmind.fastnote.features.settings

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.core.content.edit
import androidx.core.net.toUri

/**
 * Service class for managing FastNote application settings and preferences
 */
class AppSettingsStorage(private val context: Context) {

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
    }

    @Volatile
    private var INSTANCE: AppSettingsStorage? = null

    /**
     * Get singleton instance of AppSettingsStorage with thread-safe initialization
     */
    fun getInstance(context: Context): AppSettingsStorage {
        return INSTANCE ?: synchronized(this) {
            INSTANCE ?: AppSettingsStorage(context.applicationContext).also { INSTANCE = it }
        }
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
     * Saves phase 1 custom name
     */
    fun setPhaseGatheringAlias(name: String) {
        sharedPreferences.edit {
            putString(PARAM_PHASE_GATHERING_ALIAS, name.trim())
        }
    }

    /**
     * Retrieves phase 1 name with fallback to default
     */
    fun getPhaseGatheringAlias(defaultName: String = "Faza 1"): String {
        return sharedPreferences.getString(PARAM_PHASE_GATHERING_ALIAS, defaultName) ?: defaultName
    }

    /**
     * Saves phase 2 custom name
     */
    fun setPhaseChoosingAlias(name: String) {
        sharedPreferences.edit {
            putString(PARAM_PHASE_CHOOSING_ALIAS, name.trim())
        }
    }

    /**
     * Retrieves phase 2 name with fallback to default
     */
    fun getPhaseChoosingAlias(defaultName: String = "Faza 2"): String {
        return sharedPreferences.getString(PARAM_PHASE_CHOOSING_ALIAS, defaultName) ?: defaultName
    }

    /**
     * Saves phase 3 custom name
     */
    fun setPhaseSilentAlias(name: String) {
        sharedPreferences.edit {
            putString(PARAM_PHASE_SILENT_ALIAS, name.trim())
        }
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

    // === UTILITY METHODS ===

    /**
     * Checks if URI is still accessible by trying to query it
     */
    fun isUriAccessible(uri: Uri): Boolean {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use {
                it.moveToFirst()
            } ?: false
        } catch (e: Exception) {
            false
        }
    }
}