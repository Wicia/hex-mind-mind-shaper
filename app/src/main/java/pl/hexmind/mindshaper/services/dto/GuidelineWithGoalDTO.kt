package pl.hexmind.mindshaper.services.dto

data class GuidelineWithGoalDTO(
    val guidelineId: Int,
    val guidelineDescription: String,
    val goalId: Int,
    val goalDescription: String,
    val goalImportance: Int
)