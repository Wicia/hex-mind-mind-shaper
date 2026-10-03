package pl.hexmind.mindshaper.common.ui.dialogs

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.common.ui.views.FlashcardEditRow
import pl.hexmind.mindshaper.common.ui.views.HexInputField
import pl.hexmind.mindshaper.services.dto.FlashcardDTO

/**
 * One flashcard on the scrim, in one of three modes:
 * - [edit] - an existing flashcard; the rest of the set stays untouched
 * - [addSeries] - new flashcards one after another: "Dodaj kolejną" stores it and clears the fields, "Zakończ" closes
 * - [newSet] - the set's name first ("Utwórz"), then the same dialog goes on as a series - no second dialog
 * The whole set at once: FlashcardsEditDialog.
 */
class FlashcardEditDialog private constructor(
    private val context: Context,
    private val flashcard: FlashcardDTO?, // null = series of new flashcards
    private var number: Int,              // 1-based position in the set - shown in the header
    private val onSave: (FlashcardDTO) -> Unit,
    private val newSet: NewSetSteps? = null
) {

    /** Steps around a series that starts with naming a new set */
    class NewSetSteps(
        // Create the set, then call onCreated - the series starts once it exists
        val onCreateSet: (name: String, onCreated: () -> Unit) -> Unit,
        // "Zakończ" of the series - e.g. open the new set
        val onFinish: () -> Unit
    )

    companion object {
        fun edit(context: Context, flashcard: FlashcardDTO, number: Int, onSave: (FlashcardDTO) -> Unit) =
            FlashcardEditDialog(context, flashcard, number, onSave)

        /** @param firstNumber number the first new flashcard gets - the set's size + 1 */
        fun addSeries(context: Context, firstNumber: Int, onAdd: (FlashcardDTO) -> Unit) =
            FlashcardEditDialog(context, null, firstNumber, onAdd)

        /** Name of a new set, then its flashcards in a series - all in one dialog */
        fun newSet(
            context: Context,
            onCreateSet: (name: String, onCreated: () -> Unit) -> Unit,
            onAdd: (FlashcardDTO) -> Unit,
            onFinish: () -> Unit
        ) = FlashcardEditDialog(context, null, 1, onAdd, NewSetSteps(onCreateSet, onFinish))
    }

    private val isSeries = flashcard == null

    // New set: naming step until the set is created, the series after
    private var isNamingSet = newSet != null

    private val dialogView = LayoutInflater.from(context).inflate(R.layout.common_flashcard_edit_dialog, null)
    private val tvHeader: TextView = dialogView.findViewById(R.id.tv_header)
    private val tvAddedCount: TextView = dialogView.findViewById(R.id.tv_flashcard_added_count)
    private val hifSetName: HexInputField = dialogView.findViewById(R.id.hif_flashcard_set_name)
    private val row: FlashcardEditRow = dialogView.findViewById(R.id.flashcard_edit_row)
    private val tvError: TextView = dialogView.findViewById(R.id.tv_flashcard_error)
    private val dialog: AlertDialog

    private var addedCount = 0

    init {
        row.bind(flashcard)
        renderHeader()

        dialog = when {
            isNamingSet -> {
                hifSetName.visibility = View.VISIBLE
                row.visibility = View.GONE
                hifSetName.addTextChangedListener { hifSetName.clearError() }

                ScrimDialogs.create(
                    context, dialogView,
                    positiveText = context.getString(R.string.flashcards_btn_create_set),
                    onPositive = { if (isNamingSet) createSet() else addNext() },
                    onNegative = { scrimDialog -> if (isNamingSet) scrimDialog.dismiss() else finishSeries() }
                ).apply {
                    // A stray tap next to the fields would throw away what is being typed
                    setCanceledOnTouchOutside(false)
                }
            }
            isSeries -> {
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
            else -> {
                ScrimDialogs.create(
                    context, dialogView,
                    positiveText = context.getString(R.string.common_btn_save),
                    onPositive = { saveEdited() }
                )
            }
        }
    }

    private fun renderHeader() {
        tvHeader.text =
            if (isNamingSet) context.getString(R.string.flashcards_create_header)
            else context.getString(if (isSeries) R.string.flashcards_new_header else R.string.flashcards_row_label, number)
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
        if (row.isLearnedContentChanged()) {
            Toast.makeText(context, R.string.flashcards_edit_progress_info, Toast.LENGTH_LONG).show()
        }
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
        newSet?.onFinish?.invoke()
    }

    private fun createSet() {
        val steps = newSet ?: return
        val name = hifSetName.getText()
        if (name.isEmpty()) {
            hifSetName.showError(context.getString(R.string.flashcards_error_no_name))
            return
        }

        // Created asynchronously - a second tap meanwhile would create the set twice
        val btnPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        btnPositive.isEnabled = false
        steps.onCreateSet(name) {
            btnPositive.isEnabled = true
            startSeries()
        }
    }

    /** Name field out, flashcard fields in - the dialog goes on as a series of the new set */
    private fun startSeries() {
        isNamingSet = false
        hifSetName.visibility = View.GONE
        row.visibility = View.VISIBLE
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).text = context.getString(R.string.flashcards_btn_add_next)
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).text = context.getString(R.string.flashcards_btn_finish)
        renderHeader()
        row.focusFront()
    }

    fun show() {
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        dialog.show()
        if (isNamingSet) hifSetName.requestFocusOnField() else row.focusFront()
    }
}
