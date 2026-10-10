package pl.hexmind.mindshaper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.hexmind.mindshaper.services.FlashcardsReview
import pl.hexmind.mindshaper.services.FlashcardsScheduler
import pl.hexmind.mindshaper.services.dto.FlashcardDTO
import pl.hexmind.mindshaper.services.dto.FlashcardRating
import pl.hexmind.mindshaper.services.dto.FlashcardSetDTO
import pl.hexmind.mindshaper.services.dto.FlashcardStatus
import java.time.LocalDate
import kotlin.random.Random

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

    // ========== QUEUE ==========

    private fun set(id: Int, vararg flashcards: FlashcardDTO) =
        FlashcardSetDTO(id = id, name = "Set $id", flashcards = flashcards.toList())

    @Test
    fun `kolejka - najnizsze poziomy, na koncu nowe`() {
        val sets = listOf(
            set(
                1,
                card(id = 1),
                card(FlashcardStatus.ACTIVE, level = 3, id = 2).copy(dueOn = today),
                card(FlashcardStatus.ACTIVE, level = 1, id = 3).copy(dueOn = today),
                card(FlashcardStatus.ACTIVE, level = 2, id = 5).copy(dueOn = today.plusDays(1))
            )
        )
        val plan = FlashcardsReview.plan(sets, FlashcardsReview.Params(10, 30), today)
        assertEquals(listOf(3, 2, 1), plan.queue.map { card -> card.flashcard.id })
        assertEquals(2, plan.repetitionsCount)
        assertEquals(1, plan.newCount)
    }

    @Test
    fun `kolejka - w obrebie poziomu losowo, poziomy zawsze rosnaco`() {
        val due = (1..20).map { id -> card(FlashcardStatus.ACTIVE, level = 1 + id % 3, id = id).copy(dueOn = today) }
        val sets = listOf(set(1, *due.toTypedArray()))
        val orders = (1..5).map { seed ->
            FlashcardsReview.plan(sets, FlashcardsReview.Params(10, 30), today, random = Random(seed))
                .queue.map { card -> card.flashcard }
        }
        orders.forEach { order -> assertEquals(order.map { it.level }.sorted(), order.map { it.level }) }
        assertTrue(orders.map { order -> order.map { it.id } }.distinct().size > 1)
    }

    @Test
    fun `kolejka - limit nowych dziennie liczy juz wprowadzone dzisiaj`() {
        val sets = listOf(
            set(
                1,
                card(FlashcardStatus.ACTIVE, level = 1, id = 1).copy(dueOn = today.plusDays(1), introducedOn = today),
                card(id = 2),
                card(id = 3),
                card(id = 4)
            )
        )
        val plan = FlashcardsReview.plan(sets, FlashcardsReview.Params(newPerDay = 3, backlogThreshold = 30), today)
        assertEquals(listOf(2, 3), plan.queue.map { card -> card.flashcard.id })
    }

    @Test
    fun `kolejka - zaleglosci ponad prog wstrzymuja nowe`() {
        val due = (1..6).map { id -> card(FlashcardStatus.ACTIVE, level = 1, id = id).copy(dueOn = today) }
        val sets = listOf(set(1, *(due + card(id = 99)).toTypedArray()))
        val plan = FlashcardsReview.plan(sets, FlashcardsReview.Params(newPerDay = 10, backlogThreshold = 5), today)
        assertEquals(0, plan.newCount)
        assertEquals(1, plan.newPausedBacklog)
    }
}
