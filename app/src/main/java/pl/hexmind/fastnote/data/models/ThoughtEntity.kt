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
            entity = AreaEntity::class,
            parentColumns = ["id"],
            childColumns = ["area_id"],
            onDelete = ForeignKey.SET_NULL // ! When child entity is deleted = don't delete this (parent) entity
        )
    ],
    indices = [Index(value = ["area_id"])]
)
data class ThoughtEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "area_id")
    val areaId: Int?,

    @ColumnInfo(name = "thread")
    val thread: String? = null,

    @ColumnInfo(name = "essence")
    val essence: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Date = Date(),

    @ColumnInfo(name = "priority")
    val priority: Int = 3
)
{
    val areaIdentifier: AreaIdentifier?
        get() = areaId?.let { AreaIdentifier.fromIntOrDefault(it) }
}