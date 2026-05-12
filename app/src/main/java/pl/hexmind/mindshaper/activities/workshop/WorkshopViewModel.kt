package pl.hexmind.mindshaper.activities.workshop

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import pl.hexmind.mindshaper.services.GoalsService
import pl.hexmind.mindshaper.services.PathsService
import javax.inject.Inject

// ── UI models TODO: to be moved somewhere as separate class?

data class Goal(
    val id: Int,
    val description: String,
    val priority: Int,
    val subItems: List<GoalGuideline> = emptyList(),
    val lastModifiedAt: Long = System.currentTimeMillis()
)

data class GoalGuideline(
    val id: Int,
    val description: String,
    val isDone: Boolean = false
)

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
    private val pathsService: PathsService
) : ViewModel() {

    // GOALS
    private val _goals = MutableLiveData<List<Goal>>()
    val goals: LiveData<List<Goal>> = _goals

    // PATHS
    private val _pickedPaths = MutableLiveData<List<PathItem>>()
    val pickedPaths: LiveData<List<PathItem>> = _pickedPaths

    init {
        loadGoals()
        loadTodayPaths()
    }

    // ── Load ───────────────────────────────────────────────────────────────────

    private fun loadGoals() {
        viewModelScope.launch {
            _goals.value = goalsService.getAllGoals().map { dto ->
                Goal(
                    id             = dto.id,
                    description    = dto.description,
                    priority       = dto.priority,
                    lastModifiedAt = dto.lastModifiedAt,
                    subItems       = dto.guidelines.map { g ->
                        GoalGuideline(id = g.id, description = g.description, isDone = g.isDone)
                    }
                )
            }
        }
    }

    fun reload() {
        loadGoals()
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

    fun repickPath(pathKey: String) {
        viewModelScope.launch {
            pathsService.repickPath(pathKey)
            refreshPaths()
        }
    }

    // ── Goal actions ───────────────────────────────────────────────────────────

    fun cycleGoalPriority(goalId: Int) {
        val current = _goals.value?.firstOrNull { it.id == goalId } ?: return
        val next = if (current.priority >= 3) 1 else current.priority + 1 // TODO: Search and introduce PRIORITY_MAX_VALUE = 3
        // Optimistic update + re-sort in memory (same logic as in DB query)
        _goals.value = _goals.value
            ?.map { goal ->
                if (goal.id == goalId) goal.copy(
                    priority       = next,
                    lastModifiedAt = System.currentTimeMillis()
                ) else goal
            }
            ?.let { sortGoals(it) }
        viewModelScope.launch { goalsService.updateGoalPriority(goalId, next) }
    }

    fun addGoal(description: String) {
        viewModelScope.launch {
            goalsService.addGoal(description)
            loadGoals() // re-fetch — DB returns already sorted
        }
    }

    fun deleteGoal(goalId: Int) {
        _goals.value = _goals.value?.filter { it.id != goalId }
        viewModelScope.launch { goalsService.deleteGoal(goalId) }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    // Mirrors DB query / logic
    private fun sortGoals(list: List<Goal>): List<Goal> =
        list.sortedWith(compareBy<Goal> { it.priority }.thenByDescending { it.lastModifiedAt })
}