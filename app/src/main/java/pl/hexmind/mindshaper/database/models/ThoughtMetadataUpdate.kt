package pl.hexmind.mindshaper.database.models

import androidx.room.ColumnInfo

/**
 * For Room Partial Update mechanism
 * It allows to update thought metadata separatelly
 */
data class ThoughtMetadataUpdate(

    @ColumnInfo(name = "id")
    val id: Int,

    @ColumnInfo(name = "domain_id")
    val domainId: Int?,

    @ColumnInfo(name = "thread")
    val thread: String?,

    @ColumnInfo(name = "soul_mate")
    val soulMate: String?,

    @ColumnInfo(name = "project")
    val project: String?,

    @ColumnInfo(name = "value")
    val value: Int
)