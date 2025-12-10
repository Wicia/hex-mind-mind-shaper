package pl.hexmind.mindshaper.activities.capture.handlers

import androidx.core.widget.doAfterTextChanged
import pl.hexmind.mindshaper.activities.capture.ThoughtCaptureHandler
import pl.hexmind.mindshaper.activities.capture.models.ThoughtMainContentType
import pl.hexmind.mindshaper.services.validators.ThoughtValidator
import pl.hexmind.mindshaper.common.validation.ValidationResult
import pl.hexmind.mindshaper.services.dto.ThoughtDTO

class RichTextCaptureHandler(
    private val view: RichTextCaptureView,
    override val validator: ThoughtValidator
) : ThoughtCaptureHandler {

    fun setupListeners() {
        view.etRichText.doAfterTextChanged { editable ->
            editable?.let {
                val result = validator.validateRichText(it.toString())
                view.updateValidationInfo(result)
            }
        }
    }

    override fun getUpdatedDTO(dto: ThoughtDTO): ThoughtDTO {
        dto.mainContentType = ThoughtMainContentType.RICH_TEXT
        dto.richText = view.etRichText.text.toString()
        return dto
    }

    override fun performTypeSpecificValidation(dto: ThoughtDTO): ValidationResult {
        val result = validator.validateRichText(dto.richText)
        view.updateValidationInfo(result)
        return result
    }
}