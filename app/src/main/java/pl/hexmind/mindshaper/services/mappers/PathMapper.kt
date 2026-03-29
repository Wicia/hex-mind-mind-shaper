package pl.hexmind.mindshaper.services.mappers

import pl.hexmind.mindshaper.database.models.PathWithSteps
import pl.hexmind.mindshaper.services.dto.PathDTO

object PathMapper {

    fun toDTO(row: PathWithSteps): PathDTO {
        val steps = row.steps.sortedBy { it.position }
        val totalSteps = steps.size
        val index = row.path.currentStepIndex.coerceIn(0, (totalSteps - 1).coerceAtLeast(0))

        return PathDTO(
            pathKey = row.path.pathKey,
            category = row.path.category,
            status = row.path.status,
            currentStepIndex = index,
            totalSteps = totalSteps,
            currentStepContent = steps.getOrNull(index)?.content ?: "",
            isFirstStep = index == 0,
            isLastStep = index == totalSteps - 1
        )
    }
}