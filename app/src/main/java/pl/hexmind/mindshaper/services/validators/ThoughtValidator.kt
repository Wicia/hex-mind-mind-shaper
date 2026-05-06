package pl.hexmind.mindshaper.services.validators

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.common.ui.views.values.ThoughtValueSystem
import pl.hexmind.mindshaper.common.validation.ValidatedProperty
import pl.hexmind.mindshaper.common.validation.ValidationResult
import pl.hexmind.mindshaper.services.AppSettingsStorage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThoughtValidator @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val appSettingsStorage: AppSettingsStorage
) {
    companion object {
        const val THREAD_MAX_CHARS: Int = 36
        const val PROJECT_MAX_CHARS: Int = 36
        const val SOUL_MATES_MAX_CHARS: Int = 36

        const val VOICE_RECORDING_MAX_DURATION_MS = 180_000L
    }

    private val valueSystem: ThoughtValueSystem
        get() = appSettingsStorage.getThoughtValueSystem()

    fun getThoughtValueMax(): Int {
        return valueSystem.maxValue
    }

    fun getThoughtValueMin(): Int {
        return valueSystem.minValue
    }

    fun validateRichText(richText: String?): ValidationResult {
        return if (richText.isNullOrBlank()) {
            ValidationResult.Error(
                context.getString(R.string.capture_rich_text_error_note_empty),
                ValidatedProperty.T_RICH_TEXT
            )
        }
        else {
            ValidationResult.Valid()
        }
    }

    fun validateThread(threadString: String?): ValidationResult {
        val thread = threadString?.trim().orEmpty()
        if (thread.isEmpty()) {
            return ValidationResult.Valid()
        }

        return if (thread.length > THREAD_MAX_CHARS) {
            ValidationResult.Error(
                context.getString(R.string.common_thread_error_chars_exceeded, THREAD_MAX_CHARS),
                ValidatedProperty.T_THREAD
            )
        }
        else {
            ValidationResult.Valid()
        }
    }

    fun validateProject(projectString: String?): ValidationResult {
        val project = projectString?.trim().orEmpty()
        if (project.isEmpty()) {
            return ValidationResult.Valid()
        }

        return if (project.length > PROJECT_MAX_CHARS) {
            ValidationResult.Error(
                context.getString(R.string.common_project_error_chars_exceeded, PROJECT_MAX_CHARS),
                ValidatedProperty.T_PROJECT
            )
        }
        else {
            ValidationResult.Valid()
        }
    }

    fun validateSoulMates(soulMatesString: String?): ValidationResult {
        val soulMates = soulMatesString?.trim().orEmpty()
        if (soulMates.isEmpty()) {
            return ValidationResult.Valid()
        }

        return if (soulMates.length > SOUL_MATES_MAX_CHARS) {
            ValidationResult.Error(
                context.getString(R.string.common_soul_mates_error_chars_exceeded, SOUL_MATES_MAX_CHARS),
                ValidatedProperty.T_SOUL_MATES
            )
        }
        else {
            ValidationResult.Valid()
        }
    }

    fun getValidThoughtValue(newPotentialValue : Int) : Int {
        return newPotentialValue.coerceIn(getThoughtValueMin(), getThoughtValueMax())
    }

    fun canIncreaseValue(currentValue: Int): Boolean {
        return currentValue < getThoughtValueMax()
    }

    fun canDecreaseValue(currentValue: Int): Boolean {
        return currentValue > getThoughtValueMin()
    }
}