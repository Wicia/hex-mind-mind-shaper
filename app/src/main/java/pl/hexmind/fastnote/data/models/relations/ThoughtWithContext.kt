package pl.hexmind.fastnote.data.models.relations

import androidx.room.Embedded
import androidx.room.Relation
import pl.hexmind.fastnote.data.models.ContextEntity
import pl.hexmind.fastnote.data.models.ThoughtEntity

/**
 * Data class to represent a Thought with its associated Context
 * Used for querying joined data
 */
data class ThoughtWithContext(
    @Embedded val thought: ThoughtEntity,
    @Relation(
        parentColumn = "context_id",
        entityColumn = "id"
    )
    val context: ContextEntity
)