package pl.hexmind.mindshaper.database.models

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Room POJO: single query result combining a goal with all its guidelines.
 * Used internally by GoalDao — not exposed outside the database layer.
 */
data class GoalWithGuidelines(

    @Embedded
    val goal: GoalEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "goal_id",
        entity = GuidelineEntity::class
    )
    val guidelines: List<GuidelineEntity>
)