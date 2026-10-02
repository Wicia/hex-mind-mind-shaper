package pl.hexmind.mindshaper.activities.flashcards

import android.content.Context
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.common.ui.dialogs.ActionsDialog

/**
 * Delete confirmations of Utrwalanie - the same one for the X button and the long press.
 */
object FlashcardsDeletion {

    fun confirmSet(context: Context, onDelete: () -> Unit) =
        confirm(context, R.string.flashcards_removing_header, onDelete)

    fun confirmFlashcard(context: Context, onDelete: () -> Unit) =
        confirm(context, R.string.flashcard_removing_header, onDelete)

    private fun confirm(context: Context, headerRes: Int, onDelete: () -> Unit) {
        ActionsDialog.Builder(context)
            .setTitle(context.getString(headerRes))
            .setDescription(context.getString(R.string.flashcards_removing_content))
            .setCautionAction(context.getString(R.string.common_deletion_dialog_yes_2)) { onDelete() }
            .show()
    }
}
