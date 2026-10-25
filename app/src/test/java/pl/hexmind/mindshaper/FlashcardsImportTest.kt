package pl.hexmind.mindshaper

import org.junit.Assert.assertEquals
import org.junit.Test
import pl.hexmind.mindshaper.activities.flashcards.FlashcardsImport
import pl.hexmind.mindshaper.activities.flashcards.FlashcardsImport.Separator

class FlashcardsImportTest {

    private fun pairsOf(text: String) =
        FlashcardsImport.parse(text, Separator.DASH).flashcards.map { flashcard -> flashcard.front to flashcard.back }

    @Test
    fun `myslnik - zwykly dywiz ze spacjami`() {
        assertEquals(listOf("kot" to "cat"), pairsOf("kot - cat"))
    }

    @Test
    fun `myslnik - polpauza, pauza i minus tez dziela`() {
        assertEquals(
            listOf("kot" to "cat", "pies" to "dog", "dom" to "house"),
            pairsOf("kot – cat\npies — dog\ndom − house")
        )
    }

    @Test
    fun `myslnik - twarde spacje wokol kreski`() {
        assertEquals(listOf("kot" to "cat"), pairsOf("kot — cat"))
    }

    @Test
    fun `myslnik - slowo z lacznikiem zostaje cale`() {
        assertEquals(listOf("biało-czerwony" to "white and red"), pairsOf("biało-czerwony - white and red"))
    }

    @Test
    fun `myslnik - liczy sie pierwsza kreska, reszta w rewersie`() {
        assertEquals(listOf("Od kiedy?" to "How long — exactly?"), pairsOf("Od kiedy? — How long — exactly?"))
    }

    @Test
    fun `myslnik - linia bez kreski ze spacjami pominieta`() {
        val result = FlashcardsImport.parse("kot-cat\npies - dog", Separator.DASH)
        assertEquals(1, result.flashcards.size)
        assertEquals(1, result.skippedLines)
    }

    @Test
    fun `enter - para po parze, nieparzysta ostatnia pominieta`() {
        val result = FlashcardsImport.parse("kot\ncat\n\npies\ndog\nsam", Separator.NEW_LINE)
        assertEquals(listOf("kot" to "cat", "pies" to "dog"), result.flashcards.map { it.front to it.back })
        assertEquals(1, result.skippedLines)
    }
}
