package pl.hexmind.mindshaper.database.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Index
import java.time.Instant

import java.util.Date

@Entity(
    tableName = "THOUGHTS",
    foreignKeys = [
        ForeignKey(
            entity = DomainEntity::class,
            parentColumns = ["id"],
            childColumns = ["domain_id"],
            onDelete = ForeignKey.SET_NULL // ! When child entity is deleted = don't delete this (parent) entity
        )
    ],
    indices = [Index(value = ["domain_id"])]
)
data class ThoughtEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,

    @ColumnInfo(name = "domain_id")
    val domainId: Int?,

    @ColumnInfo(name = "thread")
    val thread: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant = Instant.now(),

    @ColumnInfo(name = "rich_text")
    val richText: String? = null,

    @ColumnInfo(name = "soul_mate")
    val soulMate: String? = null,

    @ColumnInfo(name = "project")
    val project: String? = null
)