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