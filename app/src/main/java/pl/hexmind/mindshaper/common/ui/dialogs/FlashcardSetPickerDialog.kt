package pl.hexmind.mindshaper.common.ui.dialogs

import android.content.Context
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import com.google.android.material.button.MaterialButton
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.services.dto.FlashcardSetDTO

/**
 * Set a flashcard moves to, on the scrim: one chip per set, "Przenieś" once one is picked.
 */
class FlashcardSetPickerDialog(
    context: Context,
    sets: List<FlashcardSetDTO>,
    private val onPick: (setId: Int) -> Unit
) {

    private val dialogView = LayoutInflater.from(context).inflate(R.layout.common_flashcard_set_picker_dialog, null)
    private val llSets: LinearLayout = dialogView.findViewById(R.id.ll_picker_sets)
    private val dialog: AlertDialog

    private var pickedSetId: Int? = null

    init {
        val chips = sets.mapNotNull { set ->
            val setId = set.id ?: return@mapNotNull null
            val chip = LayoutInflater.from(context)
                .inflate(R.layout.common_flashcard_set_picker_chip, llSets, false) as MaterialButton
            chip.text = set.name
            llSets.addView(chip)
            setId to chip
        }

        dialog = ScrimDialogs.create(
            context, dialogView,
            positiveText = context.getString(R.string.flashcards_btn_move),
            onPositive = { scrimDialog ->
                pickedSetId?.let { setId ->
                    onPick(setId)
                    scrimDialog.dismiss()
                }
            }
        )

        // Single choice - picking one releases the others
        // ! Set after the dialog exists - the listeners enable its Move button
        chips.forEach { (setId, chip) ->
            chip.setOnClickListener {
                chips.forEach { (_, otherChip) -> otherChip.isSelected = otherChip === chip }
                pickedSetId = setId
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
            }
        }
    }

    fun show() {
        dialog.show()
        // Buttons exist only once shown - nothing picked yet
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
    }
}
