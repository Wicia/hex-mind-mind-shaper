package pl.hexmind.mindshaper.services

import pl.hexmind.mindshaper.database.models.PathEntity
import pl.hexmind.mindshaper.database.repositories.PathRepository
import pl.hexmind.mindshaper.services.dto.PathDTO
import pl.hexmind.mindshaper.services.mappers.PathMapper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PathsService @Inject constructor(
    private val repository: PathRepository
) {
    companion object {
        private const val MAX_DAILY_PATHS = 2
    }

    // ── Picking paths ─────────────────────────────────────────────────────────────

    suspend fun pickIfNeededOnStart() {
        val today = todayEpochDay()
        val activeCount = repository.getActiveTodayWithSteps(today).size
        val needed = MAX_DAILY_PATHS - activeCount
        repeat(needed) { pickNewPath() }
    }

    /**
     * Pick one more path from the pool.
     * @return true if a path was picked, false if the paths pool is empty
     */
    suspend fun pickNewPath(): Boolean {
        val today = todayEpochDay()
        val candidate = repository.getRandomPickable(today) ?: return false
        repository.updatePath(
            candidate.copy(
                lastDrawnDate = today,
                status = PathEntity.STATUS_UNSELECTED,
                currentStepIndex = 0
            )
        )
        return true
    }

    // ── Loading paths ───────────────────────────────────────────────────────────

    suspend fun getTodayPaths(): List<PathDTO> =
        repository.getActiveTodayWithSteps(todayEpochDay()).map { PathMapper.toDTO(it) }

    // ── State transitions ──────────────────────────────────────────────────────

    suspend fun revealPath(pathKey: String) {
        val path = repository.getPathByKey(pathKey) ?: return
        if (path.status == PathEntity.STATUS_UNSELECTED) {
            repository.updatePath(path.copy(status = PathEntity.STATUS_STARTED))
        }
    }

    suspend fun advanceToNextStep(pathKey: String) {
        val path = repository.getPathByKey(pathKey) ?: return
        val totalSteps = repository.countSteps(pathKey)
        val nextIndex = path.currentStepIndex + 1

        if (nextIndex >= totalSteps) {
            // Last step completed → COMPLETED, auto-draw a replacement
            repository.updatePath(path.copy(status = PathEntity.STATUS_COMPLETED))
            pickNewPath()
        } 
        else {
            repository.updatePath(path.copy(currentStepIndex = nextIndex))
        }
    }

    suspend fun repickPath(pathKey: String) {
        val path = repository.getPathByKey(pathKey) ?: return
        // Return to pool: clear drawn date and reset progress
        repository.updatePath(
            path.copy(
                lastDrawnDate = null,
                status = PathEntity.STATUS_UNSELECTED,
                currentStepIndex = 0
            )
        )
        pickNewPath()
    }

    // ── Helper ─────────────────────────────────────────────────────────────────

    private fun todayEpochDay(): Long =
        System.currentTimeMillis() / (24L * 60 * 60 * 1000)
}
