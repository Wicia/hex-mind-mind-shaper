package pl.hexmind.mindshaper.services.validators

import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.common.ui.views.values.ThoughtValueSystem
import pl.hexmind.mindshaper.common.validation.ValidatedProperty
import pl.hexmind.mindshaper.common.validation.ValidationResult
import pl.hexmind.mindshaper.services.AppSettingsStorage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThoughtValidator @Inject constructor(
    private val appSettingsStorage: AppSettingsStorage
) {
    companion object {
        const val SUBJECT_MAX_CHARS: Int = 36
        val TAG_SEPARATOR_PATTERN = Regex("[,\\s]+")

        // Per TAG, not per field - a field may hold several tags separated by spaces or commas
        const val PROJECT_MAX_CHARS: Int = 16
        const val PEOPLE_MAX_CHARS: Int = 16

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
                R.string.capture_rich_text_error_note_empty,
                refProperty = ValidatedProperty.T_RICH_TEXT
            )
        }
        else {
            ValidationResult.Valid()
        }
    }

    fun validateSubject(subjectString: String?): ValidationResult {
        val subject = subjectString?.trim().orEmpty()
        if (subject.isEmpty()) {
            return ValidationResult.Valid()
        }

        return if (subject.length > SUBJECT_MAX_CHARS) {
            ValidationResult.Error(
                R.string.common_subject_error_chars_exceeded,
                SUBJECT_MAX_CHARS.toString(),
                ValidatedProperty.T_SUBJECT
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

        return if (longestTagLength(project) > PROJECT_MAX_CHARS) {
            ValidationResult.Error(
                R.string.common_hex_tag_error_chars_exceeded,
                PROJECT_MAX_CHARS.toString(),
                ValidatedProperty.T_PROJECT
            )
        }
        else {
            ValidationResult.Valid()
        }
    }

    fun validatePeople(peopleString: String?): ValidationResult {
        val people = peopleString?.trim().orEmpty()
        if (people.isEmpty()) {
            return ValidationResult.Valid()
        }

        return if (longestTagLength(people) > PEOPLE_MAX_CHARS) {
            ValidationResult.Error(
                R.string.common_hex_tag_error_chars_exceeded,
                PEOPLE_MAX_CHARS.toString(),
                ValidatedProperty.T_PEOPLE
            )
        }
        else {
            ValidationResult.Valid()
        }
    }

    // The field carries several tags, so the limit applies to the longest one, not the whole text
    private fun longestTagLength(tagsText: String): Int =
        tagsText.split(TAG_SEPARATOR_PATTERN)
            .filter { tagName -> tagName.isNotBlank() }
            .maxOfOrNull { tagName -> tagName.length }
            ?: 0

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