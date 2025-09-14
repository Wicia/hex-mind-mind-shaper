package pl.hexmind.mindshaper.database.models.relations

import androidx.room.Embedded
import androidx.room.Relation
import pl.hexmind.mindshaper.database.models.DomainEntity
import pl.hexmind.mindshaper.database.models.ThoughtEntity

/**
 * Data class to represent a Thought with its associated Context
 * Used for querying joined data
 */
data class ThoughtWithDomain(
    @Embedded val thought: ThoughtEntity,
    @Relation(
        parentColumn = "domain_id",
        entityColumn = "id"
    )
    val domain: DomainEntity?
)