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
import pl.hexmind.mindshaper.services.FlashcardsSession
import pl.hexmind.mindshaper.services.dto.FlashcardDTO

/**
 * Review session of a set on a scrim: flashcards one by one -> end card (tick = close).
 * Tap on the scrim / back = pause - the session is stored step by step, the rocket of the set resumes it.
 * Opened with [open] - it decides between a new session and resuming the paused one.
 */
class FlashcardsSessionDialog(
    context: Context,
    private val flashcards: List<FlashcardDTO>,
    private val onSessionChanged: (List<FlashcardDTO>) -> Unit
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
            setFlashcards(flashcards)
            setCallback(object : HexFlashcardView.FlashcardsCallback {
                override fun onSessionChanged(flashcards: List<FlashcardDTO>) {
                    this@FlashcardsSessionDialog.onSessionChanged(flashcards)
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

        /** A paused session is resumed where it was left, a new one only when there is none in progress. */
        fun open(context: Context, flashcards: List<FlashcardDTO>, onSessionChanged: (List<FlashcardDTO>) -> Unit) {
            if (flashcards.isEmpty()) return

            val session = if (FlashcardsSession.isInProgress(flashcards)) {
                flashcards
            }
            else {
                // Stored right away - closing the scrim before the first answer still keeps the session
                FlashcardsSession.start(flashcards).also(onSessionChanged)
            }
            FlashcardsSessionDialog(context, session, onSessionChanged).show()
        }
    }
}
