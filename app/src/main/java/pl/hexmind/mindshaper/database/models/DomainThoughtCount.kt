package pl.hexmind.mindshaper.database.models

import androidx.room.ColumnInfo

/**
 * A domain paired with how many thoughts carry it - the count on a domain tile in Metadata.
 */
data class DomainThoughtCount(
    @ColumnInfo(name = "domain_id")
    val domainId: Int,

    @ColumnInfo(name = "thought_count")
    val thoughtCount: Int
)
