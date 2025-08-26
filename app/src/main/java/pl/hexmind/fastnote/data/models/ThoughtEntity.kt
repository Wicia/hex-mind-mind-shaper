package pl.hexmind.fastnote.data.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Index

import java.util.Date

// #TODO when project is getting more & more complex -> improve code by adding Thought DTO + mapper
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
    val id: Int = 0,

    @ColumnInfo(name = "domain_id")
    val domainId: Int?,

    @ColumnInfo(name = "thread")
    val thread: String? = null,

    @ColumnInfo(name = "essence")
    val essence: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Date = Date(),

    @ColumnInfo(name = "priority")
    val priority: Int = 3
)