package pl.hexmind.mindshaper.activities.capture.handlers

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.common.validation.ValidationResult
import pl.hexmind.mindshaper.services.dto.ThoughtDTO
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThoughtValidator @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {

        const val ESSENCE_MAX_WORDS: Int = 16
    }

    fun validateDTO(input: ThoughtDTO): ValidationResult {
        val essenceValidationResult = validateEssence(input.essence.toString())
        // TODO: tu beda jeszcze inne walidacje :)
        return  essenceValidationResult
    }

    fun validateEssence(essenceText : String) : ValidationResult {
        if (essenceText.trim().isEmpty()) {
            return ValidationResult.Error(context.getString(R.string.capture_essence_error_empty))
        }

        val wordCount = essenceText.trim().split("\\s+".toRegex()).size // TODO: Skip all 1 and 2 chars words (make a copy -> remove -> regex -> validate)

        return when {
            wordCount > ESSENCE_MAX_WORDS -> {
                ValidationResult.Error(context.getString(R.string.capture_essence_error_too_long, ESSENCE_MAX_WORDS.toString()))
            }
            wordCount == ESSENCE_MAX_WORDS -> {
                ValidationResult.Valid(context.getString(R.string.capture_essence_state_no_words_left))
            }
            else -> {
                val remaining = ESSENCE_MAX_WORDS - wordCount
                val info = "●".repeat(remaining)
                ValidationResult.Valid(context.getString(R.string.capture_essence_state_remaining, info))
            }
        }
    }
}