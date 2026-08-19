package pl.hexmind.mindshaper.activities.workshop

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import pl.hexmind.mindshaper.database.models.GoalEntity
import pl.hexmind.mindshaper.services.GoalsService
import pl.hexmind.mindshaper.services.PathsService
import pl.hexmind.mindshaper.services.ThoughtsService
import pl.hexmind.mindshaper.services.dto.GoalDTO
import javax.inject.Inject

// ── UI models TODO: to be moved somewhere as separate class?

data class Goal(
    val id: Int,
    val description: String,
    val importance: Int,
    val subItems: List<GoalStep> = emptyList(),
    val lastModifiedAt: Long = System.currentTimeMillis()
)

data class GoalStep(
    val id: Int,
    val description: String,
    val currentRepetitions: Int = 0,
    val maxRepetitions: Int = 1,

    // Linked thought -> TODO: Change names to linkedThoughtId etc.?
    val thoughtId: Int? = null,
    val thoughtSubject: String? = null,

    val reminderTime: String? = null,
    val reminderDays: String? = null
) {
    // Completed when progress reaches the target; used for visual styling in the adapter
    val isCompleted: Boolean get() = currentRepetitions >= maxRepetitions

    val hasLinkedThought: Boolean get() = thoughtId != null
}

// Currently shown step
data class PathItem(
    val pathKey: String,
    val category: String,
    val status: String, // PathEntity.STATUS_*
    val currentStepIndex: Int,
    val totalSteps: Int,
    val currentStepContent: String,
    val isFirstStep: Boolean,
    val isLastStep: Boolean
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class WorkshopViewModel @Inject constructor(
    private val goalsService: GoalsService,
    private val pathsService: PathsService,
    private val thoughtsService: ThoughtsService
) : ViewModel() {


    // GOALS
    private val _goals = MutableLiveData<List<Goal>>()
    val goals: LiveData<List<Goal>> = _goals

    private val _archivedGoals = MutableLiveData<List<Goal>>()
    val archivedGoals: LiveData<List<Goal>> = _archivedGoals

    // PATHS
    private val _pickedPaths = MutableLiveData<List<PathItem>>()
    val pickedPaths: LiveData<List<PathItem>> = _pickedPaths

    // ONBOARDING
    suspend fun getNewestThoughtId(): Int? = thoughtsService.getNewestThoughtId()

    init {
        loadGoals()
        loadArchivedGoals()
        loadTodayPaths()
    }

    // ── Load ───────────────────────────────────────────────────────────────────

    private fun loadGoals() {
        viewModelScope.launch {
            // DB query already sorts by importance DESC, last_modified_at DESC
            _goals.value = goalsService.getAllGoals().map { dto -> dto.toUiModel() }
        }
    }

    private fun loadArchivedGoals() {
        viewModelScope.launch {
            // already sorted - newest change first
            _archivedGoals.value = goalsService.getArchivedGoals().map { dto -> dto.toUiModel() }
        }
    }

    fun reload() {
        loadGoals()
        loadArchivedGoals()
    }

    private fun loadTodayPaths() {
        viewModelScope.launch {
            pathsService.pickIfNeededOnStart()
            refreshPaths()
        }
    }

    private suspend fun refreshPaths() {
        _pickedPaths.value = pathsService.getTodayPaths().map { dto ->
            PathItem(
                pathKey            = dto.pathKey,
                category           = dto.category,
                status             = dto.status,
                currentStepIndex   = dto.currentStepIndex,
                totalSteps         = dto.totalSteps,
                currentStepContent = dto.currentStepContent,
                isFirstStep        = dto.isFirstStep,
                isLastStep         = dto.isLastStep
            )
        }
    }

    // ── Path actions ───────────────────────────────────────────────────────────

    fun revealPath(pathKey: String) {
        viewModelScope.launch {
            pathsService.revealPath(pathKey)
            refreshPaths()
        }
    }

    fun advanceToNextStep(pathKey: String) {
        viewModelScope.launch {
            pathsService.advanceToNextStep(pathKey)
            refreshPaths()
        }
    }

    fun resetAllPaths() {
        viewModelScope.launch {
            pathsService.resetAllPaths()
            refreshPaths()
        }
    }

    fun repickPath(pathKey: String) {
        viewModelScope.launch {
            pathsService.repickPath(pathKey)
            refreshPaths()
        }
    }

    // ── Goal actions ───────────────────────────────────────────────────────────

    fun cycleGoalImportance(goalId: Int) {
        val current = _goals.value?.firstOrNull { it.id == goalId } ?: return
        val next = if (current.importance >= 3) 1 else current.importance + 1 // TODO: Search and introduce IMPORTANCE_MAX_VALUE = 3
        // Optimistic update + re-sort in memory (mirrors DB ORDER BY importance DESC, last_modified_at DESC)
        _goals.value = _goals.value
            ?.map { goal ->
                if (goal.id == goalId) goal.copy(
                    importance     = next,
                    lastModifiedAt = System.currentTimeMillis()
                ) else goal
            }
            ?.let { sortGoals(it) }
        viewModelScope.launch { goalsService.updateGoalImportance(goalId, next) }
    }

    fun addGoal(description: String) {
        viewModelScope.launch {
            goalsService.addGoal(description)
            loadGoals() // re-fetch — DB returns already sorted
        }
    }

    fun deleteGoal(goalId: Int) {
        _goals.value = _goals.value?.filter { it.id != goalId }
        _archivedGoals.value = _archivedGoals.value?.filter { it.id != goalId }
        viewModelScope.launch { goalsService.deleteGoal(goalId) }
    }

    fun archiveGoal(goalId: Int) {
        moveGoal(goalId, GoalEntity.STATUS_ARCHIVED, IMPORTANCE_CLEARED)
    }

    fun restoreGoal(goalId: Int) {
        moveGoal(goalId, GoalEntity.STATUS_ACTIVE, IMPORTANCE_DEFAULT)
    }

    private fun moveGoal(goalId: Int, status: String, importance: Int) {
        viewModelScope.launch {
            goalsService.setGoalStatus(goalId, status, importance)
            // reload both lists when goal has changed its place
            loadGoals()
            loadArchivedGoals()
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private fun GoalDTO.toUiModel(): Goal =
        Goal(
            id             = id,
            description    = description,
            importance     = importance,
            lastModifiedAt = lastModifiedAt,
            subItems       = steps.map { step ->
                GoalStep(
                    id                 = step.id,
                    description        = step.description,
                    currentRepetitions = step.currentRepetitions,
                    maxRepetitions     = step.maxRepetitions,
                    thoughtId          = step.thoughtId
                )
            }
        )

    // Mirrors DB: ORDER BY importance DESC, last_modified_at DESC
    private fun sortGoals(list: List<Goal>): List<Goal> =
        list.sortedWith(compareByDescending<Goal> { it.importance }.thenByDescending { it.lastModifiedAt })

    companion object {
        private const val IMPORTANCE_CLEARED = 0
        private const val IMPORTANCE_DEFAULT = 1
    }
}
