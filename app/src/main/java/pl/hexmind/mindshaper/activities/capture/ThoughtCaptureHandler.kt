package pl.hexmind.mindshaper.activities.capture

import pl.hexmind.mindshaper.common.validation.ValidationResult
import pl.hexmind.mindshaper.services.dto.ThoughtDTO

interface ThoughtCaptureHandler  {

    fun performValidation() : ValidationResult

    /**
     * Updating dto before performing DB operations e.g. saving
     */
    fun getUpdatedDTO(dto : ThoughtDTO) : ThoughtDTO
}