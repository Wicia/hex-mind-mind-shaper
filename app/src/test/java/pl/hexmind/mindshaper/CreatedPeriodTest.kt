package pl.hexmind.mindshaper

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.hexmind.mindshaper.activities.stream.CreatedPeriod
import java.time.LocalDate

class CreatedPeriodTest {

    private val today = LocalDate.of(2026, 10, 18)

    private fun CreatedPeriod.has(daysAgo: Long) = contains(today.minusDays(daysAgo), today)

    @Test
    fun `dzisiaj - tylko dzisiejszy dzien`() {
        assertTrue(CreatedPeriod.TODAY.has(0))
        assertFalse(CreatedPeriod.TODAY.has(1))
    }

    @Test
    fun `od wczoraj - wczoraj i dzisiaj`() {
        assertTrue(CreatedPeriod.SINCE_YESTERDAY.has(0))
        assertTrue(CreatedPeriod.SINCE_YESTERDAY.has(1))
        assertFalse(CreatedPeriod.SINCE_YESTERDAY.has(2))
    }

    @Test
    fun `ostatni tydzien - dzisiaj i 6 dni wstecz`() {
        assertTrue(CreatedPeriod.LAST_WEEK.has(0))
        assertTrue(CreatedPeriod.LAST_WEEK.has(6))
        assertFalse(CreatedPeriod.LAST_WEEK.has(7))
    }

    @Test
    fun `ostatni miesiac - do dnia po tym samym dniu miesiac temu`() {
        assertTrue(CreatedPeriod.LAST_MONTH.contains(LocalDate.of(2026, 9, 19), today))
        assertFalse(CreatedPeriod.LAST_MONTH.contains(LocalDate.of(2026, 9, 18), today))
        assertTrue(CreatedPeriod.LAST_MONTH.has(0))
    }

    @Test
    fun `myśl z przyszlosci - w zadnym okresie`() {
        val tomorrow = today.plusDays(1)
        CreatedPeriod.entries.forEach { period -> assertFalse(period.contains(tomorrow, today)) }
    }
}
