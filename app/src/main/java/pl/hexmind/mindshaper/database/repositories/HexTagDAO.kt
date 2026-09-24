package pl.hexmind.mindshaper.database.repositories

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import pl.hexmind.mindshaper.common.regex.HexTagNormalizer
import pl.hexmind.mindshaper.database.models.HexTagEntity
import pl.hexmind.mindshaper.database.models.HexTagUsage
import pl.hexmind.mindshaper.database.models.ThoughtHexTagEntity

@Dao
interface HexTagDAO {

    // ===========================================
    //      Tag dictionary
    // ===========================================

    @Query("SELECT * FROM HEX_TAGS WHERE type = :type ORDER BY display_name ASC")
    suspend fun getTagsByType(type: String): List<HexTagEntity>

    /**
     * Every tag of a type with its usage count.
     * LEFT JOIN so a tag linked to nothing still returns (count 0) - a never-used tag must stay listable.
     */
    @Query("""
        SELECT t.display_name AS name, COUNT(link.thought_id) AS usage_count
        FROM HEX_TAGS t
        LEFT JOIN THOUGHT_HEX_TAGS link ON link.hex_tag_id = t.id
        WHERE t.type = :type
        GROUP BY t.id
        ORDER BY usage_count DESC, t.display_name ASC
    """)
    suspend fun getTagsWithUsageByType(type: String): List<HexTagUsage>

    @Query("UPDATE HEX_TAGS SET display_name = :displayName, normalized_name = :normalizedName WHERE id = :tagId")
    suspend fun renameTag(tagId: Int, displayName: String, normalizedName: String)

    @Query("SELECT * FROM HEX_TAGS WHERE type = :type AND display_name = :displayName LIMIT 1")
    suspend fun findTag(type: String, displayName: String): HexTagEntity?

    /** Tags that only differ in case or diacritics - "zolw" finds "żółw". */
    @Query("SELECT * FROM HEX_TAGS WHERE type = :type AND normalized_name = :normalizedName")
    suspend fun findSimilarTags(type: String, normalizedName: String): List<HexTagEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: HexTagEntity): Long

    /**
     * Returns the id of an existing tag or creates it.
     *
     * Matching is exact on the display name - a variant such as "sad" next to "sąd" becomes its own
     * tag. Steering the user towards an existing variant is the UI's job, not the database's.
     */
    @Transaction
    suspend fun getOrCreateTagId(type: String, displayName: String): Int {
        val trimmedName = displayName.trim()

        val insertedId = insertTag(
            HexTagEntity(
                displayName    = trimmedName,
                normalizedName = HexTagNormalizer.normalize(trimmedName),
                type           = type
            )
        )
        if (insertedId != -1L) return insertedId.toInt()

        // IGNORE returned -1, so the tag is already there
        return findTag(type, trimmedName)?.id
            ?: error("Hex tag '$trimmedName' ($type) neither inserted nor found")
    }

    // Tags outlive the thoughts that used them - call this only if the dictionary needs pruning
    @Query("DELETE FROM HEX_TAGS WHERE id NOT IN (SELECT hex_tag_id FROM THOUGHT_HEX_TAGS)")
    suspend fun deleteOrphanedTags()


    // ===========================================
    //      Thought links
    // ===========================================

    @Query("""
        SELECT t.* FROM HEX_TAGS t
        INNER JOIN THOUGHT_HEX_TAGS link ON link.hex_tag_id = t.id
        WHERE link.thought_id = :thoughtId
        ORDER BY t.type ASC, t.display_name ASC
    """)
    suspend fun getTagsForThought(thoughtId: Int): List<HexTagEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun linkTag(link: ThoughtHexTagEntity)

    @Query("DELETE FROM THOUGHT_HEX_TAGS WHERE thought_id = :thoughtId")
    suspend fun unlinkAllTags(thoughtId: Int)

    @Query("""
        DELETE FROM THOUGHT_HEX_TAGS
        WHERE thought_id = :thoughtId
          AND hex_tag_id IN (SELECT id FROM HEX_TAGS WHERE type = :type)
    """)
    suspend fun unlinkTagsOfType(thoughtId: Int, type: String)

    // ===========================================
    //      Suggestions
    // ===========================================

    @Query("""
        SELECT t.display_name FROM HEX_TAGS t
        INNER JOIN THOUGHT_HEX_TAGS link ON link.hex_tag_id = t.id
        WHERE t.type = :type
        GROUP BY t.id
        ORDER BY COUNT(link.thought_id) DESC, t.display_name ASC
        LIMIT :limit
    """)
    suspend fun getMostUsedTagNames(type: String, limit: Int): List<String>

    @Query("""
        SELECT t.display_name FROM HEX_TAGS t
        INNER JOIN THOUGHT_HEX_TAGS link ON link.hex_tag_id = t.id
        INNER JOIN THOUGHTS th ON th.id = link.thought_id
        WHERE t.type = :type
        GROUP BY t.id
        ORDER BY MAX(th.updated_at) DESC
        LIMIT :limit
    """)
    suspend fun getRecentTagNames(type: String, limit: Int): List<String>

    /** Tags whose name contains the typed fragment - powers the list from the third character on. */
    @Query("""
        SELECT display_name FROM HEX_TAGS
        WHERE type = :type AND normalized_name LIKE '%' || :normalizedFragment || '%'
        ORDER BY LENGTH(display_name) ASC, display_name ASC
        LIMIT :limit
    """)
    suspend fun findTagNamesContaining(type: String, normalizedFragment: String, limit: Int): List<String>


    /** Replaces every tag of one type on a thought - the shape most edits take. */
    @Transaction
    suspend fun replaceTagsOfType(thoughtId: Int, type: String, displayNames: List<String>) {
        unlinkTagsOfType(thoughtId, type)

        displayNames.map { name -> name.trim() }
            .filter { name -> name.isNotEmpty() }
            .distinct()
            .forEach { name ->
                val tagId = getOrCreateTagId(type, name)
                linkTag(ThoughtHexTagEntity(thoughtId = thoughtId, hexTagId = tagId))
            }
    }


    // ===========================================
    //      Snapshot (backup / restore)
    // ===========================================

    @Query("SELECT * FROM HEX_TAGS")
    suspend fun getAllTags(): List<HexTagEntity>

    @Query("SELECT * FROM THOUGHT_HEX_TAGS")
    suspend fun getAllThoughtHexTagLinks(): List<ThoughtHexTagEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceTags(tags: List<HexTagEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceLinks(links: List<ThoughtHexTagEntity>)

    // Split per table: one DAO owns two tables, so a single clearAll()/insertOrReplace() would clash
    @Query("DELETE FROM HEX_TAGS")
    suspend fun clearAllTags()

    @Query("DELETE FROM THOUGHT_HEX_TAGS")
    suspend fun clearAllLinks()
}
