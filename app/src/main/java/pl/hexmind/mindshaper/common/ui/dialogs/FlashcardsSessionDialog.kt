package pl.hexmind.mindshaper.common.ui.dialogs

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.common.ui.views.content.HexFlashcardView
import pl.hexmind.mindshaper.services.FlashcardsReview
import pl.hexmind.mindshaper.services.dto.FlashcardDTO
import pl.hexmind.mindshaper.services.dto.FlashcardRating

/**
 * Review session on a scrim: the planned queue one by one -> end card (tick = close).
 * Tap on the scrim / back = break off - every rating is stored right away, the next session picks up the rest.
 * Opened with [open] - nothing to review = no scrim.
 */
class FlashcardsSessionDialog(
    context: Context,
    private val queue: List<FlashcardsReview.Card>,
    private val onRated: (FlashcardDTO, FlashcardRating) -> Unit
) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.common_flashcards_session_dialog)

        window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            // ! Transparent window - the default dialog background would frame the card
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setDimAmount(SCRIM_DIM_AMOUNT)
            setWindowAnimations(R.style.FlashcardsSessionAnimation)
        }

        // Window is full screen, so a "touch outside" never happens - the scrim is the root view itself
        findViewById<View>(R.id.fl_flashcards_session_scrim).setOnClickListener { dismiss() }

        findViewById<HexFlashcardView>(R.id.flashcards_session_view).apply {
            setQueue(queue)
            setCallback(object : HexFlashcardView.FlashcardsCallback {
                override fun onRated(flashcard: FlashcardDTO, rating: FlashcardRating) {
                    this@FlashcardsSessionDialog.onRated(flashcard, rating)
                }

                override fun onSessionClosed() {
                    dismiss()
                }
            })
        }
    }

    companion object {
        // Same dim as FlashcardsEditDialog / TextEditDialog
        private const val SCRIM_DIM_AMOUNT = 0.9f

        fun open(context: Context, plan: FlashcardsReview.Plan, onRated: (FlashcardDTO, FlashcardRating) -> Unit) {
            if (plan.isEmpty) return
            FlashcardsSessionDialog(context, plan.queue, onRated).show()
        }
    }
}
