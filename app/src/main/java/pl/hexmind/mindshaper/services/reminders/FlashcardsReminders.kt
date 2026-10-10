package pl.hexmind.mindshaper.services.reminders

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.activities.flashcards.FlashcardsActivity
import pl.hexmind.mindshaper.services.AppSettingsStorage
import pl.hexmind.mindshaper.services.FlashcardsService
import pl.hexmind.mindshaper.services.PermissionService
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Flashcards reminder - local notification once a day at the user's hour: review session (planned reviews + new ones).
 * See "Tech - Notifications" doc.
 *
 * One inexact alarm at a time - it fires once, the receiver orders the next one.
 * The content is counted when the alarm fires: nothing to do = no notification.
 */
@Singleton
class FlashcardsReminders @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appSettingsStorage: AppSettingsStorage,
    private val permissionService: PermissionService,
    private val flashcardsService: FlashcardsService
) {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    /**
     * Replaces the alarm with the nearest reminder hour - safe to call any number of times.
     * Flashcards or reminders off / no permission = no alarm at all.
     */
    fun scheduleNext() {
        if (!isActive()) {
            cancel()
            return
        }

        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            nextTrigger(ZonedDateTime.now()).toInstant().toEpochMilli(),
            alarmIntent()
        )
    }

    fun cancel() {
        alarmManager.cancel(alarmIntent())
    }

    /**
     * Called by the alarm - counts the queue now and stays silent when nothing waits.
     * ! Permission checked in isActive() - the lint does not see through PermissionService
     */
    @SuppressLint("MissingPermission")
    suspend fun notifyIfDue() {
        if (!isActive()) return

        val plan = flashcardsService.planReview(flashcardsService.getAllSets())
        if (plan.isEmpty) return

        ensureChannel()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_flashcards)
            // Android 12+: white brain on an orange circle, older: orange brain - MIUI ignores both and shows the app icon
            .setColor(ContextCompat.getColor(context, R.color.app_primary))
            .setContentTitle(context.getString(R.string.notifications_flashcards_session_title))
            .setContentText(context.getString(R.string.notifications_flashcards_session_text, plan.queue.size))
            .setContentIntent(sessionIntent())
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun isActive(): Boolean =
        appSettingsStorage.isFlashcardsFeatureEnabled() &&
        appSettingsStorage.isFlashcardsRemindersEnabled() &&
        permissionService.isNotificationsGranted()

    // Nearest full reminder hour after now - today or tomorrow
    private fun nextTrigger(now: ZonedDateTime): ZonedDateTime {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone).atTime(appSettingsStorage.getFlashcardsReminderHour(), 0).atZone(zone)
        // Margin - an alarm delivered a moment early must not order the same hour again
        return if (today.isAfter(now.plusMinutes(1))) today else today.plusDays(1)
    }

    // Always the same request code - a new alarm replaces the old one
    private fun alarmIntent(): PendingIntent {
        val intent = Intent(context, FlashcardsReminderReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    // Home under the flashcards screen - back from the session lands in the app, not outside
    private fun sessionIntent(): PendingIntent {
        val intent = Intent(context, FlashcardsActivity::class.java).apply {
            putExtra(FlashcardsActivity.EXTRA_START_SESSION, true)
        }
        return TaskStackBuilder.create(context)
            .addNextIntentWithParentStack(intent)
            .getPendingIntent(SESSION_REQUEST_CODE, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)!!
    }

    private fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notifications_flashcards_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notifications_flashcards_channel_description)
        }
        // Creating an existing channel again is a no-op
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "flashcards_reminders"
        private const val NOTIFICATION_ID = 1001
        private const val ALARM_REQUEST_CODE = 1001
        private const val SESSION_REQUEST_CODE = 0
    }
}
