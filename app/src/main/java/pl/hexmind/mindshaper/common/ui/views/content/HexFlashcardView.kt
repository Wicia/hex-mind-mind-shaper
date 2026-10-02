package pl.hexmind.mindshaper.common.ui.views.content

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.services.FlashcardsSession
import pl.hexmind.mindshaper.services.dto.FlashcardDTO
import pl.hexmind.mindshaper.services.dto.FlashcardSessionState

/**
 * Review of a set's flashcards on the scrim (FlashcardsSessionDialog): flashcards one by one -> end card.
 *
 * - front:       reveal (flip) / skip (to the end of the queue)
 * - back:        thumb up / thumb down (with the answer counters) - both go to the next flashcard
 * - end card:    result of the session, tick = close the scrim
 *
 * The session state lives in the flashcards (see FlashcardsSession) - every change goes out through
 * the callback to be stored, so a revealed but unanswered flashcard waits until it is answered.
 */
class HexFlashcardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private enum class Phase {
        FLASHCARD,
        END
    }

    interface FlashcardsCallback {
        /** Session moved on (revealed / skipped / answered) - the list is to be stored. */
        fun onSessionChanged(flashcards: List<FlashcardDTO>) {}

        /** Tick on the end card. */
        fun onSessionClosed() {}
    }

    private val card: LinearLayout
    private val tvText: TextView
    private val llSummary: LinearLayout
    private val tvSummaryTitle: TextView
    private val tvMastery: TextView
    private val btnLeft: MaterialButton
    private val tvLeftCount: TextView
    private val tvRightCount: TextView
    private val btnRight: MaterialButton
    private val llProgress: LinearLayout
    private val vProgressDone: View
    private val vProgressLeft: View

    private var callback: FlashcardsCallback? = null

    private var flashcards: List<FlashcardDTO> = emptyList()

    // One animation at a time - a second tap mid-flip would change the session twice
    private var isAnimating = false

    init {
        inflate(context, R.layout.common_flashcard_view, this)
        orientation = VERTICAL

        card = findViewById(R.id.ll_flashcard_card)
        tvText = findViewById(R.id.tv_flashcard_text)
        llSummary = findViewById(R.id.ll_flashcard_summary)
        tvSummaryTitle = findViewById(R.id.tv_flashcard_summary_title)
        tvMastery = findViewById(R.id.tv_flashcard_mastery)
        btnLeft = findViewById(R.id.btn_flashcard_left)
        tvLeftCount = findViewById(R.id.tv_flashcard_left_count)
        tvRightCount = findViewById(R.id.tv_flashcard_right_count)
        btnRight = findViewById(R.id.btn_flashcard_right)
        llProgress = findViewById(R.id.ll_flashcard_progress)
        vProgressDone = findViewById(R.id.v_flashcard_progress_done)
        vProgressLeft = findViewById(R.id.v_flashcard_progress_left)

        // Without it the Y rotation looks like the card is bursting out of the screen
        card.cameraDistance = CAMERA_DISTANCE * resources.displayMetrics.density
        // Progress bar runs along the bottom edge - clipped to the rounded corners of the card
        card.clipToOutline = true

        setupListeners()
        render()
    }

    private fun setupListeners() {
        btnLeft.setOnClickListener {
            val index = currentIndex() ?: return@setOnClickListener
            if (isRevealed(index)) answer(index, isCorrect = true) else reveal(index)
        }

        btnRight.setOnClickListener {
            when (phase()) {
                Phase.FLASHCARD -> {
                    val index = currentIndex() ?: return@setOnClickListener
                    if (isRevealed(index)) answer(index, isCorrect = false) else skip(index)
                }
                Phase.END       -> callback?.onSessionClosed()
            }
        }
    }

    private fun phase(): Phase =
        if (FlashcardsSession.isInProgress(flashcards)) Phase.FLASHCARD else Phase.END

    private fun currentIndex(): Int? = FlashcardsSession.currentIndex(flashcards)

    private fun isRevealed(index: Int): Boolean =
        flashcards[index].sessionState == FlashcardSessionState.REVEALED

    // ===========================================
    //      Rendering
    // ===========================================

    private fun render() {
        when (phase()) {
            Phase.FLASHCARD -> renderFlashcard()
            Phase.END       -> renderEndCard()
        }
    }

    private fun renderFlashcard() {
        val index = currentIndex() ?: return
        val flashcard = flashcards[index]
        val isRevealed = isRevealed(index)

        tvText.visibility = VISIBLE
        llSummary.visibility = GONE
        tvText.text = if (isRevealed) flashcard.back else flashcard.front

        if (isRevealed) {
            // Not approve / close icons - in the app those mean "save" and "delete"
            btnLeft.setIconResource(R.drawable.ic_thumb_up)
            btnRight.setIconResource(R.drawable.ic_thumb_down)
            tvLeftCount.text = flashcard.correctCount.toString()
            tvRightCount.text = flashcard.wrongCount.toString()
            tvLeftCount.visibility = VISIBLE
            tvRightCount.visibility = VISIBLE
            btnRight.visibility = VISIBLE
        }
        else {
            btnLeft.setIconResource(R.drawable.ic_path_reveal)
            btnRight.setIconResource(R.drawable.ic_replace_or_renew)
            tvLeftCount.visibility = GONE
            tvRightCount.visibility = GONE
            // Nothing to skip to when it is the last one in the queue - INVISIBLE keeps the row height
            btnRight.visibility = if (FlashcardsSession.queuedCount(flashcards) > 1) VISIBLE else INVISIBLE
        }
        btnLeft.visibility = VISIBLE

        showProgress(
            done = FlashcardsSession.answeredCount(flashcards),
            total = FlashcardsSession.sessionSize(flashcards)
        )
    }

    private fun renderEndCard() {
        tvText.visibility = GONE
        llSummary.visibility = VISIBLE
        tvSummaryTitle.text = context.getString(R.string.flashcards_end_title)

        val mastery = FlashcardsSession.masteryPercent(flashcards)
        tvMastery.visibility = if (mastery != null) VISIBLE else GONE
        mastery?.let { tvMastery.text = context.getString(R.string.flashcards_mastery, it) }

        btnLeft.visibility = INVISIBLE
        tvLeftCount.visibility = GONE
        tvRightCount.visibility = GONE
        btnRight.setIconResource(R.drawable.ic_action_approve)
        btnRight.visibility = VISIBLE

        showProgress(done = 1, total = 1)
    }

    private fun showProgress(done: Int, total: Int) {
        llProgress.visibility = VISIBLE
        val doneRatio = if (total == 0) 0f else done.toFloat() / total
        (vProgressDone.layoutParams as LayoutParams).weight = doneRatio
        (vProgressLeft.layoutParams as LayoutParams).weight = 1f - doneRatio
        llProgress.requestLayout()
    }

    // ===========================================
    //      Session actions
    // ===========================================

    private fun reveal(index: Int) {
        changeSession(flip = true) { FlashcardsSession.reveal(flashcards, index) }
    }

    private fun skip(index: Int) {
        changeSession(flip = false) { FlashcardsSession.skip(flashcards, index) }
    }

    private fun answer(index: Int, isCorrect: Boolean) {
        changeSession(flip = false) { FlashcardsSession.answer(flashcards, index, isCorrect) }
    }

    /** Swaps the list while the card is hidden mid-animation and hands it over to be stored. */
    private fun changeSession(flip: Boolean, change: () -> List<FlashcardDTO>) {
        animateCardChange(flip) {
            flashcards = change()
            callback?.onSessionChanged(flashcards)
        }
    }

    // ===========================================
    //      Animations
    // ===========================================

    /**
     * flip: rotates to the edge, swaps the content while the card is invisible, rotates back
     * fade: fades out, swaps, fades in
     */
    private fun animateCardChange(flip: Boolean, change: () -> Unit) {
        if (isAnimating) return
        isAnimating = true

        val hide = card.animate()
            .setDuration(if (flip) FLIP_HALF_DURATION_MS else FADE_DURATION_MS)
            .setInterpolator(AccelerateInterpolator())
        if (flip) hide.rotationY(90f) else hide.alpha(0f)

        hide.withEndAction {
            change()
            render()

            val show = card.animate()
                .setDuration(if (flip) FLIP_HALF_DURATION_MS else FADE_DURATION_MS)
                .setInterpolator(DecelerateInterpolator())
            if (flip) {
                card.rotationY = -90f
                show.rotationY(0f)
            }
            else {
                show.alpha(1f)
            }
            show.withEndAction { isAnimating = false }.start()
        }.start()
    }

    private fun resetCardAnimation() {
        card.animate().cancel()
        card.rotationY = 0f
        card.alpha = 1f
        isAnimating = false
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // A cancelled animation never runs its end action - without the reset the card stays half-turned
        resetCardAnimation()
    }

    // ===========================================
    //      Public API Methods
    // ===========================================

    fun setCallback(callback: FlashcardsCallback) {
        this.callback = callback
    }

    /**
     * Shows the session exactly where it was left.
     * ! The same list again changes nothing - otherwise every stored step would restart the animation.
     */
    fun setFlashcards(newFlashcards: List<FlashcardDTO>) {
        if (newFlashcards == flashcards) return

        resetCardAnimation()
        flashcards = newFlashcards
        render()
    }

    companion object {
        private const val FLIP_HALF_DURATION_MS = 150L
        private const val FADE_DURATION_MS = 150L
        private const val CAMERA_DISTANCE = 8000f
    }
}
