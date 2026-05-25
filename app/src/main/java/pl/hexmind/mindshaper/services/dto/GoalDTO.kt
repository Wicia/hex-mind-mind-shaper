package pl.hexmind.mindshaper.services.dto

data class GoalDTO(
    val id: Int = 0,
    val description: String = "",
    val importance: Int = 1,
    val lastModifiedAt: Long = System.currentTimeMillis(),
    val steps: List<StepDTO> = emptyList()
)
