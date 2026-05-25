package pl.hexmind.mindshaper.services.dto

data class StepDTO(
    val id: Int = 0,
    val goalId: Int = 0,
    val description: String = "",
    val position: Int = 0,
    val currentRepetitions: Int = 0,
    val maxRepetitions: Int = 1,
    val thoughtId: Int? = null
)
