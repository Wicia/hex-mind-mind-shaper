package pl.hexmind.mindshaper.services

import android.accounts.Account
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.os.Bundle
import android.provider.CalendarContract
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

/** A calendar the user can write to, offered as a target for reminders */
data class WritableCalendar(
    val id: Long,
    val displayName: String,
    val accountName: String,
    val isPrimary: Boolean
)

/**
 * Writes step reminders into the system calendar as recurring events.
 * Every call assumes the calendar permission is already granted.
 */
@Singleton
class CalendarService @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /** Synced account calendars the user can write to; empty when none exist or permission is missing */
    fun getWritableCalendars(): List<WritableCalendar> {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.IS_PRIMARY
        )
        val selection = "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ? AND " +
                "${CalendarContract.Calendars.ACCOUNT_TYPE} != ?"
        val selectionArgs = arrayOf(
            CalendarContract.Calendars.CAL_ACCESS_OWNER.toString(),
            CalendarContract.ACCOUNT_TYPE_LOCAL
        )

        val calendars = mutableListOf<WritableCalendar>()

        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${CalendarContract.Calendars.IS_PRIMARY} DESC"
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                calendars.add(
                    WritableCalendar(
                        id = cursor.getLong(0),
                        displayName = cursor.getString(1) ?: cursor.getString(2) ?: "",
                        accountName = cursor.getString(2) ?: "",
                        isPrimary = cursor.getInt(3) == 1
                    )
                )
            }
        }

        return calendars
    }

    /**
     * @return id of the created event, or null when there is no writable calendar
     */
    fun createRecurringReminder(
        title: String,
        timeHHmm: String,
        daysCsv: String,
        repetitions: Int,
        calendarId: Long?
    ): Long? {
        val targetCalendarId = calendarId ?: findWritableCalendarId() ?: return null
        val startMillis = nextOccurrenceMillis(timeHHmm, daysCsv) ?: return null

        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, targetCalendarId)
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.DTSTART, startMillis)
            // Recurring events must declare DURATION instead of DTEND
            put(CalendarContract.Events.DURATION, "PT${REMINDER_MINUTES}M")
            put(CalendarContract.Events.RRULE, buildWeeklyRule(daysCsv, repetitions))
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            put(CalendarContract.Events.HAS_ALARM, 1)
        }

        val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
        val eventId = uri?.lastPathSegment?.toLongOrNull() ?: return null

        addCalendarBasedReminder(eventId)

        // Local DB has the event instantly; ask its account (whatever provider) to sync so the cloud catches up
        requestSyncFor(targetCalendarId)

        return eventId
    }

    private fun addCalendarBasedReminder(eventId: Long) {
        // The calendar's own notification is what actually pings the phone at reminder time
        val values = ContentValues().apply {
            put(CalendarContract.Reminders.EVENT_ID, eventId)
            put(CalendarContract.Reminders.MINUTES, NOTIFICATION_MINUTES_BEFORE)
            put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
        }
        context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, values)
    }

    /**
     * Best-effort: without network the system just queues the request and syncs once a connection returns.
     */
    private fun requestSyncFor(calendarId: Long) {
        val account = accountOf(calendarId) ?: return

        val extras = Bundle().apply {
            putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true)
            putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true)
        }
        ContentResolver.requestSync(account, CalendarContract.AUTHORITY, extras)
    }

    private fun accountOf(calendarId: Long): Account? {
        val projection = arrayOf(
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE
        )
        val uri = ContentUris.withAppendedId(CalendarContract.Calendars.CONTENT_URI, calendarId)

        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val name = cursor.getString(0) ?: return null
                val type = cursor.getString(1) ?: return null
                return Account(name, type)
            }
        }

        return null
    }

    fun deleteReminder(eventId: Long) {
        // Read the calendar before deleting - once the event is gone we can't tell which account to sync
        val calendarId = calendarIdOfEvent(eventId)

        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        context.contentResolver.delete(uri, null, null)

        // Push the removal to the cloud right away instead of waiting for the next sync cycle
        if (calendarId != null) requestSyncFor(calendarId)
    }

    private fun calendarIdOfEvent(eventId: Long): Long? {
        val projection = arrayOf(CalendarContract.Events.CALENDAR_ID)
        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)

        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) return cursor.getLong(0)
        }

        return null
    }

    /**
     * Pick a synced account calendar (not a local one) so events leave the device
     */
    private fun findWritableCalendarId(): Long? {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.IS_PRIMARY
        )
        val selection = "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ? AND " +
                "${CalendarContract.Calendars.ACCOUNT_TYPE} != ?"
        val selectionArgs = arrayOf(
            CalendarContract.Calendars.CAL_ACCESS_OWNER.toString(),
            CalendarContract.ACCOUNT_TYPE_LOCAL
        )

        // Primary (the account's main calendar) first, so a shared/family one doesn't win
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${CalendarContract.Calendars.IS_PRIMARY} DESC"
        )?.use { cursor ->
            if (cursor.moveToFirst()) return cursor.getLong(0)
        }

        return null
    }

    /**
     * Nearest [weekday + time] that has not passed yet.
     * Today counts only when the time is still ahead, otherwise the search moves on.
     */
    private fun nextOccurrenceMillis(timeHHmm: String, daysCsv: String): Long? {
        val (hour, minute) = parseTime(timeHHmm) ?: return null
        val days = parseDays(daysCsv)
        if (days.isEmpty()) return null

        val candidate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        repeat(DAYS_IN_WEEK + 1) {
            val isSelectedDay = toIsoWeekday(candidate.get(Calendar.DAY_OF_WEEK)) in days
            if (isSelectedDay && candidate.timeInMillis > System.currentTimeMillis())
                return candidate.timeInMillis

            candidate.add(Calendar.DAY_OF_YEAR, 1)
        }

        return null
    }

    private fun buildWeeklyRule(daysCsv: String, repetitions: Int): String {
        val byDay = parseDays(daysCsv)
            .sorted()
            .joinToString(",") { dayNumber -> RRULE_DAYS[dayNumber - 1] }

        // COUNT caps total occurrences; the calendar rolls onto following weeks until it collects them all
        val count = repetitions.coerceAtLeast(1)

        return "FREQ=WEEKLY;BYDAY=$byDay;COUNT=$count"
    }

    private fun parseTime(timeHHmm: String): Pair<Int, Int>? {
        val parts = timeHHmm.split(":")
        if (parts.size != 2) return null

        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null

        return hour to minute
    }

    private fun parseDays(daysCsv: String): Set<Int> =
        daysCsv.split(",")
            .mapNotNull { day -> day.trim().toIntOrNull() }
            .filter { day -> day in 1..DAYS_IN_WEEK }
            .toSet()

    // Calendar.DAY_OF_WEEK starts on Sunday=1, the app stores Monday=1
    private fun toIsoWeekday(calendarDay: Int): Int =
        if (calendarDay == Calendar.SUNDAY) 7 else calendarDay - 1

    companion object {
        // Event length in the calendar, unrelated to when the notification fires
        private const val REMINDER_MINUTES = 30

        // How long before the step's time the calendar notification pops
        private const val NOTIFICATION_MINUTES_BEFORE = 30
        private const val DAYS_IN_WEEK = 7

        private val RRULE_DAYS = listOf("MO", "TU", "WE", "TH", "FR", "SA", "SU")
    }
}