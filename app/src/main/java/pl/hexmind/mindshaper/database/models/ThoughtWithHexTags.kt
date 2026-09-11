package pl.hexmind.mindshaper.database.models

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

/**
 * A thought together with its hex tags, fetched in one go.
 *
 * Room resolves the junction itself, so a list of thoughts costs two queries instead of one per row.
 */
data class ThoughtWithHexTags(

    @Embedded
    val thought: ThoughtEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = ThoughtHexTagEntity::class,
            parentColumn = "thought_id",
            entityColumn = "hex_tag_id"
        )
    )
    val hexTags: List<HexTagEntity>
) {

    fun tagNamesOfType(type: HexTagType): List<String> =
        hexTags.filter { hexTag -> hexTag.type == type.name }
            .map { hexTag -> hexTag.displayName }
}
