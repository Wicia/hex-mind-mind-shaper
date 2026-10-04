package pl.hexmind.mindshaper.common.ui.dialogs

import android.content.Context
import android.graphics.Color.TRANSPARENT
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.drawable.toDrawable
import pl.hexmind.mindshaper.R

/**
 * AlertDialog on the app's scrim: no dialog frame, the content sits straight on a strong dim.
 * ! Buttons never close the dialog on their own - each listener decides, so validation can keep it open.
 *
 * Leading action: a button of the content (e.g. "add row", "insert bullet") at the left end of the button bar,
 * in one row with Cancel / Save.
 */
object ScrimDialogs {

    private const val DIM_AMOUNT = 0.9f

    fun create(
        context: Context,
        view: View,
        positiveText: String,
        onPositive: (AlertDialog) -> Unit,
        negativeText: String = context.getString(R.string.common_btn_cancel),
        onNegative: (AlertDialog) -> Unit = { dialog -> dialog.dismiss() },
        leadingAction: View? = null
    ): AlertDialog {
        val dialog = AlertDialog.Builder(context)
            .setView(view)
            // Listeners set on show - the default ones close the dialog before the callback can say no
            .setPositiveButton(positiveText, null)
            .setNegativeButton(negativeText, null)
            .create()

        dialog.window?.apply {
            setBackgroundDrawable(TRANSPARENT.toDrawable())
            setDimAmount(DIM_AMOUNT)
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener { onPositive(dialog) }
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener { onNegative(dialog) }
            styleButtons(dialog)
            leadingAction?.let { action -> moveToButtonBar(dialog, action) }
        }
        return dialog
    }

    private fun styleButtons(dialog: AlertDialog) {
        val negative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
        val positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        // Same size / gap as the text buttons of the other dialogs (dimens) - px, so no sp scaling twice
        val textSize = dialog.context.resources.getDimension(R.dimen.dialog_text_button_size)
        listOf(negative, positive).forEach { button -> button.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize) }

        val gap = dialog.context.resources.getDimensionPixelSize(R.dimen.dialog_text_buttons_gap)
        (positive.layoutParams as? ViewGroup.MarginLayoutParams)?.let { params ->
            params.marginStart = gap
            positive.layoutParams = params
        }
    }

    /**
     * The bar exists only once the dialog is shown. Its spacer (weight 1) keeps Cancel / Save on the right.
     * Visibility and listeners stay with the view - a hidden toolbar stays hidden.
     */
    private fun moveToButtonBar(dialog: AlertDialog, action: View) {
        val buttonBar = dialog.getButton(AlertDialog.BUTTON_POSITIVE).parent as? ViewGroup ?: return
        if (action.parent === buttonBar) return // Shown again

        (action.parent as? ViewGroup)?.removeView(action)
        buttonBar.addView(
            action,
            0,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER_VERTICAL
                marginStart = dialog.context.resources.getDimensionPixelSize(R.dimen.dialog_leading_action_margin_start)
            }
        )
    }
}
