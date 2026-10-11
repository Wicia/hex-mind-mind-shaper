package pl.hexmind.mindshaper.activities.flashcards

import android.content.Context
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.common.ui.dialogs.ActionsDialog
import pl.hexmind.mindshaper.common.ui.dialogs.FlashcardSetPickerDialog
import pl.hexmind.mindshaper.services.dto.FlashcardSetDTO

/**
 * Where a flashcard moves - the Move button of the flashcard's edit dialog.
 * From a set: to another set (picker) or to the loose ones. A loose one: straight to the set picker.
 */
object FlashcardMove {

    /** Nowhere to move = no Move button. A flashcard of a set can always become loose. */
    fun canMove(fromSetId: Int?, sets: List<FlashcardSetDTO>): Boolean =
        fromSetId != null || sets.isNotEmpty()

    /**
     * @param fromSetId null = a loose flashcard
     * @param onMove toSetId: null = to the loose ones
     */
    fun start(context: Context, fromSetId: Int?, sets: List<FlashcardSetDTO>, onMove: (toSetId: Int?) -> Unit) {
        val targetSets = sets.filter { set -> set.id != fromSetId }
        val pickSet = { FlashcardSetPickerDialog(context, targetSets, onPick = onMove).show() }

        if (fromSetId == null) {
            pickSet()
            return
        }

        val builder = ActionsDialog.Builder(context)
            .setTitle(context.getString(R.string.flashcard_move_header))

        // No other set - becoming loose is the only way out
        if (targetSets.isEmpty()) {
            builder.setPrimaryAction(context.getString(R.string.flashcard_move_to_loose)) { onMove(null) }
        }
        else {
            builder
                .setDescription(context.getString(R.string.flashcard_move_content))
                .setPrimaryAction(context.getString(R.string.flashcard_move_to_set)) { pickSet() }
                .setSecondaryAction(context.getString(R.string.flashcard_move_to_loose)) { onMove(null) }
        }
        builder.show()
    }
}
