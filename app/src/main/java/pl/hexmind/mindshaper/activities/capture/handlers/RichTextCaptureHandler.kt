package pl.hexmind.mindshaper.activities.capture.handlers

import androidx.core.widget.doAfterTextChanged
import pl.hexmind.mindshaper.activities.ThoughtCaptureHandler
import pl.hexmind.mindshaper.services.validators.ThoughtValidator
import pl.hexmind.mindshaper.common.validation.ValidationResult
import pl.hexmind.mindshaper.services.dto.ThoughtDTO

class RichTextCaptureHandler(
    private val view: RichTextCaptureView,
    private val validator: ThoughtValidator
) : ThoughtCaptureHandler {

    fun setupListeners() {
        view.etRichText.doAfterTextChanged { editable ->
            editable?.let {
                val result : ValidationResult = validator.validateRichText(view.etRichText.text.toString())
                view.updateValidationInfo(result)
            }
        }
    }

    override fun performValidation() : ValidationResult {
        val result : ValidationResult = validator.validateRichText(view.etRichText.text.toString())
        view.updateValidationInfo(result)
        return result
    }

    override fun getUpdatedDTO(dto : ThoughtDTO): ThoughtDTO {
        dto.richText = view.etRichText.text.toString()
        return dto
    }
}