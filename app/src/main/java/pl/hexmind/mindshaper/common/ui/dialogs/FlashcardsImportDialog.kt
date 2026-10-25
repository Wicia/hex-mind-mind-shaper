package pl.hexmind.mindshaper.common.ui.dialogs

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.activities.flashcards.FlashcardsBank
import pl.hexmind.mindshaper.activities.flashcards.FlashcardsImport
import pl.hexmind.mindshaper.common.ui.TextFilePicker
import pl.hexmind.mindshaper.common.ui.views.HexTabSwitch
import pl.hexmind.mindshaper.common.ui.views.lists.HexOptionTiles
import pl.hexmind.mindshaper.services.dto.FlashcardDTO

/**
 * Text -> new flashcards (loose or of a set - the caller stores them). The user picks the separator, the info below the field
 * says how many flashcards the text makes - Import stays disabled until there is at least one.
 *
 * Source of the text:
 * - Input - typed / pasted into the field
 * - Bank - a file shipped with the app, picked from the list
 * - File - a .txt file from the phone
 * Bank and file land in the field once picked, so they can be checked and fixed before Import.
 */
class FlashcardsImportDialog(
    private val context: Context,
    private val textFilePicker: TextFilePicker,
    private val onImport: (List<FlashcardDTO>) -> Unit
) {

    private enum class Source { INPUT, BANK, FILE }

    private val dialogView = LayoutInflater.from(context).inflate(R.layout.common_flashcards_import_dialog, null)
    private val llSeparator: View = dialogView.findViewById(R.id.ll_flashcards_import_separator)
    private val tilesSeparator: HexOptionTiles = dialogView.findViewById(R.id.tiles_flashcards_import_separator)
    private val tabsSource: HexTabSwitch = dialogView.findViewById(R.id.tabs_flashcards_import_source)
    private val svBank: View = dialogView.findViewById(R.id.sv_flashcards_import_bank)
    private val cgBank: ChipGroup = dialogView.findViewById(R.id.cg_flashcards_import_bank)
    private val btnFile: MaterialButton = dialogView.findViewById(R.id.btn_flashcards_import_file)
    private val llText: View = dialogView.findViewById(R.id.ll_flashcards_import_text)
    private val etImport: TextInputEditText = dialogView.findViewById(R.id.et_flashcards_import)
    private val tvInfo: TextView = dialogView.findViewById(R.id.tv_flashcards_import_info)
    private val tvWarning: View = dialogView.findViewById(R.id.tv_flashcards_import_warning)

    private val bankEntries = FlashcardsBank.entries()
    private val bankChips = mutableListOf<MaterialButton>()

    // No bank files shipped = no Bank tab
    private val sources = Source.entries.filter { source -> source != Source.BANK || bankEntries.isNotEmpty() }
    private var currentSource = Source.INPUT

    private var result = FlashcardsImport.Result(emptyList(), 0)

    private val dialog: AlertDialog = ScrimDialogs.create(
        context, dialogView,
        positiveText = context.getString(R.string.flashcards_import_btn),
        onPositive = { dialog ->
            onImport(result.flashcards)
            val count = result.flashcards.size
            Toast.makeText(context, context.resources.getQuantityString(R.plurals.flashcards_import_done, count, count), Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
    )

    init {
        // ! Centered, the dialog jumped at both edges whenever the source widgets or the text changed its height -
        // pinned to the top it only grows / shrinks downwards
        dialog.window?.attributes = dialog.window?.attributes?.apply {
            gravity = Gravity.TOP
            y = (TOP_OFFSET_DP * context.resources.displayMetrics.density).toInt()
        }

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

        setupSource()
        setupBank()

        // File content lands in the field - the separator and the count work on it as on a typed text
        btnFile.setOnClickListener {
            textFilePicker.pick { text -> showLoadedText(text) }
        }
    }

    // Tab index = position in [sources]; a tap on the selected tab is not reported, so the text stays
    private fun setupSource() {
        tabsSource.setTabs(
            sources.map { source ->
                context.getString(
                    when (source) {
                        Source.INPUT -> R.string.flashcards_import_source_input
                        Source.BANK  -> R.string.flashcards_import_from_bank
                        Source.FILE  -> R.string.flashcards_import_from_file
                    }
                )
            }
        )
        tabsSource.onTabSelected = { index -> showSource(sources[index]) }
        showSource(Source.INPUT)
    }

    // One chip per bank file - single choice, the picked one lands in the field
    private fun setupBank() {
        bankEntries.forEach { entry ->
            val chip = LayoutInflater.from(context)
                .inflate(R.layout.common_flashcard_set_picker_chip, cgBank, false) as MaterialButton
            chip.text = entry.name
            chip.setOnClickListener {
                bankChips.forEach { otherChip -> otherChip.isSelected = otherChip === chip }
                showLoadedText(FlashcardsBank.read(context, entry))
            }
            cgBank.addView(chip)
            bankChips += chip
        }
    }

    /**
     * Every source starts empty - text left from another one would be imported by mistake.
     * Bank / file: the field shows up only once there is something in it.
     */
    private fun showSource(source: Source) {
        currentSource = source
        etImport.setText("")
        bankChips.forEach { chip -> chip.isSelected = false }

        svBank.isVisible = source == Source.BANK
        llSeparator.isVisible = source != Source.BANK
        btnFile.isVisible = source == Source.FILE
        llText.isVisible = source == Source.INPUT

        if (source == Source.FILE) {
            textFilePicker.pick { text -> showLoadedText(text) }
        }
    }

    private fun showLoadedText(text: String) {
        etImport.setText(text)
        llText.isVisible = true
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

    // Bank files always use the dash - the user's choice is kept for the other sources
    private fun selectedSeparator(): FlashcardsImport.Separator =
        if (currentSource == Source.BANK) FlashcardsImport.Separator.DASH
        else FlashcardsImport.Separator.entries[tilesSeparator.getSelectedId()]

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
        tvWarning.isVisible = result.skippedLines > 0 || result.flashcards.isEmpty()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = result.flashcards.isNotEmpty()
    }

    fun show() {
        dialog.show()
        // ! After show - the buttons exist only then (ScrimDialogs owns the show listener)
        refresh()
        etImport.requestFocus()
    }

    private companion object {
        const val TOP_OFFSET_DP = 48
    }
}
