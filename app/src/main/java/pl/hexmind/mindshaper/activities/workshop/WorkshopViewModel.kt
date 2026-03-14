package pl.hexmind.mindshaper.activities.workshop

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import pl.hexmind.mindshaper.services.GoalsService
import javax.inject.Inject

// ── UI models TODO: to be moved somewhere as separate class

data class Goal(
    val id: Int,
    val description: String,
    val priority: Int,
    val subItems: List<GoalGuideline> = emptyList(),
    val isExpanded: Boolean = false,
    val lastModifiedAt: Long = System.currentTimeMillis()
)

data class GoalGuideline(
    val id: Int,
    val description: String,
    val isDone: Boolean = false
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class WorkshopViewModel @Inject constructor(
    private val goalsService: GoalsService
) : ViewModel() {

    private val _goals = MutableLiveData<List<Goal>>()
    val goals: LiveData<List<Goal>> = _goals

    init {
        loadGoals()
    }

    // ── Load ───────────────────────────────────────────────────────────────────

    private fun loadGoals(preserveExpanded: Set<Int> = emptySet()) {
        viewModelScope.launch {
            _goals.value = goalsService.getAllGoals().map { dto ->
                Goal(
                    id = dto.id,
                    description = dto.description,
                    priority = dto.priority,
                    lastModifiedAt = dto.lastModifiedAt,
                    isExpanded = dto.id in preserveExpanded,
                    subItems = dto.guidelines.map { g ->
                        GoalGuideline(id = g.id, description = g.description, isDone = g.isDone)
                    }
                )
            }
        }
    }

    // ── Goal actions ───────────────────────────────────────────────────────────

    fun toggleGoalExpanded(goalId: Int) {
        _goals.value = _goals.value?.map { goal ->
            if (goal.id == goalId) goal.copy(isExpanded = !goal.isExpanded) else goal
        }
    }

    fun cycleGoalPriority(goalId: Int) {
        val current = _goals.value?.firstOrNull { it.id == goalId } ?: return
        val next = if (current.priority >= 3) 1 else current.priority + 1

        // Optimistic update
        _goals.value = _goals.value?.map { goal ->
            if (goal.id == goalId) goal.copy(priority = next, lastModifiedAt = System.currentTimeMillis())
            else goal
        }
        viewModelScope.launch {
            goalsService.updateGoalPriority(goalId, next)
        }
    }

    fun updateGoalDescription(goalId: Int, description: String) {
        // Optimistic update
        _goals.value = _goals.value?.map { goal ->
            if (goal.id == goalId)
                goal.copy(description = description.trim(), lastModifiedAt = System.currentTimeMillis())
            else goal
        }
        viewModelScope.launch {
            goalsService.updateGoalDescription(goalId, description)
        }
    }

    fun addGoal(description: String) {
        viewModelScope.launch {
            goalsService.addGoal(description)
            loadGoals(expandedIds())
        }
    }

    fun deleteGoal(goalId: Int) {
        _goals.value = _goals.value?.filter { it.id != goalId }
        viewModelScope.launch {
            goalsService.deleteGoal(goalId)
        }
    }

    fun sortGoals() {
        _goals.value = _goals.value?.sortedWith(
            compareBy<Goal> { it.priority }.thenByDescending { it.lastModifiedAt }
        )
    }

    // ── Guideline actions ──────────────────────────────────────────────────────

    fun toggleSubItemDone(goalId: Int, guidelineId: Int) {
        _goals.value = _goals.value?.map { goal ->
            if (goal.id != goalId) goal
            else goal.copy(subItems = goal.subItems.map { g ->
                if (g.id == guidelineId) g.copy(isDone = !g.isDone) else g
            })
        }
        viewModelScope.launch {
            goalsService.toggleGuidelineDone(guidelineId)
        }
    }

    fun updateSubItemDescription(goalId: Int, guidelineId: Int, description: String) {
        _goals.value = _goals.value?.map { goal ->
            if (goal.id != goalId) goal
            else goal.copy(subItems = goal.subItems.map { g ->
                if (g.id == guidelineId) g.copy(description = description.trim()) else g
            })
        }
        viewModelScope.launch {
            goalsService.updateGuidelineDescription(guidelineId, description)
        }
    }

    fun addSubItem(goalId: Int, description: String) {
        viewModelScope.launch {
            goalsService.addGuideline(goalId, description)
            loadGoals(expandedIds())
        }
    }

    fun deleteSubItem(goalId: Int, guidelineId: Int) {
        _goals.value = _goals.value?.map { goal ->
            if (goal.id != goalId) goal
            else goal.copy(subItems = goal.subItems.filter { it.id != guidelineId })
        }
        viewModelScope.launch {
            goalsService.deleteGuideline(guidelineId)
        }
    }

    fun reorderSubItems(goalId: Int, from: Int, to: Int) {
        val current = _goals.value?.firstOrNull { it.id == goalId } ?: return
        val mutable = current.subItems.toMutableList()
        val moved = mutable.removeAt(from)
        mutable.add(to, moved)
        _goals.value = _goals.value?.map { goal ->
            if (goal.id != goalId) goal else goal.copy(subItems = mutable)
        }
        viewModelScope.launch {
            goalsService.reorderGuidelines(mutable.map { it.id })
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private fun expandedIds(): Set<Int> =
        _goals.value?.filter { it.isExpanded }?.map { it.id }?.toSet() ?: emptySet()
}