package pl.hexmind.mindshaper.services

import pl.hexmind.mindshaper.common.regex.HexTagNormalizer
import java.util.Locale
import pl.hexmind.mindshaper.database.models.HexTagType
import pl.hexmind.mindshaper.database.models.HexTagUsage
import pl.hexmind.mindshaper.database.repositories.HexTagDAO
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HexTagsService @Inject constructor(
    private val hexTagDAO: HexTagDAO
) {

    /**
     * Chips offered under a tag field.
     *
     * Up to two characters the user has nothing to search by yet, so the list is a shortlist of what
     * they use anyway; from the third character it narrows to what actually matches.
     */
    suspend fun getSuggestions(tagType: HexTagType, typedText: String): List<String> {
        // ! Only the tag being typed counts. A field may already hold finished tags, and matching the
        // whole text would look for one tag literally named "ja kam" and find nothing.
        val trimmedText = lastTag(typedText)

        return if (trimmedText.length >= FRAGMENT_MIN_CHARS) {
            hexTagDAO.findTagNamesContaining(
                type               = tagType.name,
                normalizedFragment = HexTagNormalizer.normalize(trimmedText),
                limit              = SUGGESTIONS_LIMIT
            )
        }
        else {
            defaultSuggestions(tagType)
        }
    }

    /**
     * Tags spelled differently but meaning the same - the basis of the "similar tag exists" prompt.
     *
     * ! Case is NOT a difference: every tag is stored lower case, so "Ja" simply IS "ja" and must
     * never be offered as a variant. Only diacritics make a genuinely different word.
     */
    suspend fun findSimilarTagNames(tagType: HexTagType, typedText: String): List<String> {
        val typedName = typedText.trim().lowercase(Locale.ROOT)
        if (typedName.isEmpty()) return emptyList()

        val similarTags = hexTagDAO.findSimilarTags(tagType.name, HexTagNormalizer.normalize(typedName))

        // ! Nothing to warn about when the tag already exists - the user is reusing it, not creating
        // a variant. Without this, editing a thought tagged "powieść" kept offering "powiesc".
        val tagAlreadyExists = similarTags.any { hexTag -> hexTag.displayName == typedName }
        if (tagAlreadyExists) return emptyList()

        return similarTags.map { hexTag -> hexTag.displayName }
    }

    /**
     * Tags of one type for the Metadata list: the self tag "ja" first (a core tag), then the rest
     * most-used to least-used. Count 0 is kept, so a never-linked tag still shows.
     */
    suspend fun getTagsWithUsage(tagType: HexTagType): List<HexTagUsage> {
        val tags = hexTagDAO.getTagsWithUsageByType(tagType.name)

        // "ja" is stored lower case, so an exact match is safe; pull it out and pin it so it is not
        // listed twice regardless of its own count
        val selfTag = tags.firstOrNull { hexTag -> hexTag.name == SELF_TAG_NAME }
        if (selfTag == null) {
            return tags
        }

        return listOf(selfTag) + tags.filter { hexTag -> hexTag.name != SELF_TAG_NAME }
    }

    /**
     * Renames a tag in the dictionary. A name already held by another tag of the same type is
     * rejected (combining the two is a separate merge feature), so the caller can warn instead of
     * hitting the unique (display_name, type) index.
     */
    suspend fun renameTag(tagType: HexTagType, currentName: String, newName: String): RenameOutcome {
        val displayName = newName.trim().lowercase(Locale.ROOT)
        if (displayName.isEmpty()) {
            return RenameOutcome.NOT_FOUND
        }

        val currentTag = hexTagDAO.findTag(tagType.name, currentName)
            ?: return RenameOutcome.NOT_FOUND
        if (displayName == currentTag.displayName) {
            return RenameOutcome.UNCHANGED
        }

        val clashingTag = hexTagDAO.findTag(tagType.name, displayName)
        if (clashingTag != null) {
            return RenameOutcome.NAME_TAKEN
        }

        hexTagDAO.renameTag(currentTag.id!!, displayName, HexTagNormalizer.normalize(displayName))
        return RenameOutcome.RENAMED
    }

    /**
     * Removes a tag from the dictionary. Its links die with it through ON DELETE CASCADE, so the
     * thoughts stay untouched and simply no longer carry this tag.
     */
    suspend fun deleteTag(tagType: HexTagType, tagName: String) {
        val tag = hexTagDAO.findTag(tagType.name, tagName) ?: return
        hexTagDAO.deleteTag(tag.id!!)
    }

    // Recent first so the shortlist reacts to what the user is doing now, then filled with favourites
    private suspend fun defaultSuggestions(tagType: HexTagType): List<String> {
        val recent   = hexTagDAO.getRecentTagNames(tagType.name, RECENT_COUNT)
        val mostUsed = hexTagDAO.getMostUsedTagNames(tagType.name, SUGGESTIONS_LIMIT)

        return (recent + mostUsed)
            .distinct()
            .take(SUGGESTIONS_LIMIT)
    }

    private fun lastTag(typedText: String): String {
        if (typedText.isEmpty()) return ""

        // A trailing separator means the previous tag is done and a new, empty one has begun
        val lastCharacter = typedText.last()
        if (lastCharacter.isWhitespace() || lastCharacter == ',') return ""

        return typedText.split(TAG_SEPARATOR_PATTERN).last()
    }

    private companion object {
        val TAG_SEPARATOR_PATTERN = Regex("[,\\s]+")

        // Two most recent plus three most used - a starting point, to be revisited after testing
        const val RECENT_COUNT = 2
        const val SUGGESTIONS_LIMIT = 5

        const val FRAGMENT_MIN_CHARS = 3

        // The self / core person tag, pinned to the front of the Metadata list
        const val SELF_TAG_NAME = "ja"
    }
}

enum class RenameOutcome {
    RENAMED,
    NAME_TAKEN,
    UNCHANGED,
    NOT_FOUND
}
