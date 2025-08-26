package pl.hexmind.fastnote.activities.capture.models

import androidx.annotation.StringRes
import pl.hexmind.fastnote.R

class CapturedThoughtValidator {

    companion object {

        const val ESSENCE_MAX_WORDS = 10

        fun validate(input: CapturedThought): ValidationResult {
            val essenceValidationResult = validateEssence(input.essence)
            // TODO: tu beda jeszcze inne walidacje :)
            return  essenceValidationResult
        }

        fun validateEssence(essenceText : String) : ValidationResult {
            if (essenceText.trim().isEmpty()) {
                return ValidationResult.Error(R.string.capture_essence_error_empty)
            }

            val wordCount = essenceText.trim().split("\\s+".toRegex()).size

            return when {
                wordCount > ESSENCE_MAX_WORDS ->
                    ValidationResult.Error(R.string.capture_essence_error_too_long, ESSENCE_MAX_WORDS.toString())

                wordCount == ESSENCE_MAX_WORDS ->
                    ValidationResult.Valid(R.string.capture_essence_state_no_words_left)

                else -> {
                    val remaining = ESSENCE_MAX_WORDS - wordCount
                    val info = "•".repeat(remaining)
                    ValidationResult.Valid(R.string.capture_essence_state_remaining, info)
                }
            }
        }
    }

    sealed class ValidationResult {
        data class Valid(@StringRes val messageId: Int, val param : String? = null) : ValidationResult()
        data class Error(@StringRes val messageId: Int, val param : String? = null) : ValidationResult()
    }
}