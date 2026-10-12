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
    private const val DASH = " - "

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
            val dashIndex = line.indexOf(DASH)
            if (dashIndex < 0) return@mapNotNull null

            val front = line.substring(0, dashIndex).trim()
            val back = line.substring(dashIndex + DASH.length).trim()
            if (front.isEmpty() || back.isEmpty()) null else FlashcardDTO(front = front, back = back)
        }
        return Result(flashcards, skippedLines = lines.size - flashcards.size)
    }
}
