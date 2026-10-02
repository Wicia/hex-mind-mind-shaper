package pl.hexmind.mindshaper.common.ui.dialogs

import android.content.Context
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.common.ui.views.HexInputField

/**
 * First step of a new flashcard set: its name. The flashcards are added right after, in a series
 * (FlashcardEditDialog.addSeries) - a name alone is enough to create the set.
 */
class FlashcardSetNameDialog(
    context: Context,
    private val onCreate: (name: String) -> Unit
) {

    private val dialogView = LayoutInflater.from(context).inflate(R.layout.common_flashcard_set_name_dialog, null)
    private val hifName: HexInputField = dialogView.findViewById(R.id.hif_flashcards_set_name)
    private val dialog: AlertDialog = ScrimDialogs.create(
        context, dialogView,
        positiveText = context.getString(R.string.flashcards_btn_create_set),
        onPositive = { dialog ->
            val name = hifName.getText()
            if (name.isEmpty()) {
                hifName.showError(context.getString(R.string.flashcards_error_no_name))
            }
            else {
                onCreate(name)
                dialog.dismiss()
            }
        }
    )

    init {
        hifName.addTextChangedListener { hifName.clearError() }
    }

    fun show() {
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        dialog.show()
        hifName.requestFocusOnField()
    }
}
