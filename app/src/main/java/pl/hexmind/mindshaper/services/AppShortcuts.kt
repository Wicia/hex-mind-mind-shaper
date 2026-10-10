package pl.hexmind.mindshaper.services

import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.activities.capture.CaptureActivity
import pl.hexmind.mindshaper.activities.flashcards.FlashcardsActivity
import pl.hexmind.mindshaper.activities.home.HomeActivity
import pl.hexmind.mindshaper.services.dto.DefaultCaptureForm
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Launcher shortcuts (long press on the app icon). See "Tech - App shortcuts" doc.
 * The set depends on the settings (flashcards feature), so it is registered on app start and again on each such change.
 */
@Singleton
class AppShortcuts @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appSettingsStorage: AppSettingsStorage
) {

    // ! dynamic shortcuts instead of static res/xml/shortcuts.xml - raw XML cannot expand ${applicationId}, so it misses the .debug build
    fun register() {
        val shortcuts = buildList {
            if (appSettingsStorage.isFlashcardsFeatureEnabled()) {
                add(buildShortcut(
                    id           = "new_flashcards",
                    labelRes     = R.string.shortcut_new_flashcards,
                    iconRes      = R.drawable.ic_shortcut_new_flashcards,
                    targetIntent = Intent(context, FlashcardsActivity::class.java)
                        .putExtra(FlashcardsActivity.EXTRA_NEW_SET, true)
                ))
            }
            add(buildCaptureShortcut(
                id          = "new_text",
                labelRes    = R.string.shortcut_new_text,
                iconRes     = R.drawable.ic_shortcut_new_text,
                captureForm = DefaultCaptureForm.TEXT
            ))
            add(buildCaptureShortcut(
                id          = "new_voice",
                labelRes    = R.string.shortcut_new_voice,
                iconRes     = R.drawable.ic_shortcut_new_voice,
                captureForm = DefaultCaptureForm.VOICE
            ))
            add(buildCaptureShortcut(
                id          = "new_photo",
                labelRes    = R.string.shortcut_new_photo,
                iconRes     = R.drawable.ic_shortcut_new_photo,
                captureForm = DefaultCaptureForm.PHOTO
            ))
        }

        // Replaces the whole set: list order is kept, and a shortcut left out (flashcards off,
        // the old generic "new_thought") disappears from the launcher
        ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
    }

    private fun buildCaptureShortcut(
        id                   : String,
        @StringRes labelRes  : Int,
        @DrawableRes iconRes : Int,
        captureForm          : DefaultCaptureForm
    ): ShortcutInfoCompat = buildShortcut(
        id           = id,
        labelRes     = labelRes,
        iconRes      = iconRes,
        targetIntent = Intent(context, CaptureActivity::class.java)
            .putExtra(CaptureActivity.EXTRA_CAPTURE_FORM, captureForm.name)
    )

    private fun buildShortcut(
        id                   : String,
        @StringRes labelRes  : Int,
        @DrawableRes iconRes : Int,
        targetIntent         : Intent
    ): ShortcutInfoCompat {
        val homeIntent = Intent(context, HomeActivity::class.java).apply {
            action = Intent.ACTION_MAIN
        }

        // Shortcut intents must have an action
        targetIntent.action = Intent.ACTION_VIEW

        return ShortcutInfoCompat.Builder(context, id)
            .setShortLabel(context.getString(labelRes))
            .setLongLabel(context.getString(labelRes))
            .setIcon(IconCompat.createWithResource(context, iconRes))
            // Home first, target last: the launcher stacks earlier intents behind the last one,
            // so BACK from a cold start lands on Home instead of leaving the app
            .setIntents(arrayOf(homeIntent, targetIntent))
            .build()
    }
}
