package pl.hexmind.mindshaper.services

import pl.hexmind.mindshaper.database.models.GoalEntity
import pl.hexmind.mindshaper.database.models.StepEntity
import pl.hexmind.mindshaper.database.repositories.WorkshopRepository
import pl.hexmind.mindshaper.services.dto.GoalDTO
import pl.hexmind.mindshaper.services.dto.StepWithGoalDTO
import pl.hexmind.mindshaper.services.mappers.GoalMapper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoalsService @Inject constructor(
    private val repository: WorkshopRepository,
    private val thoughtsService: ThoughtsService
) {

    // ── Goals ──────────────────────────────────────────────────────────────────

    suspend fun getAllGoals(): List<GoalDTO> =
        repository.getAllGoalsWithSteps().map { GoalMapper.entityToDTO(it) }

    suspend fun addGoal(description: String, importance: Int = 1): Long {
        val entity = GoalEntity(
            description    = description.trim(),
            importance     = importance,
            lastModifiedAt = System.currentTimeMillis()
        )
        return repository.insertGoal(entity)
    }

    suspend fun updateGoalDescription(goalId: Int, description: String) {
        val current = repository.getGoalById(goalId) ?: return
        repository.updateGoal(current.copy(
            description    = description.trim(),
            lastModifiedAt = System.currentTimeMillis()
        ))
    }

    suspend fun updateGoalImportance(goalId: Int, importance: Int) {
        val current = repository.getGoalById(goalId) ?: return
        repository.updateGoal(current.copy(
            importance     = importance,
            lastModifiedAt = System.currentTimeMillis()
        ))
    }

    suspend fun deleteGoal(goalId: Int) =
        repository.deleteGoal(goalId)

    // ── Steps ─────────────────────────────────────────────────────────────────

    suspend fun addStep(goalId: Int, description: String, maxRepetitions: Int = 1) {
        val existingCount = repository.getStepsByGoalId(goalId).size
        val entity = StepEntity(
            goalId             = goalId,
            description        = description.trim(),
            position           = existingCount,
            currentRepetitions = 0,
            maxRepetitions     = maxRepetitions
        )
        repository.insertStep(entity)
    }

    suspend fun updateStep(stepId: Int, description: String, maxRepetitions: Int) {
        val current = repository.getStepById(stepId) ?: return
        repository.updateStep(current.copy(
            description        = description.trim(),
            // Clamp current so it never exceeds the new max
            currentRepetitions = current.currentRepetitions.coerceAtMost(maxRepetitions),
            maxRepetitions     = maxRepetitions
        ))
    }

    suspend fun updateStepCurrentRepetitions(stepId: Int, currentRepetitions: Int) {
        val current = repository.getStepById(stepId) ?: return
        repository.updateStep(current.copy(currentRepetitions = currentRepetitions))
    }

    suspend fun deleteStep(stepId: Int) =
        repository.deleteStep(stepId)

    suspend fun reorderSteps(orderedIds: List<Int>) {
        orderedIds.forEachIndexed { index, id ->
            repository.updateStepPosition(id, index)
        }
    }

    // ── Linked thought ──────────────────────────────────────────────────

    suspend fun linkThought(stepId: Int, thoughtId: Int) {
        val current = repository.getStepById(stepId) ?: return
        repository.updateStep(current.copy(thoughtId = thoughtId))
    }

    /**
     * Unlinks the thought from the step.
     * If [alsoDeleteThought] is true, the underlying thought is also deleted from THOUGHTS.
     */
    suspend fun unlinkThought(stepId: Int, alsoDeleteThought: Boolean) {
        val current = repository.getStepById(stepId) ?: return
        val linkedId = current.thoughtId ?: return

        // Clear FK first (avoid orphan window even though SET_NULL would handle it)
        repository.updateStep(current.copy(thoughtId = null))

        if (alsoDeleteThought) {
            thoughtsService.deleteThoughtById(linkedId)
        }
    }

    /**
     * Returns the step that the given thought is linked to (along with goal context),
     * or null if no step references this thought.
     */
    suspend fun findStepLinkedToThought(thoughtId: Int): StepWithGoalDTO? {
        val step = repository.findStepByThoughtId(thoughtId) ?: return null
        val goal = repository.getGoalById(step.goalId) ?: return null
        return StepWithGoalDTO(
            stepId          = step.id,
            stepDescription = step.description,
            goalId          = goal.id,
            goalDescription = goal.description,
            goalImportance  = goal.importance
        )
    }

    /**
     * Returns goals with their steps that have no linked thought yet
     */
    suspend fun getAvailableStepsForLink(): List<GoalDTO> =
        getAllGoals()
            .map { goal -> goal.copy(steps = goal.steps.filter { it.thoughtId == null }) }
            .filter { it.steps.isNotEmpty() }

    /**
     * Links thought to step from the thought side.
     * If the thought was already linked to a different step, the previous link is cleared first
     */
    suspend fun linkThoughtToStep(thoughtId: Int, stepId: Int) {
        // Clear any previous link pointing to this thought ("defensive programming" type of protection)
        repository.findStepByThoughtId(thoughtId)?.let { previous ->
            if (previous.id != stepId) {
                repository.updateStep(previous.copy(thoughtId = null))
            }
        }
        linkThought(stepId, thoughtId)
    }
}
