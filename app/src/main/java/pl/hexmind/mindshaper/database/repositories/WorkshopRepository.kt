package pl.hexmind.mindshaper.database.repositories

import pl.hexmind.mindshaper.database.models.GoalEntity
import pl.hexmind.mindshaper.database.models.GoalWithGuidelines
import pl.hexmind.mindshaper.database.models.GuidelineEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository combining workshop-related DAOs
 */
@Singleton
class WorkshopRepository @Inject constructor(
    private val goalDao: GoalDAO,
    private val guidelineDao: GuidelineDAO
) {

    // ── Goals ──────────────────────────────────────────────────────────────────

    suspend fun getAllGoalsWithGuidelines(): List<GoalWithGuidelines> =
        goalDao.getAllWithGuidelines()

    suspend fun insertGoal(entity: GoalEntity): Long =
        goalDao.insert(entity)

    suspend fun updateGoal(entity: GoalEntity) =
        goalDao.update(entity)

    suspend fun getGoalById(goalId: Int): GoalEntity? =
        goalDao.getById(goalId)

    suspend fun deleteGoal(goalId: Int) =
        goalDao.deleteById(goalId)

    // ── Guidelines ─────────────────────────────────────────────────────────────

    suspend fun insertGuideline(entity: GuidelineEntity): Long =
        guidelineDao.insert(entity)

    suspend fun updateGuideline(entity: GuidelineEntity) =
        guidelineDao.update(entity)

    suspend fun getGuidelineById(guidelineId: Int): GuidelineEntity? =
        guidelineDao.getById(guidelineId)

    suspend fun getGuidelinesByGoalId(goalId: Int): List<GuidelineEntity> =
        guidelineDao.getByGoalId(goalId)

    suspend fun deleteGuideline(guidelineId: Int) =
        guidelineDao.deleteById(guidelineId)

    suspend fun updateGuidelinePosition(id: Int, position: Int) =
        guidelineDao.updatePosition(id, position)

    suspend fun findGuidelineByThoughtId(thoughtId: Int): GuidelineEntity? =
        guidelineDao.findByThoughtId(thoughtId)
}