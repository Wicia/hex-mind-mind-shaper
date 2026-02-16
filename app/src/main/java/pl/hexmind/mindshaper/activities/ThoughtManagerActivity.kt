package pl.hexmind.mindshaper.activities

import android.content.Intent
import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.activities.settings.SettingsActivity

/**
 * Parent class for all common thought capture & edit functionalities
 */
open class ThoughtManagerActivity : CoreActivity() {

    fun showEnableAdditionalFeaturesDialog() { // TODO: use custom dialog + strings.xml
        MaterialAlertDialogBuilder(this)
            .setTitle("Nieaktywna forma zapisu myśli")
            .setMessage("" +
                    "Możesz aktywować zapisywanie myśli w formie nagrań głosowych oraz zdjęć w Ustawieniach.\n\n" +
                    "Czy przejść do Ustawień?")
            .setPositiveButton("Tak") { _, _ ->
                startActivity(Intent(this, SettingsActivity::class.java))
            }
            .setNegativeButton("Nie", null)
            .show()
    }

    /**
     * Updates adding buttons visual state based on feature enabled/disabled
     */
    protected fun updateAddButtonVisualState(
        addFormButton: MaterialButton,
        featureEnabled: Boolean
    ) {
        if (featureEnabled) {
            addFormButton.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.button_content_filled_background)
            )

            addFormButton.iconTint = ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.button_content_filled_icon)
            )

            addFormButton.text = "+"
            addFormButton.setTextColor(
                ContextCompat.getColor(this, R.color.button_content_filled_icon)
            )
        }
        else { // Disabled
            addFormButton.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.button_content_empty_background_disabled)
            )

            addFormButton.iconTint = ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.button_content_empty_icon_disabled)
            )

            addFormButton.text = "?"
            addFormButton.setTextColor(
                ContextCompat.getColor(this, R.color.button_content_empty_icon_disabled)
            )
        }
    }
}