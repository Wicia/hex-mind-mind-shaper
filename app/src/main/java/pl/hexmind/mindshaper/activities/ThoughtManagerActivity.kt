package pl.hexmind.mindshaper.activities

import android.content.Intent
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.activities.settings.SettingsActivity
import pl.hexmind.mindshaper.common.ui.dialogs.ActionsDialog

/**
 * Parent class for all common thought capture & edit functionalities
 */
open class ThoughtManagerActivity : CoreActivity() {

    fun showEnableAdditionalFeaturesDialog() {
        ActionsDialog.Builder(this)
            .setTitle("Nieaktywna forma zapisu myśli")
            .setDescription(getString(R.string.common_thoughts_forms_permissions_needed))
            .setCautionAction(getString(R.string.common_btn_confirm_ok_2)) {
                startActivity(Intent(this, SettingsActivity::class.java))
            }
            .setDismissText("Nie")
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
            addFormButton.backgroundTintList = colorStateListOf(R.color.button_content_filled_background)
            addFormButton.iconTint = colorStateListOf( R.color.button_content_filled_icon)
            addFormButton.text = "+"
            addFormButton.setTextColor(
                ContextCompat.getColor(this, R.color.button_content_filled_icon)
            )
        }
        else { // Disabled
            addFormButton.backgroundTintList = colorStateListOf( R.color.button_content_empty_background_disabled)
            addFormButton.iconTint = colorStateListOf( R.color.button_content_empty_icon_disabled)
            addFormButton.text = "?"
            addFormButton.setTextColor(
                ContextCompat.getColor(this, R.color.button_content_empty_icon_disabled)
            )
        }
    }
}