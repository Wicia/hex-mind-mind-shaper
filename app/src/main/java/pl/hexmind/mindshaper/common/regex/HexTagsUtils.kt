package pl.hexmind.mindshaper.common.regex

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

class HexTagsUtils {

    companion object {
        fun parseInput(input: String?): HexTags {
            val input = input.orEmpty()

            // Find tag positions
            val projectIndex = input.indexOf("#")
            val soulMateIndex = input.indexOf("@")

            // List of tags with positions (sorted)
            data class Tag(val type: String, val index: Int)
            val tags = mutableListOf<Tag>()
            if (projectIndex != -1) tags.add(Tag("project", projectIndex))
            if (soulMateIndex != -1) tags.add(Tag("soulMate", soulMateIndex))
            tags.sortBy { it.index }

            // Thread - text before first tag (null if empty)
            val thread = if (tags.isNotEmpty()) {
                val text = input.substring(0, tags.first().index).trim()
                text.ifEmpty { null }
            } else {
                val text = input.trim()
                text.ifEmpty { null }
            }

            // Project - text after # (until next tag or end)
            val project = if (projectIndex != -1) {
                val start = projectIndex + 1
                val nextTagIndex = tags.firstOrNull { it.index > projectIndex }?.index ?: input.length
                input.substring(start, nextTagIndex).trim().ifEmpty { null }
            } else null

            // SoulMate - text after @ (until next tag or end)
            val soulMate = if (soulMateIndex != -1) {
                val start = soulMateIndex + 1
                val nextTagIndex = tags.firstOrNull { it.index > soulMateIndex }?.index ?: input.length
                input.substring(start, nextTagIndex).trim().ifEmpty { null }
            } else null

            return HexTags(thread = thread, soulMate = soulMate, project = project)
        }
    }
}

@Parcelize
data class HexTags (
    val thread : String? = null,
    val soulMate: String? = null,
    val project: String? = null
) : Parcelable {
    fun areCriteriaEmpty() : Boolean{
        return thread.isNullOrBlank() && soulMate.isNullOrBlank() && project.isNullOrBlank()
    }
}