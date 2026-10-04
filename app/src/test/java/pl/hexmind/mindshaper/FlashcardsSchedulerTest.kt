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
import java.time.Instant
import java.time.LocalDate

class FlashcardsSchedulerTest {

    private val today = LocalDate.of(2026, 10, 3)
    private val now = Instant.parse("2026-10-03T12:00:00Z")

    private fun card(status: FlashcardStatus = FlashcardStatus.NEW, level: Int = 0, id: Int = 1) =
        FlashcardDTO(id = id, front = "Q$id", back = "A$id", status = status, level = level)

    private fun rate(flashcard: FlashcardDTO, rating: FlashcardRating) =
        FlashcardsScheduler.rate(flashcard, rating, today, now)

    // ========== NEW ==========

    @Test
    fun `nowa - dobrze i srednio daja poziom 1 na jutro`() {
        listOf(FlashcardRating.GOOD, FlashcardRating.OK).forEach { rating ->
            val rated = rate(card(), rating)
            assertEquals(FlashcardStatus.ACTIVE, rated.status)
            assertEquals(1, rated.level)
            assertEquals(today.plusDays(1), rated.dueOn)
            assertEquals(today, rated.introducedOn)
        }
    }

    @Test
    fun `nowa - kiepsko zamraza na poziomie 1`() {
        val rated = rate(card(), FlashcardRating.BAD)
        assertEquals(FlashcardStatus.FROZEN, rated.status)
        assertEquals(1, rated.level)
        assertEquals(now.plus(FlashcardsScheduler.SECOND_CHANCE_DELAY), rated.frozenUntil)
        assertEquals(today, rated.introducedOn)
    }

    // ========== PLANNED REVIEW ==========

    @Test
    fun `planowa - dobrze podnosi poziom i liczy termin od dzisiaj`() {
        val rated = rate(card(FlashcardStatus.ACTIVE, level = 2), FlashcardRating.GOOD)
        assertEquals(3, rated.level)
        assertEquals(today.plusDays(7), rated.dueOn)
    }

    @Test
    fun `planowa - srednio zostawia poziom`() {
        val rated = rate(card(FlashcardStatus.ACTIVE, level = 3), FlashcardRating.OK)
        assertEquals(FlashcardStatus.ACTIVE, rated.status)
        assertEquals(3, rated.level)
        assertEquals(today.plusDays(7), rated.dueOn)
    }

    @Test
    fun `planowa - kiepsko zamraza bez zmiany poziomu`() {
        val rated = rate(card(FlashcardStatus.ACTIVE, level = 4), FlashcardRating.BAD)
        assertEquals(FlashcardStatus.FROZEN, rated.status)
        assertEquals(4, rated.level)
        assertNull(rated.dueOn)
    }

    @Test
    fun `planowa - dobrze na poziomie 5 = opanowana`() {
        val rated = rate(card(FlashcardStatus.ACTIVE, level = 5), FlashcardRating.GOOD)
        assertEquals(FlashcardStatus.MASTERED, rated.status)
        assertNull(rated.dueOn)
    }

    // ========== SECOND CHANCE ==========

    @Test
    fun `druga szansa - dobrze ratuje poziom, ale go nie podnosi`() {
        val rated = rate(card(FlashcardStatus.FROZEN, level = 3), FlashcardRating.GOOD)
        assertEquals(FlashcardStatus.ACTIVE, rated.status)
        assertEquals(3, rated.level)
        assertEquals(today.plusDays(7), rated.dueOn)
        assertNull(rated.frozenUntil)
    }

    @Test
    fun `druga szansa - srednio obniza poziom`() {
        val rated = rate(card(FlashcardStatus.FROZEN, level = 3), FlashcardRating.OK)
        assertEquals(FlashcardStatus.ACTIVE, rated.status)
        assertEquals(2, rated.level)
        assertEquals(today.plusDays(3), rated.dueOn)
    }

    @Test
    fun `druga szansa - kiepsko obniza poziom i zamraza ponownie, nie ponizej 1`() {
        val rated = rate(card(FlashcardStatus.FROZEN, level = 1), FlashcardRating.BAD)
        assertEquals(FlashcardStatus.FROZEN, rated.status)
        assertEquals(1, rated.level)
    }

    @Test
    fun `zamrozona dostepna dopiero po godzinie, niewykorzystana czeka dalej`() {
        val frozen = rate(card(FlashcardStatus.ACTIVE, level = 2), FlashcardRating.BAD)
        assertFalse(FlashcardsScheduler.isRepetitionDue(frozen, today, now))
        assertTrue(FlashcardsScheduler.isRepetitionDue(frozen, today, now.plusSeconds(3600)))
        assertTrue(FlashcardsScheduler.isRepetitionDue(frozen, today.plusDays(1), now.plusSeconds(86_400)))
    }

    @Test
    fun `spoznienie nie obniza poziomu`() {
        val overdue = card(FlashcardStatus.ACTIVE, level = 3).copy(dueOn = today.minusDays(10))
        assertTrue(FlashcardsScheduler.isRepetitionDue(overdue, today, now))
        assertEquals(4, rate(overdue, FlashcardRating.GOOD).level)
    }

    // ========== QUEUE ==========

    private fun set(id: Int, vararg flashcards: FlashcardDTO) =
        FlashcardSetDTO(id = id, name = "Set $id", flashcards = flashcards.toList())

    @Test
    fun `kolejka - drugie szanse, potem najnizsze poziomy, na koncu nowe`() {
        val sets = listOf(
            set(
                1,
                card(id = 1),
                card(FlashcardStatus.ACTIVE, level = 3, id = 2).copy(dueOn = today),
                card(FlashcardStatus.ACTIVE, level = 1, id = 3).copy(dueOn = today),
                card(FlashcardStatus.FROZEN, level = 4, id = 4).copy(frozenUntil = now.minusSeconds(1)),
                card(FlashcardStatus.ACTIVE, level = 2, id = 5).copy(dueOn = today.plusDays(1))
            )
        )
        val plan = FlashcardsReview.plan(sets, FlashcardsReview.Params(10, 30), today, now)
        assertEquals(listOf(4, 3, 2, 1), plan.queue.map { card -> card.flashcard.id })
        assertEquals(3, plan.repetitionsCount)
        assertEquals(1, plan.newCount)
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
        val plan = FlashcardsReview.plan(sets, FlashcardsReview.Params(newPerDay = 3, backlogThreshold = 30), today, now)
        assertEquals(listOf(2, 3), plan.queue.map { card -> card.flashcard.id })
    }

    @Test
    fun `kolejka - zaleglosci ponad prog wstrzymuja nowe`() {
        val due = (1..6).map { id -> card(FlashcardStatus.ACTIVE, level = 1, id = id).copy(dueOn = today) }
        val sets = listOf(set(1, *(due + card(id = 99)).toTypedArray()))
        val plan = FlashcardsReview.plan(sets, FlashcardsReview.Params(newPerDay = 10, backlogThreshold = 5), today, now)
        assertEquals(0, plan.newCount)
        assertEquals(1, plan.newPausedBacklog)
    }
}
