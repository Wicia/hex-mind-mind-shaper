package pl.hexmind.mindshaper.services

import pl.hexmind.mindshaper.common.dormant.ThoughtState
import pl.hexmind.mindshaper.services.AppSettingsStorage.Companion.DORMANT_DAYS_MIN
import pl.hexmind.mindshaper.services.dto.ThoughtDTO
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * Computes the current state of a thought based on slow mode and dormant mode settings.
 */
class ThoughtStatusService @Inject constructor(
    private val appSettingsStorage: AppSettingsStorage
) {
    fun computeState(thought: ThoughtDTO): ThoughtState {
        if (isLocked(thought)) return ThoughtState.LOCKED
        if (!appSettingsStorage.isDormantModeEnabled()) return ThoughtState.ACTIVE
        if (thought.value > appSettingsStorage.getDormantValueThreshold()) return ThoughtState.ACTIVE

        val daysSinceUpdate  = ChronoUnit.DAYS.between(thought.updatedAt, Instant.now())
        val daysThreshold    = appSettingsStorage.getDormantDaysThreshold().toLong()
        val warningThreshold = maxOf(0L, daysThreshold - DORMANT_DAYS_MIN)

        return when {
            daysSinceUpdate >= daysThreshold    -> ThoughtState.DORMANT
            daysSinceUpdate >= warningThreshold -> ThoughtState.WARNING
            else                               -> ThoughtState.ACTIVE
        }
    }

    fun isLocked(thought: ThoughtDTO): Boolean {
        if (!appSettingsStorage.isSlowModeEnabled()) return false
        val elapsed = Duration.between(thought.createdAt, Instant.now()).toHours()
        return elapsed < appSettingsStorage.getSlowModeHours()
    }
}