package pl.hexmind.mindshaper.database.models.relations

import androidx.room.Embedded
import androidx.room.Relation
import pl.hexmind.mindshaper.database.models.DomainEntity
import pl.hexmind.mindshaper.database.models.ThoughtEntity

/**
 * Data class to represent a Context with its associated Thought
 * Used for reverse relationship queries
 */
data class DomainWithThought(
    @Embedded val domain: DomainEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "domain_id"
    )
    val thoughts: List<ThoughtEntity>
)