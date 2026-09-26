package pl.hexmind.mindshaper.common.regex

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

class HexTagsUtils {

    companion object {
        fun parseInput(input: String?): HexTags {
            val text = input.orEmpty()

            // ! Every marker counts, not just the first one - with "@ja @michal" the second "@" has to
            // end the first tag, otherwise its marker stays glued to the name and becomes part of it
            val markers = text.mapIndexedNotNull { index, character ->
                if (character == PERSON_MARKER || character == PROJECT_MARKER) index to character else null
            }

            // Subject - whatever comes before the first marker
            val subjectText = if (markers.isEmpty()) text else text.substring(0, markers.first().first)
            val subject = subjectText.trim().ifEmpty { null }

            val people = mutableListOf<String>()
            val projects  = mutableListOf<String>()

            markers.forEachIndexed { position, (markerIndex, marker) ->
                val nextMarkerIndex = markers.getOrNull(position + 1)?.first ?: text.length
                val value = text.substring(markerIndex + 1, nextMarkerIndex).trim()
                if (value.isEmpty()) return@forEachIndexed

                if (marker == PERSON_MARKER) {
                    people += value
                }
                else {
                    projects += value
                }
            }

            return HexTags(
                subject  = subject,
                person = people.joinToString(" ").ifEmpty { null },
                project  = projects.joinToString(" ").ifEmpty { null }
            )
        }

        /**
         * Scans free-form text (e.g. a note's rich text) for @person and #project markers anywhere
         * inside it. Unlike [parseInput], a marker only grabs the single word right after it - the
         * surrounding prose is not a tag, so "spotkanie z @Michałem wczoraj" tags just "Michałem".
         */
        fun extractEmbeddedTags(text: String?): HexTags {
            val people = mutableListOf<String>()
            val projects = mutableListOf<String>()

            EMBEDDED_TAG_PATTERN.findAll(text.orEmpty()).forEach { match ->
                val (marker, value) = match.destructured
                if (marker[0] == PERSON_MARKER) people += value else projects += value
            }

            return HexTags(
                person  = people.joinToString(" ").ifEmpty { null },
                project = projects.joinToString(" ").ifEmpty { null }
            )
        }

        /**
         * Unions two space-separated tag strings, dropping case-insensitive duplicates while keeping
         * the first-seen spelling - so a tag already in the hex tags field isn't added a second time
         * just because it also appears inline in the note text.
         */
        fun mergeTagNames(existing: String?, additional: String?): String? {
            val merged = LinkedHashSet<String>()

            fun addAll(value: String?) {
                value?.split(TAG_SEPARATOR_PATTERN)
                    ?.map { name -> name.trim() }
                    ?.filter { name -> name.isNotEmpty() }
                    ?.forEach { name ->
                        if (merged.none { it.equals(name, ignoreCase = true) }) merged += name
                    }
            }

            addAll(existing)
            addAll(additional)

            return merged.joinToString(" ").ifEmpty { null }
        }

        // Group 1 = marker, group 2 = tag name - also used by HexTextView to render inline tags
        // ! Marker has to start a word (text start or whitespace before it) - "cos@poczta.pl" is not a tag
        val EMBEDDED_TAG_PATTERN = Regex("(?<!\\S)([@#])([\\p{L}\\p{N}_-]+)")
        private val TAG_SEPARATOR_PATTERN = Regex("[,\\s]+")

        private const val PERSON_MARKER = '@'
        private const val PROJECT_MARKER = '#'
    }
}

@Parcelize
data class HexTags (
    val subject : String? = null,
    val person: String? = null,
    val project: String? = null
) : Parcelable {
    fun areCriteriaEmpty() : Boolean{
        return subject.isNullOrBlank() && person.isNullOrBlank() && project.isNullOrBlank()
    }
}