package pl.hexmind.mindshaper.activities.flashcards

import android.content.Context
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.common.ui.dialogs.ActionsDialog

/**
 * Delete confirmations of Utrwalanie - the same one for the X button and the long press.
 */
object FlashcardsDeletion {

    fun confirmSet(context: Context, onDelete: () -> Unit) = confirm(
        context, R.string.flashcards_removing_header, R.string.flashcards_removing_content,
        R.string.flashcards_removing_yes, onDelete
    )

    fun confirmFlashcard(context: Context, onDelete: () -> Unit) = confirm(
        context, R.string.flashcard_removing_header, R.string.common_deletion_dialog_warning,
        R.string.flashcard_removing_yes, onDelete
    )

    private fun confirm(context: Context, headerRes: Int, contentRes: Int, confirmRes: Int, onDelete: () -> Unit) {
        ActionsDialog.Builder(context)
            .setTitle(context.getString(headerRes))
            .setDescription(context.getString(contentRes))
            .setPrimaryAction(context.getString(confirmRes), caution = true) { onDelete() }
            .show()
    }
}
