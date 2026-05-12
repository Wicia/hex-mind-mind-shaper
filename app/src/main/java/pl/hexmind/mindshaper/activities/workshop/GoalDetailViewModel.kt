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
                    priority       = it.priority,
                    lastModifiedAt = it.lastModifiedAt,
                    subItems       = it.guidelines.map { guideline -> GoalGuideline(
                        id          = guideline.id,
                        description = guideline.description,
                        isDone      = guideline.isDone
                    )}
                )
            }
        }
    }

    // ── Goal header actions ────────────────────────────────────────────────────

    fun cycleGoalPriority() {
        val current = _goal.value ?: return
        val next = if (current.priority >= 3) 1 else current.priority + 1 // TODO: Search and introduce PRIORITY_MAX_VALUE = 3
        _goal.value = current.copy(priority = next)
        viewModelScope.launch { goalsService.updateGoalPriority(goalId, next) }
    }

    fun updateGoalDescription(description: String) {
        val current = _goal.value ?: return
        _goal.value = current.copy(description = description.trim())
        viewModelScope.launch { goalsService.updateGoalDescription(goalId, description) }
    }

    // ── Guideline actions ──────────────────────────────────────────────────────

    fun toggleGuideline(guidelineId: Int) {
        _goal.value = _goal.value?.let { goal ->
            goal.copy(subItems = goal.subItems.map { guideline ->
                if (guideline.id == guidelineId) guideline.copy(isDone = !guideline.isDone) else guideline
            })
        }
        viewModelScope.launch { goalsService.toggleGuidelineDone(guidelineId) }
    }

    fun updateGuideline(guidelineId: Int, description: String) {
        _goal.value = _goal.value?.let { goal ->
            goal.copy(subItems = goal.subItems.map { guideline ->
                if (guideline.id == guidelineId)
                    guideline.copy(description = description.trim())
                else
                    guideline
            })
        }
        viewModelScope.launch { goalsService.updateGuidelineDescription(guidelineId, description) }
    }

    fun deleteGuideline(guidelineId: Int) {
        _goal.value = _goal.value?.let { goal ->
            goal.copy(subItems = goal.subItems.filter { it.id != guidelineId })
        }
        viewModelScope.launch { goalsService.deleteGuideline(guidelineId) }
    }

    fun addGuideline(description: String) {
        viewModelScope.launch {
            goalsService.addGuideline(goalId, description)
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
