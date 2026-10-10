package pl.hexmind.mindshaper.services.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Orders the reminder alarm again when the system lost it or it points at a wrong moment:
 * - phone restart - the system forgets all the alarms
 * - time / time zone change - the alarm is a moment in time, not "19:00 on the clock"
 */
class RemindersRescheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> FlashcardsReminderReceiver.remindersOf(context).scheduleNext()
        }
    }
}
