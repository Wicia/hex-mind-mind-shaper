package pl.hexmind.mindshaper.services.dto

data class PathDTO(
    val pathKey: String,
    val category: String,
    val status: String,
    val currentStepIndex: Int,
    val totalSteps: Int,
    val currentStepContent: String,
    val isFirstStep: Boolean,
    val isLastStep: Boolean
)