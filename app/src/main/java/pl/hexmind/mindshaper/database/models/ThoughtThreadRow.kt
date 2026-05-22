package pl.hexmind.mindshaper.database.models

import androidx.room.ColumnInfo

/**
 * Lightweight row for fetching thought titles only
 */
data class ThoughtThreadRow(
    val id: Int,
    @ColumnInfo(name = "thread")
    val thread: String?
)