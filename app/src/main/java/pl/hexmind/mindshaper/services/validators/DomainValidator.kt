package pl.hexmind.mindshaper.services.validators

import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.common.validation.ValidationResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DomainValidator @Inject constructor() {

    companion object {
        const val NAME_MAX_CHARS: Int = 36
    }

    fun validateName(name: String): ValidationResult {
        return when {
            name.trim().isEmpty() -> {
                ValidationResult.Error(R.string.settings_domain_edit_error_name_empty)
            }

            name.length > NAME_MAX_CHARS -> {
                ValidationResult.Error(
                    R.string.settings_domain_edit_error_name_too_long,
                    NAME_MAX_CHARS.toString()
                )
            }

            else -> ValidationResult.Valid()
        }
    }
}