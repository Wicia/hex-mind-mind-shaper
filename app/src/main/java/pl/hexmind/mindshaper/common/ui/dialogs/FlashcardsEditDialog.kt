package pl.hexmind.mindshaper.common.ui.dialogs

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.button.MaterialButton
import pl.hexmind.mindshaper.common.ui.views.FlashcardEditRow
import pl.hexmind.mindshaper.common.ui.views.HexInputField
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.services.dto.FlashcardDTO
import pl.hexmind.mindshaper.services.dto.FlashcardSetDTO

/**
 * Edits a flashcard set: its name + the whole flashcard list at once, one FlashcardEditRow per flashcard.
 * One flashcard / adding in a series: FlashcardEditDialog.
 */
class FlashcardsEditDialog(
    private val context: Context,
    private val set: FlashcardSetDTO,
    private val onSave: (name: String, flashcards: List<FlashcardDTO>) -> Unit
) {

    private val dialogView = LayoutInflater.from(context).inflate(R.layout.common_flashcards_edit_dialog, null)
    private val hifName: HexInputField = dialogView.findViewById(R.id.hif_flashcards_set_name)
    private val svRows: ScrollView = dialogView.findViewById(R.id.sv_flashcards)
    private val llRows: LinearLayout = dialogView.findViewById(R.id.ll_flashcards_rows)
    private val tvError: TextView = dialogView.findViewById(R.id.tv_flashcards_error)
    private val dialog: AlertDialog

    // ! Without a cap the list grows with every row and pushes the add button off the screen
    private val maxRowsHeight = (context.resources.displayMetrics.heightPixels * ROWS_MAX_SCREEN_RATIO).toInt()

    // Row just added with the add button - shown (scrolled to) once the list has been laid out
    private var rowToReveal: FlashcardEditRow? = null

    // Gap between the rows
    private val rowSpacing = context.resources.getDimensionPixelSize(R.dimen.flashcards_edit_row_spacing)

    init {
        if (set.flashcards.isEmpty()) addRow() else set.flashcards.forEach { flashcard -> addRow(flashcard) }

        hifName.setText(set.name)

        val btnAdd = dialogView.findViewById<MaterialButton>(R.id.btn_flashcard_add)
        btnAdd.setOnClickListener {
            rowToReveal = addRow()
        }

        llRows.addOnLayoutChangeListener { _, _, top, _, bottom, _, _, _, _ ->
            capRowsHeight(contentHeight = bottom - top)
            revealAddedRow()
        }

        dialog = ScrimDialogs.create(
            context, dialogView,
            positiveText = context.getString(R.string.common_btn_save),
            onPositive = { handleSave() },
            // "+" in one row with Cancel / Save
            leadingAction = btnAdd
        )
    }

    /** Wraps the list until it reaches the cap, then keeps a fixed height and scrolls inside. */
    private fun capRowsHeight(contentHeight: Int) {
        val desiredHeight = if (contentHeight > maxRowsHeight) maxRowsHeight else ViewGroup.LayoutParams.WRAP_CONTENT
        if (svRows.layoutParams.height == desiredHeight) return

        // Posted - changing a size from inside a layout pass would be ignored until the next one
        svRows.post {
            svRows.layoutParams = svRows.layoutParams.apply { height = desiredHeight }
        }
    }

    /** New rows land at the bottom - the top of the list slides up out of view. */
    private fun revealAddedRow() {
        val row = rowToReveal ?: return
        rowToReveal = null

        svRows.post {
            svRows.fullScroll(View.FOCUS_DOWN)
            row.focusFront()
        }
    }

    private fun addRow(flashcard: FlashcardDTO? = null): FlashcardEditRow {
        val row = FlashcardEditRow(context).apply {
            bind(flashcard)
            setOnRemoveClickListener {
                llRows.removeView(this)
                renumberRows()
            }
        }

        val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            .apply { bottomMargin = rowSpacing }
        llRows.addView(row, params)
        renumberRows()
        return row
    }

    private fun rows(): List<FlashcardEditRow> =
        (0 until llRows.childCount).map { index -> llRows.getChildAt(index) as FlashcardEditRow }

    private fun renumberRows() {
        rows().forEachIndexed { index, row -> row.setNumber(index + 1) }
    }

    private fun handleSave() {
        val name = hifName.getText()
        if (name.isEmpty()) {
            tvError.text = context.getString(R.string.flashcards_error_no_name)
            tvError.visibility = View.VISIBLE
            return
        }

        // Rows left completely empty are just skipped; half-filled ones are a mistake worth flagging
        val filledRows = rows().filterNot { row -> row.isEmpty() }
        if (filledRows.any { row -> !row.isComplete() }) {
            tvError.text = context.getString(R.string.flashcards_error_incomplete)
            tvError.visibility = View.VISIBLE
            return
        }

        if (filledRows.any { row -> row.isLearnedContentChanged() }) {
            Toast.makeText(context, R.string.flashcards_edit_progress_info, Toast.LENGTH_LONG).show()
        }
        onSave(name, filledRows.map { row -> row.getFlashcard() })
        dialog.dismiss()
    }

    fun show() {
        dialog.show()
    }

    companion object {
        // Room left for the header, the add button, the dialog buttons and the keyboard
        private const val ROWS_MAX_SCREEN_RATIO = 0.4f
    }
}
