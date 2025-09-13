package pl.hexmind.fastnote.activities.settings

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import pl.hexmind.fastnote.R
import pl.hexmind.fastnote.common.validation.ValidationResult
import pl.hexmind.fastnote.services.dto.DomainDTO
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DomainValidator @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        const val NAME_MAX_CHARS: Int = 35
    }

    fun validate(input: DomainDTO): ValidationResult {
        return validateName(input.name)
    }

    fun validateName(name: String): ValidationResult {
        return when {
            name.trim().isEmpty() -> {
                ValidationResult.Error(context.getString(R.string.settings_domains_error_name_empty))
            }

            name.length > NAME_MAX_CHARS -> {
                ValidationResult.Error(
                    context.getString(R.string.settings_domains_error_name_too_long, NAME_MAX_CHARS.toString())
                )
            }

            else -> ValidationResult.Valid()
        }
    }
}