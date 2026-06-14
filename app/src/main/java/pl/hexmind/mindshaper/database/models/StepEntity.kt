package pl.hexmind.mindshaper.database.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "GOAL_STEPS",
    foreignKeys = [
        ForeignKey(
            entity = GoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goal_id"],
            onDelete = ForeignKey.CASCADE   // delete steps when goal is deleted
        ),
        ForeignKey(
            entity = ThoughtEntity::class,
            parentColumns = ["id"],
            childColumns = ["thought_id"],
            onDelete = ForeignKey.SET_NULL  // unlink (not delete) step when its thought is deleted
        )
    ],
    indices = [
        Index(value = ["goal_id"]),
        Index(value = ["thought_id"])
    ]
)
data class StepEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "goal_id")
    val goalId: Int,

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "position")
    val position: Int = 0,                         // user-defined order

    @ColumnInfo(name = "current_repetitions")
    val currentRepetitions: Int = 0,               // how many times the step has been done so far

    @ColumnInfo(name = "max_repetitions")
    val maxRepetitions: Int = 1,                   // target count; 1 = single checkbox

    @ColumnInfo(name = "thought_id")
    val thoughtId: Int? = null,                    // linked thought (1:1, optional)

    @ColumnInfo(name = "reminder_time")
    val reminderTime: String? = null,             // "HH:mm"; null = no reminder

    @ColumnInfo(name = "reminder_days")
    val reminderDays: String? = null              // CSV of weekday numbers 1..7 (Mon..Sun); null = no reminder
)
