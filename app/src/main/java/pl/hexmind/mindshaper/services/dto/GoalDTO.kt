package pl.hexmind.mindshaper.services.dto

import pl.hexmind.mindshaper.database.models.GoalEntity

data class GoalDTO(
    val id: Int = 0,
    val description: String = "",
    val importance: Int = 1,
    val status: String = GoalEntity.STATUS_ACTIVE,
    val lastModifiedAt: Long = System.currentTimeMillis(),
    val steps: List<StepDTO> = emptyList()
)