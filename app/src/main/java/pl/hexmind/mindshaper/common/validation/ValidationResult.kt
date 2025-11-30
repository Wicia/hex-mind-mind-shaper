package pl.hexmind.mindshaper.common.validation

sealed class ValidationResult {
    data class Valid(val message: String? = "") : ValidationResult()
    data class Error(val message: String, val refProperty : ValidatedProperty? = null) : ValidationResult()
}

// TODO (future): Maybe replace enum with @DisplayName("thought.thread") -> use reflection to specify properties names
enum class ValidatedProperty {
    T_THREAD,
    T_PROJECT,
    T_SOUL_MATES,
    T_RICH_TEXT,
    T_AUDIO
}