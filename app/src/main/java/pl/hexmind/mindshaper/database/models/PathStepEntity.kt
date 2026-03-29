package pl.hexmind.mindshaper.database.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Single step within a learning path.
 */
@Entity(
    tableName = "PATH_STEPS",
    foreignKeys = [
        ForeignKey(
            entity = PathEntity::class,
            parentColumns = ["path_key"],
            childColumns = ["path_key"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["path_key"])]
)
data class PathStepEntity(

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int = 0,

    // FK
    @ColumnInfo(name = "path_key")
    val pathKey: String,

    // 0-based position within the path
    @ColumnInfo(name = "position")
    val position: Int,

    @ColumnInfo(name = "content")
    val content: String
)