package pl.hexmind.mindshaper.activities.stream

import java.time.LocalDate

/**
 * Stream filter by the day a thought was created - whole calendar days, today included.
 * No period chosen (null) = any time.
 */
enum class CreatedPeriod {
    TODAY,
    SINCE_YESTERDAY, // Yesterday and today - each period holds the shorter ones
    LAST_WEEK,       // Today and the 6 days before
    LAST_MONTH;      // Today and back to the same day a month ago (excluded)

    fun contains(createdOn: LocalDate, today: LocalDate): Boolean =
        when (this) {
            TODAY           -> createdOn == today
            SINCE_YESTERDAY -> isBetween(createdOn, today.minusDays(1), today)
            LAST_WEEK       -> isBetween(createdOn, today.minusDays(6), today)
            LAST_MONTH      -> isBetween(createdOn, today.minusMonths(1).plusDays(1), today)
        }

    // ! A thought "from the future" (phone clock moved back) is not in any period
    private fun isBetween(day: LocalDate, from: LocalDate, to: LocalDate): Boolean =
        !day.isBefore(from) && !day.isAfter(to)
}
