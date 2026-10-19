package pl.hexmind.mindshaper.activities.flashcards

import pl.hexmind.mindshaper.services.dto.FlashcardDTO

/**
 * Text -> new flashcards, by the separator the user picked.
 * Blank lines never count - neither as a flashcard nor as skipped.
 */
object FlashcardsImport {

    enum class Separator {
        NEW_LINE, // Front line, back line below it, next front...
        DASH      // One line = "front - back"
    }

    /** @param skippedLines lines that did not make a flashcard (no pair / no dash / an empty side) */
    data class Result(
        val flashcards: List<FlashcardDTO>,
        val skippedLines: Int
    )

    // Dash with spaces - words like "biało-czerwony" stay whole
    // ! Editors swap "-" for typographic dashes (en dash, em dash, minus) and spaces for non-breaking ones -
    // a file from Word / Notes did not split at all
    private val DASH = Regex("[\\s\\u00A0][-\\u2010\\u2011\\u2012\\u2013\\u2014\\u2015\\u2212][\\s\\u00A0]")

    fun parse(text: String, separator: Separator): Result {
        val lines = text.lines().map { line -> line.trim() }.filter { line -> line.isNotEmpty() }
        return when (separator) {
            Separator.NEW_LINE -> parseByNewLine(lines)
            Separator.DASH     -> parseByDash(lines)
        }
    }

    private fun parseByNewLine(lines: List<String>): Result {
        val pairs = lines.chunked(2)
        val flashcards = pairs
            .filter { pair -> pair.size == 2 }
            .map { pair -> FlashcardDTO(front = pair[0], back = pair[1]) }
        // Odd count - the last line has no back
        return Result(flashcards, skippedLines = lines.size % 2)
    }

    private fun parseByDash(lines: List<String>): Result {
        val flashcards = lines.mapNotNull { line ->
            val dash = DASH.find(line) ?: return@mapNotNull null

            val front = line.substring(0, dash.range.first).trim()
            val back = line.substring(dash.range.last + 1).trim()
            if (front.isEmpty() || back.isEmpty()) null else FlashcardDTO(front = front, back = back)
        }
        return Result(flashcards, skippedLines = lines.size - flashcards.size)
    }
}
