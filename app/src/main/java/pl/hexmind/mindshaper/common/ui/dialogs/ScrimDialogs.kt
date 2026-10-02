package pl.hexmind.mindshaper.common.ui.dialogs

import android.content.Context
import android.graphics.Color.TRANSPARENT
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.drawable.toDrawable
import pl.hexmind.mindshaper.R

/**
 * AlertDialog on the app's scrim: no dialog frame, the content sits straight on a strong dim.
 * ! Buttons never close the dialog on their own - each listener decides, so validation can keep it open.
 */
object ScrimDialogs {

    private const val DIM_AMOUNT = 0.9f

    fun create(
        context: Context,
        view: View,
        positiveText: String,
        onPositive: (AlertDialog) -> Unit,
        negativeText: String = context.getString(R.string.common_btn_cancel),
        onNegative: (AlertDialog) -> Unit = { dialog -> dialog.dismiss() }
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
        }
        return dialog
    }
}
