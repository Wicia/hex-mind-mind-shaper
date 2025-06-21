package pl.hexmind.fastnote.data.models.relations

import androidx.room.Embedded
import androidx.room.Relation
import pl.hexmind.fastnote.data.models.AreaEntity
import pl.hexmind.fastnote.data.models.ThoughtEntity

/**
 * Data class to represent a Context with its associated Thought
 * Used for reverse relationship queries
 */
data class AreaWithThought(
    @Embedded val area: AreaEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "area_id"
    )
    val thoughts: List<ThoughtEntity>
)