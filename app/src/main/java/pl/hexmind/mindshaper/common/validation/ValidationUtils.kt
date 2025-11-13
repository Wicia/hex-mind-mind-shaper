package pl.hexmind.mindshaper.common.validation

fun validateSequentially(vararg validators: () -> ValidationResult): ValidationResult {
    validators.forEach { validator ->
        when (val result = validator()) {
            is ValidationResult.Error -> return result
            else -> Unit
        }
    }
    return ValidationResult.Valid()
}