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

    @ColumnInfo(name = "importance")
    val importance: Int = 1,                   // 1..3, 3 = high importance

    @ColumnInfo(name = "last_modified_at")
    val lastModifiedAt: Long = System.currentTimeMillis()
)
