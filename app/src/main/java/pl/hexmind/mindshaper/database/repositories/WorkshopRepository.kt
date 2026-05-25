package pl.hexmind.mindshaper.database.repositories

import pl.hexmind.mindshaper.database.models.GoalEntity
import pl.hexmind.mindshaper.database.models.GoalWithSteps
import pl.hexmind.mindshaper.database.models.StepEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository combining workshop-related DAOs
 */
@Singleton
class WorkshopRepository @Inject constructor(
    private val goalDao: GoalDAO,
    private val stepDao: StepDAO
) {

    // ── Goals ──────────────────────────────────────────────────────────────────

    suspend fun getAllGoalsWithSteps(): List<GoalWithSteps> =
        goalDao.getAllWithSteps()

    suspend fun insertGoal(entity: GoalEntity): Long =
        goalDao.insert(entity)

    suspend fun updateGoal(entity: GoalEntity) =
        goalDao.update(entity)

    suspend fun getGoalById(goalId: Int): GoalEntity? =
        goalDao.getById(goalId)

    suspend fun deleteGoal(goalId: Int) =
        goalDao.deleteById(goalId)

    // ── Steps ─────────────────────────────────────────────────────────────────

    suspend fun insertStep(entity: StepEntity): Long =
        stepDao.insert(entity)

    suspend fun updateStep(entity: StepEntity) =
        stepDao.update(entity)

    suspend fun getStepById(stepId: Int): StepEntity? =
        stepDao.getById(stepId)

    suspend fun getStepsByGoalId(goalId: Int): List<StepEntity> =
        stepDao.getByGoalId(goalId)

    suspend fun deleteStep(stepId: Int) =
        stepDao.deleteById(stepId)

    suspend fun updateStepPosition(id: Int, position: Int) =
        stepDao.updatePosition(id, position)

    suspend fun findStepByThoughtId(thoughtId: Int): StepEntity? =
        stepDao.findByThoughtId(thoughtId)
}