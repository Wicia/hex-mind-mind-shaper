package pl.hexmind.mindshaper.common.validation

sealed class ValidationResult {
    data class Valid(val message: String? = "") : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}