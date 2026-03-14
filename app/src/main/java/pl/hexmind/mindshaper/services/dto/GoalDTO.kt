package pl.hexmind.mindshaper.services.dto

data class GoalDTO(
    val id: Int = 0,
    val description: String = "",
    val priority: Int = 3,
    val lastModifiedAt: Long = System.currentTimeMillis(),
    val guidelines: List<GuidelineDTO> = emptyList()
)