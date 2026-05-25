package pl.hexmind.mindshaper.database.models

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Room POJO: single query result combining a goal with all its steps.
 * Used internally by GoalDao — not exposed outside the database layer.
 */
data class GoalWithSteps(

    @Embedded
    val goal: GoalEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "goal_id",
        entity = StepEntity::class
    )
    val steps: List<StepEntity>
)