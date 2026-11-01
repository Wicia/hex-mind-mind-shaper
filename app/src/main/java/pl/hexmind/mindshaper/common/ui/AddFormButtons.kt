package pl.hexmind.mindshaper.common.ui

import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import pl.hexmind.mindshaper.R
import kotlin.math.ceil

/**
 * Add-form buttons (style HexContentButton.NotUsed): "+" and the form's icon, or gray "?" and the icon
 * when the form / module is turned off. Capture, Details and the capture ring on Home.
 */
object AddFormButtons {

    private const val TEXT_ENABLED = "+"
    private const val TEXT_DISABLED = "?"

    fun applyState(button: MaterialButton, featureEnabled: Boolean) {
        val context = button.context
        val backgroundRes = if (featureEnabled) R.color.button_content_filled_background else R.color.button_content_empty_background_disabled
        // Icon in the text color in both states
        val contentRes = if (featureEnabled) R.color.button_content_filled_icon else R.color.button_content_empty_icon_disabled

        button.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, backgroundRes))
        button.iconTint = ColorStateList.valueOf(ContextCompat.getColor(context, contentRes))
        button.text = if (featureEnabled) TEXT_ENABLED else TEXT_DISABLED
        button.setTextColor(ContextCompat.getColor(context, contentRes))
        keepWidth(button)
    }

    /**
     * One width for every add-form button, enabled or not - "+" and "?" differ in width in this font.
     * Also for the buttons without a disabled state (note), so the row stays even.
     */
    fun keepWidth(button: MaterialButton) {
        val textWidth = maxOf(button.paint.measureText(TEXT_ENABLED), button.paint.measureText(TEXT_DISABLED))
        button.minWidth = button.paddingStart + button.paddingEnd +
                button.iconSize + button.iconPadding + ceil(textWidth).toInt()
    }
}
