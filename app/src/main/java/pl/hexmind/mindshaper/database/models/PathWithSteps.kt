package pl.hexmind.mindshaper.database.models

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Room POJO: path + all its steps in a single @Transaction query.
 */
data class PathWithSteps(

    @Embedded
    val path: PathEntity,

    @Relation(
        parentColumn = "path_key",
        entityColumn = "path_key",
        entity = PathStepEntity::class
    )
    val steps: List<PathStepEntity>
)
