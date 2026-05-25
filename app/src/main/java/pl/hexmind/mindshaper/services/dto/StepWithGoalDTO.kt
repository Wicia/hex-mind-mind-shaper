package pl.hexmind.mindshaper.services.dto

data class StepWithGoalDTO(
    val stepId: Int,
    val stepDescription: String,
    val goalId: Int,
    val goalDescription: String,
    val goalImportance: Int
)