package pl.hexmind.mindshaper.activities.workshop

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

// ── Data models ───────────────────────────────────────────────────────────────

data class Goal(
    val id: Int,
    val description: String,
    val priority: Int, // 3 -> 1 (highest)
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

class WorkshopViewModel : ViewModel() {

    private val _goals = MutableLiveData<List<Goal>>()
    val goals: LiveData<List<Goal>> = _goals

    // Simple counters for generating unique IDs until Room is integrated (TODO: to be removed later)
    private var nextGoalId = 100
    private var nextSubItemId = 200

    init {
        loadMockData()
    }

    // ── Mock data ──────────────────────────────────────────────────────────────

    private fun loadMockData() {
        // TODO: replace with Room DAO calls when database layer is ready
        _goals.value = listOf(
            Goal(
                id = 1,
                description = "Wdrożyć system powtórek spaced repetition",
                priority = 1,
                subItems = listOf(
                    GoalGuideline(1, "Zaprojektować model algorytmu SM-2"),
                    GoalGuideline(2, "Stworzyć encję w bazie danych", isDone = true),
                    GoalGuideline(3, "Dodać widok kolejki powtórek", isDone = true)
                )
            ),
            Goal(
                id = 2,
                description = "Ocena jakości myśli (signal/noise)",
                priority = 2,
                subItems = listOf(
                    GoalGuideline(4, "Zdefiniować kryteria oceny"),
                    GoalGuideline(5, "UI slidera oceny w widoku myśli")
                )
            ),
            Goal(
                id = 3,
                description = "Eksport danych do PDF / Markdown",
                priority = 3,
                subItems = listOf(
                    GoalGuideline(6, "Biblioteka do generowania PDF"),
                    GoalGuideline(7, "Format eksportu Markdown"),
                    GoalGuideline(8, "Analiza wymagań eksportu", isDone = true)
                )
            )
        )
    }

    // ── Goal actions ───────────────────────────────────────────────────────────

    /** Toggles expanded/collapsed state. Only used outside edit mode. */
    fun toggleGoalExpanded(goalId: Int) {
        _goals.value = _goals.value?.map { goal ->
            if (goal.id == goalId) goal.copy(isExpanded = !goal.isExpanded) else goal
        }
    }

    /** Cycles priority 1→2→3→1. Works in both modes. */
    fun cycleGoalPriority(goalId: Int) {
        _goals.value = _goals.value?.map { goal ->
            if (goal.id == goalId) {
                val next = if (goal.priority >= 3) 1 else goal.priority + 1
                goal.copy(priority = next, lastModifiedAt = System.currentTimeMillis())
            } else goal
        }
    }

    /** Updates goal description. Trims whitespace. */
    fun updateGoalDescription(goalId: Int, description: String) {
        if (description.isBlank()) return
        _goals.value = _goals.value?.map { goal ->
            if (goal.id == goalId)
                goal.copy(description = description.trim(), lastModifiedAt = System.currentTimeMillis())
            else goal
        }
    }

    /** Adds a new goal with default priority 3. */
    fun addGoal(description: String) {
        if (description.isBlank()) return
        val newGoal = Goal(
            id = nextGoalId++,
            description = description.trim(),
            priority = 3,
            lastModifiedAt = System.currentTimeMillis()
        )
        _goals.value = (_goals.value ?: emptyList()) + newGoal
    }

    /** Removes a goal and all its sub-items. */
    fun deleteGoal(goalId: Int) {
        _goals.value = _goals.value?.filter { it.id != goalId }
    }

    /**
     * Sorts goals: priority ASC (1 first), then lastModifiedAt DESC.
     * Called once when the user taps the tick button to exit edit mode.
     */
    fun sortGoals() {
        _goals.value = _goals.value?.sortedWith(
            compareBy<Goal> { it.priority }
                .thenByDescending { it.lastModifiedAt }
        )
    }

    // ── Sub-item actions ───────────────────────────────────────────────────────

    /** Toggles done state. Only used outside edit mode. */
    fun toggleSubItemDone(goalId: Int, subItemId: Int) {
        _goals.value = _goals.value?.map { goal ->
            if (goal.id != goalId) goal
            else goal.copy(
                subItems = goal.subItems.map { sub ->
                    if (sub.id == subItemId) sub.copy(isDone = !sub.isDone) else sub
                }
            )
        }
    }

    /** Updates sub-item description text. */
    fun updateSubItemDescription(goalId: Int, subItemId: Int, description: String) {
        if (description.isBlank()) return
        _goals.value = _goals.value?.map { goal ->
            if (goal.id != goalId) goal
            else goal.copy(
                subItems = goal.subItems.map { sub ->
                    if (sub.id == subItemId) sub.copy(description = description.trim()) else sub
                },
                lastModifiedAt = System.currentTimeMillis()
            )
        }
    }

    /** Adds a new sub-item to a goal. */
    fun addSubItem(goalId: Int, description: String) {
        if (description.isBlank()) return
        _goals.value = _goals.value?.map { goal ->
            if (goal.id != goalId) goal
            else goal.copy(
                subItems = goal.subItems + GoalGuideline(
                    id = nextSubItemId++,
                    description = description.trim()
                ),
                lastModifiedAt = System.currentTimeMillis()
            )
        }
    }

    /** Removes a sub-item from a goal. */
    fun deleteSubItem(goalId: Int, subItemId: Int) {
        _goals.value = _goals.value?.map { goal ->
            if (goal.id != goalId) goal
            else goal.copy(subItems = goal.subItems.filter { it.id != subItemId })
        }
    }

    /**
     * Reorders sub-items within a goal after drag & drop.
     * [from] and [to] are adapter positions within the sub-items list.
     */
    fun reorderSubItems(goalId: Int, from: Int, to: Int) {
        _goals.value = _goals.value?.map { goal ->
            if (goal.id != goalId) goal
            else {
                val mutable = goal.subItems.toMutableList()
                val moved = mutable.removeAt(from)
                mutable.add(to, moved)
                goal.copy(subItems = mutable)
            }
        }
    }
}