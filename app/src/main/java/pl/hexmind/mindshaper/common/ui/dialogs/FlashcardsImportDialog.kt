package pl.hexmind.mindshaper.common.ui.dialogs

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.MotionEvent
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import com.google.android.material.textfield.TextInputEditText
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.activities.flashcards.FlashcardsImport
import pl.hexmind.mindshaper.common.ui.views.lists.HexOptionTiles
import pl.hexmind.mindshaper.services.dto.FlashcardDTO

/**
 * Pasted text -> new flashcards (loose or of a set - the caller stores them). The user picks the separator, the info below the field
 * says how many flashcards the text makes - Import stays disabled until there is at least one.
 */
class FlashcardsImportDialog(
    private val context: Context,
    private val onImport: (List<FlashcardDTO>) -> Unit
) {

    private val dialogView = LayoutInflater.from(context).inflate(R.layout.common_flashcards_import_dialog, null)
    private val tilesSeparator: HexOptionTiles = dialogView.findViewById(R.id.tiles_flashcards_import_separator)
    private val etImport: TextInputEditText = dialogView.findViewById(R.id.et_flashcards_import)
    private val tvInfo: TextView = dialogView.findViewById(R.id.tv_flashcards_import_info)

    private var result = FlashcardsImport.Result(emptyList(), 0)

    private val dialog: AlertDialog = ScrimDialogs.create(
        context, dialogView,
        positiveText = context.getString(R.string.flashcards_import_btn),
        onPositive = { dialog ->
            onImport(result.flashcards)
            dialog.dismiss()
        }
    )

    init {
        // Tile id = Separator ordinal
        tilesSeparator.setOptions(
            listOf(
                HexOptionTiles.Option(FlashcardsImport.Separator.NEW_LINE.ordinal, R.string.flashcards_import_separator_new_line),
                HexOptionTiles.Option(FlashcardsImport.Separator.DASH.ordinal, R.string.flashcards_import_separator_dash)
            )
        )
        tilesSeparator.setSelectedId(FlashcardsImport.Separator.NEW_LINE.ordinal)
        tilesSeparator.onSelectionChanged = { refresh() }
        etImport.addTextChangedListener { refresh() }
        setupFieldScroll()
    }

    /**
     * ! Past maxLines the dialog's parents took the drag and the field never scrolled.
     * While the text overflows the field, the drag stays with it.
     */
    @SuppressLint("ClickableViewAccessibility") // Returns false - taps and clicks still reach the field
    private fun setupFieldScroll() {
        etImport.setOnTouchListener { field, event ->
            val canScroll = field.canScrollVertically(1) || field.canScrollVertically(-1)
            if (canScroll) {
                val isDragging = event.actionMasked != MotionEvent.ACTION_UP && event.actionMasked != MotionEvent.ACTION_CANCEL
                field.parent.requestDisallowInterceptTouchEvent(isDragging)
            }
            false
        }
    }

    private fun selectedSeparator(): FlashcardsImport.Separator =
        FlashcardsImport.Separator.entries[tilesSeparator.getSelectedId()]

    private fun refresh() {
        val separator = selectedSeparator()
        etImport.hint = context.getString(
            when (separator) {
                FlashcardsImport.Separator.NEW_LINE -> R.string.flashcards_import_hint_new_line
                FlashcardsImport.Separator.DASH     -> R.string.flashcards_import_hint_dash
            }
        )

        result = FlashcardsImport.parse(etImport.text?.toString().orEmpty(), separator)
        val count = context.resources.getQuantityString(R.plurals.flashcards_import_count, result.flashcards.size, result.flashcards.size)
        tvInfo.text =
            if (result.skippedLines > 0) count + " " + context.getString(R.string.flashcards_import_skipped, result.skippedLines)
            else count

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = result.flashcards.isNotEmpty()
    }

    fun show() {
        dialog.show()
        // ! After show - the buttons exist only then (ScrimDialogs owns the show listener)
        refresh()
        etImport.requestFocus()
    }
}
