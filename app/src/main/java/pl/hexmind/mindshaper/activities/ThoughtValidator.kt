package pl.hexmind.mindshaper.activities

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.common.regex.convertToWords
import pl.hexmind.mindshaper.common.regex.getWordsCount
import pl.hexmind.mindshaper.common.regex.removeWordsConnectors
import pl.hexmind.mindshaper.common.validation.ValidationResult
import pl.hexmind.mindshaper.services.dto.ThoughtDTO
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThoughtValidator @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {

        const val ESSENCE_MAX_WORDS : Int = 16
        const val THREAD_MAX_WORDS : Int = 3
    }

    fun validateDTO(input: ThoughtDTO): ValidationResult {
        val essenceValidationResult = validateEssence(input.essence.toString())
        // TODO: tu beda jeszcze inne walidacje :)
        return  essenceValidationResult
    }

    fun validateEssence(essenceText : String) : ValidationResult {
        if (essenceText.trim().isEmpty()) {
            return ValidationResult.Valid()
        }

        val clearedText = essenceText.removeWordsConnectors()
        val wordCount = clearedText.getWordsCount()

        return when {
            wordCount > ESSENCE_MAX_WORDS -> {
                ValidationResult.Error(context.getString(R.string.capture_essence_error_too_long, ESSENCE_MAX_WORDS))
            }
            wordCount == ESSENCE_MAX_WORDS -> {
                ValidationResult.Valid(context.getString(R.string.capture_essence_state_no_words_left))
            }
            else -> {
                val remaining = ESSENCE_MAX_WORDS - wordCount
                ValidationResult.Valid(context.getString(R.string.capture_essence_state_remaining, remaining))
            }
        }
    }

    fun validateThread(threadString: String) : ValidationResult {
        val words = threadString.convertToWords()
        return if (words.size > THREAD_MAX_WORDS) {
            ValidationResult.Error(context.getString(R.string.capture_thread_error_words_exceeded))
        } else{
            ValidationResult.Valid()
        }
    }

    fun getEssenceDefaultTooltip() : String{
        return context.getString(R.string.capture_essence_tooltip, ESSENCE_MAX_WORDS)
    }

    fun limitToMaxWords(threadString: String): String {
        val words = threadString.convertToWords()
        return words.take(THREAD_MAX_WORDS).joinToString(" ")
    }
}