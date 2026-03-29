package pl.hexmind.mindshaper.database.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Learning path (named sequence of steps)
 */
@Entity(tableName = "PATHS")
data class PathEntity(

    @PrimaryKey
    @ColumnInfo(name = "path_key")
    val pathKey: String,

    @ColumnInfo(name = "category")
    val category: String,

    // UNSELECTED → STARTED → COMPLETED (see companion constants)
    @ColumnInfo(name = "status")
    val status: String = STATUS_UNSELECTED,

    // Index of the step currently shown to the user (0-based)
    @ColumnInfo(name = "current_step_index")
    val currentStepIndex: Int = 0,

    // Epoch day when this path was drawn (picked); null = never drawn (picked) / returned to pool
    @ColumnInfo(name = "last_drawn_date")
    val lastDrawnDate: Long? = null // TODO: replace "draw" with "pick"

) {
    companion object {
        const val STATUS_UNSELECTED = "UNSELECTED"
        const val STATUS_STARTED    = "STARTED"
        const val STATUS_COMPLETED  = "COMPLETED"
    }
}