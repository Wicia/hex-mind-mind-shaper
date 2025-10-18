package pl.hexmind.mindshaper.services.validators

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.activities.capture.models.InitialThoughtType
import pl.hexmind.mindshaper.common.regex.convertToWords
import pl.hexmind.mindshaper.common.validation.ValidationResult
import pl.hexmind.mindshaper.services.dto.ThoughtDTO
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThoughtValidator @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        const val THREAD_MAX_WORDS : Int = 3
    }

    fun validateDTO(input: ThoughtDTO): ValidationResult {
        var result = performThoughtTypeValidation(input)
        if(result is ValidationResult.Error){
            return result
        }
        result = validateThread(input.thread)
        if(result is ValidationResult.Error){
            return result
        }

        return  result
    }

    fun performThoughtTypeValidation(input: ThoughtDTO) : ValidationResult{
        return when(input.initialThoughtType){
            InitialThoughtType.RICH_TEXT -> {
                validateRichText(input.richText)
            }

            InitialThoughtType.UNKNOWN -> {
                TODO()
            }
            InitialThoughtType.RECORDING -> {
                TODO()
            }
            InitialThoughtType.PHOTO -> {
                TODO()
            }
            InitialThoughtType.DRAWING -> {
                TODO()
            }
        }
    }

    fun validateRichText(richText : String?) : ValidationResult {
        return if(richText.isNullOrBlank()){
            ValidationResult.Error(context.getString(R.string.capture_rich_text_error_note_empty))
        }
        else{
            ValidationResult.Valid()
        }
    }

    fun validateThread(threadString: String?) : ValidationResult {
        val thread = threadString?.trim().orEmpty()
        if (thread.isEmpty()) {
            return ValidationResult.Valid()
        }

        val words = thread.convertToWords()

        return if (words.size > THREAD_MAX_WORDS) {
            ValidationResult.Error(context.getString(R.string.capture_thread_error_words_exceeded))
        } else{
            ValidationResult.Valid()
        }
    }
}