package pl.hexmind.mindshaper.common.ui.dialogs

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.common.ui.views.FlashcardEditRow
import pl.hexmind.mindshaper.services.dto.FlashcardDTO

/**
 * One flashcard on the scrim, in one of two modes:
 * - [edit] - an existing flashcard; the rest of the set stays untouched
 * - [addSeries] - new flashcards one after another: "Dodaj kolejną" stores it and clears the fields, "Zakończ" closes
 * The whole set at once: FlashcardsEditDialog.
 */
class FlashcardEditDialog private constructor(
    private val context: Context,
    private val flashcard: FlashcardDTO?, // null = series of new flashcards
    private var number: Int,              // 1-based position in the set - shown in the header
    private val onSave: (FlashcardDTO) -> Unit
) {

    companion object {
        fun edit(context: Context, flashcard: FlashcardDTO, number: Int, onSave: (FlashcardDTO) -> Unit) =
            FlashcardEditDialog(context, flashcard, number, onSave)

        /** @param firstNumber number the first new flashcard gets - the set's size + 1 */
        fun addSeries(context: Context, firstNumber: Int, onAdd: (FlashcardDTO) -> Unit) =
            FlashcardEditDialog(context, null, firstNumber, onAdd)
    }

    private val isSeries = flashcard == null

    private val dialogView = LayoutInflater.from(context).inflate(R.layout.common_flashcard_edit_dialog, null)
    private val tvHeader: TextView = dialogView.findViewById(R.id.tv_header)
    private val tvAddedCount: TextView = dialogView.findViewById(R.id.tv_flashcard_added_count)
    private val row: FlashcardEditRow = dialogView.findViewById(R.id.flashcard_edit_row)
    private val tvError: TextView = dialogView.findViewById(R.id.tv_flashcard_error)
    private val dialog: AlertDialog

    private var addedCount = 0

    init {
        row.bind(flashcard)
        renderHeader()

        dialog = if (isSeries) {
            ScrimDialogs.create(
                context, dialogView,
                positiveText = context.getString(R.string.flashcards_btn_add_next),
                onPositive = { addNext() },
                negativeText = context.getString(R.string.flashcards_btn_finish),
                onNegative = { finishSeries() }
            ).apply {
                // A stray tap next to the fields would throw away the flashcard being typed
                setCanceledOnTouchOutside(false)
            }
        }
        else {
            ScrimDialogs.create(
                context, dialogView,
                positiveText = context.getString(R.string.common_btn_save),
                onPositive = { saveEdited() }
            )
        }
    }

    private fun renderHeader() {
        tvHeader.text = context.getString(
            if (isSeries) R.string.flashcards_new_header else R.string.flashcards_row_label,
            number
        )
        if (addedCount > 0) {
            tvAddedCount.text = context.getString(R.string.flashcards_added_count, addedCount)
            tvAddedCount.visibility = View.VISIBLE
        }
    }

    /** Half-filled flashcard - flagged, the dialog stays */
    private fun validate(): Boolean {
        val complete = row.isComplete()
        tvError.visibility = if (complete) View.GONE else View.VISIBLE
        return complete
    }

    private fun saveEdited() {
        if (!validate()) return
        if (row.isChanged()) onSave(row.getFlashcard())
        dialog.dismiss()
    }

    private fun addNext() {
        if (!validate()) return
        onSave(row.getFlashcard())
        addedCount++
        number++
        renderHeader()
        row.clear()
        row.focusFront()
    }

    private fun finishSeries() {
        // Empty fields = nothing more to add
        if (!row.isEmpty()) {
            if (!validate()) return
            onSave(row.getFlashcard())
        }
        dialog.dismiss()
    }

    fun show() {
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        dialog.show()
        row.focusFront()
    }
}
