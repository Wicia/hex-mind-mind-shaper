package pl.hexmind.mindshaper.database.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Links a thought with its hex tags. Many tags per thought, one tag reused by many thoughts.
 */
@Entity(
    tableName = "THOUGHT_HEX_TAGS",
    primaryKeys = ["thought_id", "hex_tag_id"],
    foreignKeys = [
        ForeignKey(
            entity = ThoughtEntity::class,
            parentColumns = ["id"],
            childColumns = ["thought_id"],
            onDelete = ForeignKey.CASCADE // Deleting a thought drops its links, never the tags themselves
        ),
        ForeignKey(
            entity = HexTagEntity::class,
            parentColumns = ["id"],
            childColumns = ["hex_tag_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["thought_id"]),
        Index(value = ["hex_tag_id"])
    ]
)
data class ThoughtHexTagEntity(

    @ColumnInfo(name = "thought_id")
    val thoughtId: Int,

    @ColumnInfo(name = "hex_tag_id")
    val hexTagId: Int
)
