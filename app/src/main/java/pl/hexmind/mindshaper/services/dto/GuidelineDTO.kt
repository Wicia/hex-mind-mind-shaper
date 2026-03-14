package pl.hexmind.mindshaper.services.dto

data class GuidelineDTO(
    val id: Int = 0,
    val goalId: Int = 0,
    val description: String = "",
    val isDone: Boolean = false,
    val position: Int = 0
)