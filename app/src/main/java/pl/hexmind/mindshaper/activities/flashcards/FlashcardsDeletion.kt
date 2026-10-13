package pl.hexmind.mindshaper.activities.flashcards

import android.content.Context
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.common.ui.dialogs.ActionsDialog
import pl.hexmind.mindshaper.services.dto.FlashcardSetDTO

/**
 * Delete confirmations of the flashcards screens - the same one for the X button and the long press.
 */
object FlashcardsDeletion {

    /**
     * A set with flashcards asks what happens to them - deleted with it, or kept as loose ones.
     * @param onDelete keepFlashcards: true = the flashcards become loose
     */
    fun confirmSet(context: Context, set: FlashcardSetDTO, onDelete: (keepFlashcards: Boolean) -> Unit) {
        if (set.flashcards.isEmpty()) {
            confirm(
                context, R.string.flashcards_removing_header, R.string.common_deletion_dialog_warning,
                R.string.flashcards_removing_yes
            ) { onDelete(false) }
            return
        }

        ActionsDialog.Builder(context)
            .setTitle(context.getString(R.string.flashcards_removing_header))
            .setDescription(context.getString(R.string.flashcards_removing_content))
            .setPrimaryAction(context.getString(R.string.flashcards_removing_with_flashcards), caution = true) { onDelete(false) }
            .setSecondaryAction(context.getString(R.string.flashcards_removing_keep_loose)) { onDelete(true) }
            .show()
    }

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
