package pl.hexmind.mindshaper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.hexmind.mindshaper.services.FlashcardsReview
import pl.hexmind.mindshaper.services.dto.FlashcardDTO
import pl.hexmind.mindshaper.services.dto.FlashcardSetDTO
import pl.hexmind.mindshaper.services.dto.FlashcardStatus
import pl.hexmind.mindshaper.services.dto.FlashcardsLibraryDTO
import java.time.LocalDate

class FlashcardsReviewTest {

    private val today = LocalDate.of(2026, 10, 3)

    private fun new(id: Int) =
        FlashcardDTO(id = id, front = "Q$id", back = "A$id")

    private fun due(id: Int, level: Int = 1, dueOn: LocalDate? = today) =
        FlashcardDTO(id = id, front = "Q$id", back = "A$id", status = FlashcardStatus.ACTIVE, level = level, dueOn = dueOn)

    private fun mastered(id: Int) =
        FlashcardDTO(id = id, front = "Q$id", back = "A$id", status = FlashcardStatus.MASTERED, level = 5)

    private fun set(id: Int?, vararg flashcards: FlashcardDTO) =
        FlashcardSetDTO(id = id, name = "Set $id", flashcards = flashcards.toList())

    private fun plan(library: FlashcardsLibraryDTO, newPerDay: Int = 10, setId: Int? = null) =
        FlashcardsReview.plan(library, FlashcardsReview.Params(newPerDay = newPerDay), today, setId)

    private fun FlashcardsReview.Plan.ids() = queue.map { card -> card.flashcard.id }

    // ========== DAILY LIMIT OF NEW ==========

    @Test
    fun `limit - bierze tylko tyle nowych, ile pozwala limit`() {
        val library = FlashcardsLibraryDTO(listOf(set(1, new(1), new(2), new(3), new(4))))
        val plan = plan(library, newPerDay = 2)
        assertEquals(listOf(1, 2), plan.ids())
        assertEquals(2, plan.newCount)
    }

    @Test
    fun `limit - wyczerpany dzisiaj = zadnych nowych, planowe zostaja`() {
        val introduced = (1..3).map { id -> due(id, dueOn = today.plusDays(1)).copy(introducedOn = today) }
        val library = FlashcardsLibraryDTO(listOf(set(1, *introduced.toTypedArray(), due(4), new(5))))
        val plan = plan(library, newPerDay = 3)
        assertEquals(listOf(4), plan.ids())
        assertEquals(0, plan.newCount)
    }

    @Test
    fun `limit - wprowadzone ponad limit nie daja ujemnej liczby`() {
        val introduced = (1..5).map { id -> due(id, dueOn = today.plusDays(1)).copy(introducedOn = today) }
        val library = FlashcardsLibraryDTO(listOf(set(1, *introduced.toTypedArray(), new(6))))
        assertEquals(0, plan(library, newPerDay = 2).newCount)
    }

    @Test
    fun `limit - wprowadzone wczoraj nie licza sie do dzisiejszego`() {
        val yesterday = due(1, dueOn = today.plusDays(1)).copy(introducedOn = today.minusDays(1))
        val library = FlashcardsLibraryDTO(listOf(set(1, yesterday, new(2))))
        assertEquals(listOf(2), plan(library, newPerDay = 1).ids())
    }

    // ========== ORDER ==========

    @Test
    fun `kolejnosc - planowe przed nowymi, nawet z wyzszym poziomem`() {
        val library = FlashcardsLibraryDTO(listOf(set(1, new(1), due(2, level = 5))))
        assertEquals(listOf(2, 1), plan(library).ids())
    }

    @Test
    fun `kolejnosc - nowe ze starszych zestawow najpierw, w zestawie jego kolejnosc`() {
        val library = FlashcardsLibraryDTO(
            listOf(
                set(7, new(71), new(72)),
                set(3, new(32), new(31))
            )
        )
        assertEquals(listOf(32, 31, 71, 72), plan(library).ids())
    }

    @Test
    fun `kolejnosc - planowe z luznych i zestawow w jednej kolejce`() {
        val library = FlashcardsLibraryDTO(
            sets  = listOf(set(1, due(1, level = 2))),
            loose = listOf(due(2, level = 1))
        )
        val plan = plan(library)
        assertEquals(listOf(2, 1), plan.ids())
        assertEquals(2, plan.repetitionsCount)
    }

    // ========== SET SESSION ==========

    @Test
    fun `sesja zestawu - tylko fiszki tego zestawu`() {
        val library = FlashcardsLibraryDTO(
            sets  = listOf(set(1, due(1), new(2)), set(2, due(3), new(4))),
            loose = listOf(due(5), new(6))
        )
        val plan = plan(library, setId = 2)
        assertEquals(listOf(3, 4), plan.ids())
        assertTrue(plan.queue.all { card -> card.setId == 2 })
    }

    @Test
    fun `sesja zestawu - limit liczony ze wszystkich fiszek`() {
        val introducedElsewhere = due(1, dueOn = today.plusDays(1)).copy(introducedOn = today)
        val library = FlashcardsLibraryDTO(
            sets  = listOf(set(1, new(11), new(12))),
            loose = listOf(introducedElsewhere)
        )
        assertEquals(listOf(11), plan(library, newPerDay = 2, setId = 1).ids())
    }

    @Test
    fun `sesja zestawu - nieznany zestaw = pusta kolejka`() {
        val library = FlashcardsLibraryDTO(listOf(set(1, due(1), new(2))))
        assertTrue(plan(library, setId = 99).isEmpty)
    }

    // ========== BOUNDARIES ==========

    @Test
    fun `granice - termin dzisiaj i zalegly wchodza, jutrzejszy nie`() {
        val library = FlashcardsLibraryDTO(
            listOf(set(1, due(1, dueOn = today), due(2, dueOn = today.minusDays(3)), due(3, dueOn = today.plusDays(1))))
        )
        assertEquals(setOf(1, 2), plan(library).ids().toSet())
    }

    @Test
    fun `granice - aktywna bez terminu traktowana jako do powtorki`() {
        val library = FlashcardsLibraryDTO(listOf(set(1, due(1, dueOn = null))))
        assertEquals(listOf(1), plan(library).ids())
    }

    @Test
    fun `granice - utrwalone pomijane`() {
        val library = FlashcardsLibraryDTO(listOf(set(1, mastered(1), due(2), new(3))))
        val plan = plan(library)
        assertFalse(1 in plan.ids())
        assertEquals(listOf(2, 3), plan.ids())
    }

    @Test
    fun `granice - puste zestawy i zestaw bez id pomijane`() {
        val library = FlashcardsLibraryDTO(listOf(set(1), set(null, due(1), new(2)), set(2, new(3))))
        assertEquals(listOf(3), plan(library).ids())
    }

    @Test
    fun `granice - nic do zrobienia = pusty plan`() {
        val library = FlashcardsLibraryDTO(listOf(set(1, mastered(1), due(2, dueOn = today.plusDays(2)))))
        val plan = plan(library)
        assertTrue(plan.isEmpty)
        assertEquals(0, plan.repetitionsCount)
        assertEquals(0, plan.newCount)
    }

    // ========== SET STATS ==========

    @Test
    fun `statystyki zestawu - do powtorki i utrwalone`() {
        val stats = FlashcardsReview.statsOf(
            set(1, due(1), due(2, dueOn = today.plusDays(1)), mastered(3), mastered(4), new(5)),
            today
        )
        assertEquals(1, stats.dueCount)
        assertEquals(2, stats.masteredCount)
    }
}
