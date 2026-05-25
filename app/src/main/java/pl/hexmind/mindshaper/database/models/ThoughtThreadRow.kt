package pl.hexmind.mindshaper.database.models

import androidx.room.ColumnInfo

/**
 * Lightweight row for fetching thought titles only
 */
data class ThoughtSubjectRow(
    val id: Int,
    @ColumnInfo(name = "subject")
    val subject: String?
)