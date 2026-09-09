package pl.hexmind.mindshaper.database.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Dictionary of hex tags. One row per distinct (display name, type) pair, reused by every thought
 * that carries it.
 *
 * Spelling variants stay separate rows on purpose - "sad" and "sąd" are different words.
 * The normalized form only powers suggestions and the "a similar tag exists" prompt.
 */
@Entity(
    tableName = "HEX_TAGS",
    // A name is unique only within its type - "cel" could be both a person and a project
    indices = [Index(value = ["display_name", "type"], unique = true)]
)
data class HexTagEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,

    @ColumnInfo(name = "display_name")
    val displayName: String,

    @ColumnInfo(name = "normalized_name")
    val normalizedName: String,

    @ColumnInfo(name = "type")
    val type: String
)
