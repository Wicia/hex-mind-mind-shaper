package pl.hexmind.mindshaper.services

import pl.hexmind.mindshaper.database.models.GoalEntity
import pl.hexmind.mindshaper.database.models.GuidelineEntity
import pl.hexmind.mindshaper.database.repositories.WorkshopRepository
import pl.hexmind.mindshaper.services.dto.GoalDTO
import pl.hexmind.mindshaper.services.mappers.GoalMapper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoalsService @Inject constructor(
    private val repository: WorkshopRepository
) {

    // ── Goals ──────────────────────────────────────────────────────────────────

    suspend fun getAllGoals(): List<GoalDTO> =
        repository.getAllGoalsWithGuidelines().map { GoalMapper.entityToDTO(it) }

    suspend fun addGoal(description: String, priority: Int = 3): Long {
        val entity = GoalEntity(
            description = description.trim(),
            priority = priority,
            lastModifiedAt = System.currentTimeMillis()
        )
        return repository.insertGoal(entity)
    }

    suspend fun updateGoalDescription(goalId: Int, description: String) {
        val current = repository.getGoalById(goalId) ?: return
        repository.updateGoal(current.copy(
            description = description.trim(),
            lastModifiedAt = System.currentTimeMillis()
        ))
    }

    suspend fun updateGoalPriority(goalId: Int, priority: Int) {
        val current = repository.getGoalById(goalId) ?: return
        repository.updateGoal(current.copy(
            priority = priority,
            lastModifiedAt = System.currentTimeMillis()
        ))
    }

    suspend fun deleteGoal(goalId: Int) =
        repository.deleteGoal(goalId)

    // ── Guidelines ─────────────────────────────────────────────────────────────

    suspend fun addGuideline(goalId: Int, description: String) {
        val existingCount = repository.getGuidelinesByGoalId(goalId).size
        val entity = GuidelineEntity(
            goalId = goalId,
            description = description.trim(),
            position = existingCount
        )
        repository.insertGuideline(entity)
    }

    suspend fun updateGuidelineDescription(guidelineId: Int, description: String) {
        val current = repository.getGuidelineById(guidelineId) ?: return
        repository.updateGuideline(current.copy(description = description.trim()))
    }

    suspend fun toggleGuidelineDone(guidelineId: Int) {
        val current = repository.getGuidelineById(guidelineId) ?: return
        repository.updateGuideline(current.copy(isDone = !current.isDone))
    }

    suspend fun deleteGuideline(guidelineId: Int) =
        repository.deleteGuideline(guidelineId)

    suspend fun reorderGuidelines(orderedIds: List<Int>) {
        orderedIds.forEachIndexed { index, id ->
            repository.updateGuidelinePosition(id, index)
        }
    }
}