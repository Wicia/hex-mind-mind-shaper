package pl.hexmind.fastnote.data.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

import java.util.Date

// #TODO when project is getting more & more complex -> improve code by adding Thought DTO + mapper
@Entity(
    tableName = "THOUGHTS",
    foreignKeys = [
        ForeignKey(
            entity = ContextEntity::class,
            parentColumns = ["id"],
            childColumns = ["context_id"],
            onDelete = ForeignKey.SET_NULL // ! When linked Context entity is deleted = don't delete this entity
        )
    ]
)
data class ThoughtEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "context_id")
    val contextId: Int?, // Foreign key to ContextEntity

    @ColumnInfo(name = "essence")
    val essence: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Date = Date(),

    @ColumnInfo(name = "priority")
    val priority: Int = 3
)