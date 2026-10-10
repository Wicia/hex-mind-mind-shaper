package pl.hexmind.mindshaper.services.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Flashcards reminder alarm went off - notification (if anything waits) + always the next alarm.
 * ! Services taken through an entry point - no Hilt injection into the receiver itself
 */
class FlashcardsReminderReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface RemindersEntryPoint {
        fun flashcardsReminders(): FlashcardsReminders
    }

    override fun onReceive(context: Context, intent: Intent) {
        val reminders = remindersOf(context)

        // The queue comes from the database - keeps the receiver alive until it is read
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                reminders.notifyIfDue()
            }
            finally {
                reminders.scheduleNext()
                pendingResult.finish()
            }
        }
    }

    companion object {
        fun remindersOf(context: Context): FlashcardsReminders =
            EntryPointAccessors.fromApplication(context.applicationContext, RemindersEntryPoint::class.java)
                .flashcardsReminders()
    }
}
