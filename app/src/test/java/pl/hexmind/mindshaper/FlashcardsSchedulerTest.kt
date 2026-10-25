package pl.hexmind.mindshaper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.hexmind.mindshaper.services.FlashcardsScheduler
import pl.hexmind.mindshaper.services.dto.FlashcardDTO
import pl.hexmind.mindshaper.services.dto.FlashcardRating
import pl.hexmind.mindshaper.services.dto.FlashcardStatus
import java.time.LocalDate

class FlashcardsSchedulerTest {

    private val today = LocalDate.of(2026, 10, 3)

    private fun card(status: FlashcardStatus = FlashcardStatus.NEW, level: Int = 0, id: Int = 1) =
        FlashcardDTO(id = id, front = "Q$id", back = "A$id", status = status, level = level)

    private fun rate(flashcard: FlashcardDTO, rating: FlashcardRating) =
        FlashcardsScheduler.rate(flashcard, rating, today)

    // ========== NEW ==========

    @Test
    fun `nowa - kazda ocena daje poziom 1 na jutro`() {
        FlashcardRating.entries.forEach { rating ->
            val rated = rate(card(), rating)
            assertEquals(FlashcardStatus.ACTIVE, rated.status)
            assertEquals(1, rated.level)
            assertEquals(today.plusDays(1), rated.dueOn)
            assertEquals(today, rated.introducedOn)
        }
    }

    // ========== PLANNED REVIEW ==========

    @Test
    fun `mam to - podnosi poziom i liczy termin od dzisiaj`() {
        val rated = rate(card(FlashcardStatus.ACTIVE, level = 2), FlashcardRating.GOOD)
        assertEquals(3, rated.level)
        assertEquals(today.plusDays(7), rated.dueOn)
    }

    @Test
    fun `cos swita - zostawia poziom, termin wg interwalu poziomu`() {
        val rated = rate(card(FlashcardStatus.ACTIVE, level = 3), FlashcardRating.OK)
        assertEquals(FlashcardStatus.ACTIVE, rated.status)
        assertEquals(3, rated.level)
        assertEquals(today.plusDays(7), rated.dueOn)
    }

    @Test
    fun `pustka - obniza poziom, termin zawsze jutro`() {
        val rated = rate(card(FlashcardStatus.ACTIVE, level = 4), FlashcardRating.BAD)
        assertEquals(FlashcardStatus.ACTIVE, rated.status)
        assertEquals(3, rated.level)
        assertEquals(today.plusDays(1), rated.dueOn)
    }

    @Test
    fun `pustka - nie ponizej poziomu 1`() {
        val rated = rate(card(FlashcardStatus.ACTIVE, level = 1), FlashcardRating.BAD)
        assertEquals(1, rated.level)
        assertEquals(today.plusDays(1), rated.dueOn)
    }

    @Test
    fun `mam to na poziomie 5 = utrwalona`() {
        val rated = rate(card(FlashcardStatus.ACTIVE, level = 5), FlashcardRating.GOOD)
        assertEquals(FlashcardStatus.MASTERED, rated.status)
        assertNull(rated.dueOn)
        assertFalse(FlashcardsScheduler.isRepetitionDue(rated, today.plusDays(365)))
    }

    @Test
    fun `spoznienie nie obniza poziomu`() {
        val overdue = card(FlashcardStatus.ACTIVE, level = 3).copy(dueOn = today.minusDays(10))
        assertTrue(FlashcardsScheduler.isRepetitionDue(overdue, today))
        assertEquals(4, rate(overdue, FlashcardRating.GOOD).level)
    }

    @Test
    fun `same mam to - utrwalenie w 55 dniu`() {
        var flashcard = card()
        var day = today
        val reviewDays = mutableListOf<Long>()
        while (flashcard.status != FlashcardStatus.MASTERED) {
            reviewDays += day.toEpochDay() - today.toEpochDay()
            flashcard = FlashcardsScheduler.rate(flashcard, FlashcardRating.GOOD, day)
            flashcard.dueOn?.let { dueOn -> day = dueOn }
        }
        assertEquals(listOf(0L, 1L, 4L, 11L, 25L, 55L), reviewDays)
    }
}
