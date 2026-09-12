package pl.hexmind.mindshaper.services

import pl.hexmind.mindshaper.common.regex.HexTagNormalizer
import java.util.Locale
import pl.hexmind.mindshaper.database.models.HexTagType
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

        return hexTagDAO.findSimilarTags(tagType.name, HexTagNormalizer.normalize(typedName))
            .map { hexTag -> hexTag.displayName }
            .filter { displayName -> displayName != typedName }
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
    }
}
