package pl.hexmind.mindshaper.activities.workshop

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import pl.hexmind.mindshaper.services.AppSettingsStorage
import pl.hexmind.mindshaper.services.CalendarService
import pl.hexmind.mindshaper.services.GoalsService
import pl.hexmind.mindshaper.services.PermissionService
import pl.hexmind.mindshaper.services.ThoughtsService
import javax.inject.Inject

@HiltViewModel
class GoalDetailViewModel @Inject constructor(
    private val goalsService: GoalsService,
    private val thoughtsService: ThoughtsService,
    private val calendarService: CalendarService,
    private val appSettingsStorage: AppSettingsStorage,
    private val permissionService: PermissionService,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Key matches GoalDetailActivity.EXTRA_GOAL_ID ("goalId")
    private val goalId: Int = savedStateHandle.get<Int>("goalId") ?: 0

    private val _goal = MutableLiveData<Goal?>()
    val goal: LiveData<Goal?> = _goal

    // Fires once when a calendar entry was actually created; the Activity clears it after showing the toast
    private val _calendarEntryCreated = MutableLiveData<Boolean?>()
    val calendarEntryCreated: LiveData<Boolean?> = _calendarEntryCreated

    fun onCalendarEntryToastShown() {
        _calendarEntryCreated.value = null
    }

    init {
        loadGoal()
    }

    // ── Load ───────────────────────────────────────────────────────────────────

    fun loadGoal() {
        viewModelScope.launch {
            val dto = goalsService.getGoal(goalId)
            if (dto == null) {
                _goal.value = null
                return@launch
            }

            // Fetch linked thoughts (lightweight: id + subject only, no BLOBs)
            val thoughtIds = dto.steps.mapNotNull { it.thoughtId }
            val subjectsMap: Map<Int, String?> = if (thoughtIds.isEmpty()) emptyMap()
                else thoughtsService.getSubjectsByIds(thoughtIds)

            val loadedSteps = dto.steps.map { g ->
                GoalStep(
                    id                 = g.id,
                    description        = g.description,
                    currentRepetitions = g.currentRepetitions,
                    maxRepetitions     = g.maxRepetitions,
                    thoughtId          = g.thoughtId,
                    thoughtSubject     = g.thoughtId?.let { subjectsMap[it] },
                    reminderTime       = g.reminderTime,
                    reminderDays       = g.reminderDays
                )
            }
            // Stable sort: false < true, so unfinished steps keep their order and finished ones land last
            val orderedSteps = loadedSteps.sortedBy { step -> step.isCompleted }

            _goal.value = Goal(
                id             = dto.id,
                description    = dto.description,
                importance     = dto.importance,
                lastModifiedAt = dto.lastModifiedAt,
                subItems       = orderedSteps
            )

            // Repairs order broken elsewhere: goals saved before the split, and new steps that
            // addStep puts at the very end (below the completed block)
            persistOrderIfChanged(loadedSteps, orderedSteps)
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

    // ── Step actions ──────────────────────────────────────────────────────────

    /**
     * Short tap on ring:
     * - Not completed yet -> increment by 1
     * - Already completed (current >= max) -> reset to 0
     */
    fun incrementStep(stepId: Int) {
        val step = _goal.value?.subItems?.firstOrNull { it.id == stepId } ?: return
        val newCurrent = if (step.isCompleted) 0 else step.currentRepetitions + 1
        updateStepCurrent(stepId, newCurrent)
    }

    /**
     * Long press on ring: step back by 1, min 0.
     */
    fun decrementStep(stepId: Int) {
        val step = _goal.value?.subItems?.firstOrNull { it.id == stepId } ?: return
        if (step.currentRepetitions == 0) return
        updateStepCurrent(stepId, step.currentRepetitions - 1)
    }

    private fun updateStepCurrent(stepId: Int, newCurrent: Int) {
        val current = _goal.value ?: return
        val updatedSteps = current.subItems.map { step ->
            if (step.id == stepId)
                step.copy(currentRepetitions = newCurrent)
            else step
        }
        val orderedSteps = moveStepToBoundary(updatedSteps, stepId)

        _goal.value = current.copy(subItems = orderedSteps)

        viewModelScope.launch { goalsService.updateStepCurrentRepetitions(stepId, newCurrent) }
        persistOrderIfChanged(updatedSteps, orderedSteps)
    }

    fun updateStep(stepId: Int, description: String, maxRepetitions: Int, reminderTime: String?, reminderDays: String?) {
        val current = _goal.value ?: return
        val updatedSteps = current.subItems.map { step ->
            if (step.id == stepId) step.copy(
                description        = description.trim(),
                // Clamp current so it never exceeds the new max
                currentRepetitions = if (maxRepetitions < step.maxRepetitions) 0
                    else step.currentRepetitions.coerceAtMost(maxRepetitions),
                maxRepetitions = maxRepetitions,
                reminderTime   = reminderTime,
                reminderDays   = reminderDays
            ) else step
        }
        // Editing maxRepetitions can flip completion, so the step may have to change block
        val orderedSteps = moveStepToBoundary(updatedSteps, stepId)

        _goal.value = current.copy(subItems = orderedSteps)

        viewModelScope.launch {
            // Recurrence rules can't be edited in place, so the old entry is replaced by a fresh one
            deleteReminderOf(stepId)
            val calendarEventId = createReminderIfPossible(description, maxRepetitions, reminderTime, reminderDays)
            if (calendarEventId != null) _calendarEntryCreated.value = true

            goalsService.updateStep(stepId, description, maxRepetitions, reminderTime, reminderDays, calendarEventId)
        }
        persistOrderIfChanged(updatedSteps, orderedSteps)
    }

    fun deleteStep(stepId: Int) {
        _goal.value = _goal.value?.let { goal ->
            goal.copy(subItems = goal.subItems.filter { it.id != stepId })
        }
        viewModelScope.launch {
            deleteReminderOf(stepId)
            goalsService.deleteStep(stepId)
        }
    }

    // ── Calendar reminders ────────────────────────────────

    private fun createReminderIfPossible(description: String, maxRepetitions: Int, reminderTime: String?, reminderDays: String?): Long? {
        // Feature off or reminder incomplete (null) -> any old entry gets dropped instead of orphaned
        if (!isCalendarReminderAllowed()) return null
        if (reminderTime == null || reminderDays == null) return null

        return calendarService.createRecurringReminder(
            description.trim(),
            reminderTime,
            reminderDays,
            maxRepetitions,
            appSettingsStorage.getCalendarTargetId()
        )
    }

    private suspend fun deleteReminderOf(stepId: Int) {
        val calendarEventId = goalsService.getStep(stepId)?.calendarEventId ?: return

        // the entry is removed even when the toggle is off, otherwise revoking the setting would strand events in the calendar
        if (permissionService.isCalendarGranted()) calendarService.deleteReminder(calendarEventId)
    }

    private fun isCalendarReminderAllowed(): Boolean =
        appSettingsStorage.isCalendarRemindersEnabled() && permissionService.isCalendarGranted()

    fun hasCalendarReminder(stepId: Int): Boolean =
        _goal.value?.subItems?.firstOrNull { step -> step.id == stepId }?.reminderTime != null

    fun addStep(description: String, maxRepetitions: Int, reminderTime: String?, reminderDays: String?) {
        viewModelScope.launch {
            val calendarEventId = createReminderIfPossible(description, maxRepetitions, reminderTime, reminderDays)
            if (calendarEventId != null) _calendarEntryCreated.value = true

            goalsService.addStep(goalId, description, maxRepetitions, reminderTime, reminderDays, calendarEventId)
            loadGoal()
        }
    }

    fun quickCompleteAll(): Boolean {
        val current = _goal.value ?: return false
        val anyChanged = current.subItems.any { !it.isCompleted }
        if (!anyChanged) return false

        _goal.value = current.copy(
            subItems = current.subItems.map { step ->
                step.copy(currentRepetitions = step.maxRepetitions)
            }
        )
        viewModelScope.launch {
            current.subItems
                .filter { !it.isCompleted }
                .forEach { step ->
                    goalsService.updateStepCurrentRepetitions(step.id, step.maxRepetitions)
                }
        }
        return true
    }

    // ── Completion order ──────────────────────────────────

    /**
     * Puts the step on the border between the unfinished and the finished block:
     * just finished -> head of the finished ones, back in progress -> tail of the unfinished ones.
     */
    private fun moveStepToBoundary(steps: List<GoalStep>, stepId: Int): List<GoalStep> {
        val movedStep = steps.firstOrNull { step -> step.id == stepId } ?: return steps
        val remainingSteps = steps.filter { step -> step.id != stepId }
        val boundaryIndex = remainingSteps.count { step -> !step.isCompleted }

        return remainingSteps.toMutableList().apply { add(boundaryIndex, movedStep) }
    }

    private fun persistOrderIfChanged(before: List<GoalStep>, after: List<GoalStep>) {
        val orderedIds = after.map { step -> step.id }
        if (before.map { step -> step.id } == orderedIds) return

        viewModelScope.launch { goalsService.reorderSteps(orderedIds) }
    }

    // ── Reorder steps ─────────────────────────────────────

    fun moveStepUp(stepId: Int) = moveStep(stepId, -1)
    fun moveStepDown(stepId: Int) = moveStep(stepId, +1)

    private fun moveStep(stepId: Int, direction: Int) {
        val current = _goal.value ?: return
        val items = current.subItems.toMutableList()
        val idx = items.indexOfFirst { it.id == stepId }
        val to = idx + direction
        if (idx < 0 || to < 0 || to >= items.size) return

        // Swap in memory
        val moved = items.removeAt(idx)
        items.add(to, moved)
        _goal.value = current.copy(subItems = items)

        // Persist the new full order
        val orderedIds = items.map { it.id }
        viewModelScope.launch { goalsService.reorderSteps(orderedIds) }
    }

    // ── Linked thought (1:1) ───────────────────────────────────────────────────

    fun linkThought(stepId: Int, thoughtId: Int) {
        viewModelScope.launch {
            goalsService.linkThought(stepId, thoughtId)
            loadGoal()  // refresh = thought chip refresh
        }
    }

    /**
     * Handles actions "unpin only" or "unpin + delete thought".
     */
    fun unlinkThought(stepId: Int, alsoDeleteThought: Boolean) {
        // Optimistic UI: clear thoughtId + subject in the affected step
        _goal.value = _goal.value?.let { goal ->
            goal.copy(subItems = goal.subItems.map {
                if (it.id == stepId) it.copy(thoughtId = null, thoughtSubject = null)
                else it
            })
        }
        viewModelScope.launch { goalsService.unlinkThought(stepId, alsoDeleteThought) }
    }
}
