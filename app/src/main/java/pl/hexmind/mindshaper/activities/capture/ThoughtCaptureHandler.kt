package pl.hexmind.mindshaper.activities.capture

import pl.hexmind.mindshaper.common.validation.ValidationResult
import pl.hexmind.mindshaper.common.validation.validateSequentially
import pl.hexmind.mindshaper.services.dto.ThoughtDTO
import pl.hexmind.mindshaper.services.validators.ThoughtValidator

interface ThoughtCaptureHandler {

    val validator: ThoughtValidator

    fun getUpdatedDTO(dto: ThoughtDTO): ThoughtDTO

    fun performTypeSpecificValidation(dto: ThoughtDTO): ValidationResult

    fun performValidation(dto: ThoughtDTO): ValidationResult {
        return validateSequentially(
            { validator.validateThread(dto.thread) },
            { validator.validateProject(dto.project) },
            { validator.validateSoulMates(dto.soulMate) },
            { performTypeSpecificValidation(dto)
            }
        )
    }
}
