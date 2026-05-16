package pl.hexmind.mindshaper.activities.workshop

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import pl.hexmind.mindshaper.services.GoalsService
import javax.inject.Inject

@HiltViewModel
class GoalDetailViewModel @Inject constructor(
    private val goalsService: GoalsService,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Key matches GoalDetailActivity.EXTRA_GOAL_ID ("goalId")
    private val goalId: Int = savedStateHandle.get<Int>("goalId") ?: 0

    private val _goal = MutableLiveData<Goal?>()
    val goal: LiveData<Goal?> = _goal

    init {
        loadGoal()
    }

    // ── Load ───────────────────────────────────────────────────────────────────

    fun loadGoal() {
        viewModelScope.launch {
            val dto = goalsService.getAllGoals().firstOrNull { it.id == goalId }
            _goal.value = dto?.let {
                Goal(
                    id             = it.id,
                    description    = it.description,
                    importance     = it.importance,
                    lastModifiedAt = it.lastModifiedAt,
                    subItems       = it.guidelines.map { g ->
                        GoalGuideline(
                            id                 = g.id,
                            description        = g.description,
                            currentRepetitions = g.currentRepetitions,
                            maxRepetitions     = g.maxRepetitions
                        )
                    }
                )
            }
        }
    }

    // ── Goal header actions ────────────────────────────────────────────────────

    fun cycleGoalImportance() {
        val current = _goal.value ?: return
        val next = if (current.importance >= 3) 1 else current.importance + 1 // TODO: Search and introduce IMPORTANCE_MAX_VALUE = 3
        _goal.value = current.copy(importance = next)
        viewModelScope.launch { goalsService.updateGoalImportance(goalId, next) }
    }

    fun updateGoalDescription(description: String) {
        val current = _goal.value ?: return
        _goal.value = current.copy(description = description.trim())
        viewModelScope.launch { goalsService.updateGoalDescription(goalId, description) }
    }

    // ── Guideline actions ──────────────────────────────────────────────────────

    /**
     * Short tap on ring:
     * - Not completed yet -> increment by 1
     * - Already completed (current >= max) -> reset to 0
     */
    fun incrementGuideline(guidelineId: Int) {
        val guideline = _goal.value?.subItems?.firstOrNull { it.id == guidelineId } ?: return
        val newCurrent = if (guideline.isCompleted) 0 else guideline.currentRepetitions + 1
        updateGuidelineCurrent(guidelineId, newCurrent)
    }

    /**
     * Long press on ring: step back by 1, min 0.
     */
    fun decrementGuideline(guidelineId: Int) {
        val guideline = _goal.value?.subItems?.firstOrNull { it.id == guidelineId } ?: return
        if (guideline.currentRepetitions == 0) return
        updateGuidelineCurrent(guidelineId, guideline.currentRepetitions - 1)
    }

    private fun updateGuidelineCurrent(guidelineId: Int, newCurrent: Int) {
        _goal.value = _goal.value?.let { goal ->
            goal.copy(subItems = goal.subItems.map { guideline ->
                if (guideline.id == guidelineId)
                    guideline.copy(currentRepetitions = newCurrent)
                else guideline
            })
        }
        viewModelScope.launch { goalsService.updateGuidelineCurrentRepetitions(guidelineId, newCurrent) }
    }

    fun updateGuideline(guidelineId: Int, description: String, maxRepetitions: Int) {
        _goal.value = _goal.value?.let { goal ->
            goal.copy(subItems = goal.subItems.map { guideline ->
                if (guideline.id == guidelineId) guideline.copy(
                    description        = description.trim(),
                    // Clamp current so it never exceeds the new max
                    currentRepetitions = if (maxRepetitions < guideline.maxRepetitions) 0
                        else guideline.currentRepetitions.coerceAtMost(maxRepetitions),
                    maxRepetitions = maxRepetitions
                ) else guideline
            })
        }
        viewModelScope.launch { goalsService.updateGuideline(guidelineId, description, maxRepetitions) }
    }

    fun deleteGuideline(guidelineId: Int) {
        _goal.value = _goal.value?.let { goal ->
            goal.copy(subItems = goal.subItems.filter { it.id != guidelineId })
        }
        viewModelScope.launch { goalsService.deleteGuideline(guidelineId) }
    }

    fun addGuideline(description: String, maxRepetitions: Int) {
        viewModelScope.launch {
            goalsService.addGuideline(goalId, description, maxRepetitions)
            loadGoal()
        }
    }

    // Called from GoalDetailActivity after drag ends (clearView) — adapter owns the visual order during drag
    fun persistReorder(orderedIds: List<Int>) {
        // Sync _goal.subItems to reflect the new order so LiveData stays consistent
        val current = _goal.value ?: return
        val reordered = orderedIds.mapNotNull { id -> current.subItems.firstOrNull { it.id == id } }
        _goal.value = current.copy(subItems = reordered)
        viewModelScope.launch { goalsService.reorderGuidelines(orderedIds) }
    }
}
