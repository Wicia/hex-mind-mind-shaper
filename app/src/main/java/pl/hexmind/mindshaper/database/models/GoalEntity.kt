package pl.hexmind.mindshaper.database.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "GOALS")
data class GoalEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "priority")
    val priority: Int = 3,                     // 1..3, 1 = highest

    @ColumnInfo(name = "last_modified_at")
    val lastModifiedAt: Long = System.currentTimeMillis()
)
