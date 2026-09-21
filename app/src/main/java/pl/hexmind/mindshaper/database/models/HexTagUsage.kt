package pl.hexmind.mindshaper.database.models

import androidx.room.ColumnInfo

/**
 * A hex tag paired with how many thoughts carry it - the row shape of the Metadata tag list.
 */
data class HexTagUsage(
    val name: String,

    @ColumnInfo(name = "usage_count")
    val usageCount: Int
)
