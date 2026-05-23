package pl.hexmind.mindshaper.services

import pl.hexmind.mindshaper.database.models.GoalEntity
import pl.hexmind.mindshaper.database.models.GuidelineEntity
import pl.hexmind.mindshaper.database.repositories.WorkshopRepository
import pl.hexmind.mindshaper.services.dto.GoalDTO
import pl.hexmind.mindshaper.services.dto.GuidelineWithGoalDTO
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
        repository.getAllGoalsWithGuidelines().map { GoalMapper.entityToDTO(it) }

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

    // ── Guidelines ─────────────────────────────────────────────────────────────

    suspend fun addGuideline(goalId: Int, description: String, maxRepetitions: Int = 1) {
        val existingCount = repository.getGuidelinesByGoalId(goalId).size
        val entity = GuidelineEntity(
            goalId             = goalId,
            description        = description.trim(),
            position           = existingCount,
            currentRepetitions = 0,
            maxRepetitions     = maxRepetitions
        )
        repository.insertGuideline(entity)
    }

    suspend fun updateGuideline(guidelineId: Int, description: String, maxRepetitions: Int) {
        val current = repository.getGuidelineById(guidelineId) ?: return
        repository.updateGuideline(current.copy(
            description        = description.trim(),
            // Clamp current so it never exceeds the new max
            currentRepetitions = current.currentRepetitions.coerceAtMost(maxRepetitions),
            maxRepetitions     = maxRepetitions
        ))
    }

    suspend fun updateGuidelineCurrentRepetitions(guidelineId: Int, currentRepetitions: Int) {
        val current = repository.getGuidelineById(guidelineId) ?: return
        repository.updateGuideline(current.copy(currentRepetitions = currentRepetitions))
    }

    suspend fun deleteGuideline(guidelineId: Int) =
        repository.deleteGuideline(guidelineId)

    suspend fun reorderGuidelines(orderedIds: List<Int>) {
        orderedIds.forEachIndexed { index, id ->
            repository.updateGuidelinePosition(id, index)
        }
    }

    // ── Linked thought ───────────────────────────────────────────────────

    suspend fun linkThought(guidelineId: Int, thoughtId: Int) {
        val current = repository.getGuidelineById(guidelineId) ?: return
        repository.updateGuideline(current.copy(thoughtId = thoughtId))
    }

    /**
     * Unlinks the thought from the guideline.
     * If [alsoDeleteThought] is true, the underlying thought is also deleted from THOUGHTS.
     */
    suspend fun unlinkThought(guidelineId: Int, alsoDeleteThought: Boolean) {
        val current = repository.getGuidelineById(guidelineId) ?: return
        val linkedId = current.thoughtId ?: return

        // Clear FK first (avoid orphan window even though SET_NULL would handle it)
        repository.updateGuideline(current.copy(thoughtId = null))

        if (alsoDeleteThought) {
            thoughtsService.deleteThoughtById(linkedId)
        }
    }

    /**
     * Returns the guideline that the given thought is linked to (along with goal context),
     * or null if no guideline references this thought.
     */
    suspend fun findGuidelineLinkedToThought(thoughtId: Int): GuidelineWithGoalDTO? {
        val guideline = repository.findGuidelineByThoughtId(thoughtId) ?: return null
        val goal = repository.getGoalById(guideline.goalId) ?: return null
        return GuidelineWithGoalDTO(
            guidelineId          = guideline.id,
            guidelineDescription = guideline.description,
            goalId               = goal.id,
            goalDescription      = goal.description,
            goalImportance       = goal.importance
        )
    }

    /**
     * Returns goals with their guidelines that have no linked thought yet
     */
    suspend fun getAvailableGuidelinesForLink(): List<GoalDTO> =
        getAllGoals()
            .map { goal -> goal.copy(guidelines = goal.guidelines.filter { it.thoughtId == null }) }
            .filter { it.guidelines.isNotEmpty() }

    /**
     * Links thought to guideline from the thought side.
     * If the thought was already linked to a different guideline, the previous link is cleared first
     */
    suspend fun linkThoughtToGuideline(thoughtId: Int, guidelineId: Int) {
        // Clear any previous link pointing to this thought ("defensive programming" type of protection)
        repository.findGuidelineByThoughtId(thoughtId)?.let { previous ->
            if (previous.id != guidelineId) {
                repository.updateGuideline(previous.copy(thoughtId = null))
            }
        }
        linkThought(guidelineId, thoughtId)
    }
}
