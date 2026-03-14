package pl.hexmind.mindshaper.database.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "GOAL_GUIDELINES",
    foreignKeys = [
        ForeignKey(
            entity = GoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goal_id"],
            onDelete = ForeignKey.CASCADE   // delete guidelines when goal is deleted
        )
    ],
    indices = [Index(value = ["goal_id"])]
)
data class GuidelineEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "goal_id")
    val goalId: Int,

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "is_done")
    val isDone: Boolean = false,

    @ColumnInfo(name = "position")
    val position: Int = 0                      // user-defined order via drag & drop
)
