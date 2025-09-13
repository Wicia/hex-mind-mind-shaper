package pl.hexmind.fastnote.common.validation

import androidx.annotation.StringRes

sealed class ValidationResult {
    data class Valid(val message: String? = "") : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}