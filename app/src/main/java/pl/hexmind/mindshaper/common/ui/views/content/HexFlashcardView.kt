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
import pl.hexmind.mindshaper.services.FlashcardsReview
import pl.hexmind.mindshaper.services.dto.FlashcardDTO
import pl.hexmind.mindshaper.services.dto.FlashcardRating

/**
 * Review session on the scrim (FlashcardsSessionDialog): flashcards of the queue one by one -> end card.
 *
 * - front:       reveal (flip) / skip (to the end of the queue)
 * - back:        good (thumb up) / ok (tilde) / bad (thumb down) - each goes to the next flashcard
 * - end card:    the session's ratings, tick = close the scrim
 *
 * Every rating goes out through the callback right away to be stored - the queue itself is not stored,
 * the next session builds it again from the flashcards' progress (see FlashcardsReview).
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
        /** Flashcard rated - its progress is to be stored. */
        fun onRated(flashcard: FlashcardDTO, rating: FlashcardRating) {}

        /** Tick on the end card. */
        fun onSessionClosed() {}
    }

    private val card: LinearLayout
    private val tvText: TextView
    private val llSummary: LinearLayout
    private val tvSummaryTitle: TextView
    private val tvSummaryResult: TextView
    private val btnLeft: MaterialButton
    private val btnMiddle: MaterialButton
    private val btnRight: MaterialButton
    private val llProgress: LinearLayout
    private val vProgressDone: View
    private val vProgressLeft: View

    private var callback: FlashcardsCallback? = null

    // Head = the flashcard on the card; rated ones leave the queue, skipped ones go to its end
    private var queue: List<FlashcardsReview.Card> = emptyList()
    private var sessionSize = 0
    private var isRevealed = false
    private val ratings = mutableMapOf<FlashcardRating, Int>()

    // One animation at a time - a second tap mid-flip would rate twice
    private var isAnimating = false

    init {
        inflate(context, R.layout.common_flashcard_view, this)
        orientation = VERTICAL

        card = findViewById(R.id.ll_flashcard_card)
        tvText = findViewById(R.id.tv_flashcard_text)
        llSummary = findViewById(R.id.ll_flashcard_summary)
        tvSummaryTitle = findViewById(R.id.tv_flashcard_summary_title)
        tvSummaryResult = findViewById(R.id.tv_flashcard_summary_result)
        btnLeft = findViewById(R.id.btn_flashcard_left)
        btnMiddle = findViewById(R.id.btn_flashcard_middle)
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
            if (phase() != Phase.FLASHCARD) return@setOnClickListener
            if (isRevealed) rate(FlashcardRating.GOOD) else reveal()
        }

        btnMiddle.setOnClickListener {
            if (phase() == Phase.FLASHCARD && isRevealed) rate(FlashcardRating.OK)
        }

        btnRight.setOnClickListener {
            when (phase()) {
                Phase.FLASHCARD -> if (isRevealed) rate(FlashcardRating.BAD) else skip()
                Phase.END       -> callback?.onSessionClosed()
            }
        }
    }

    private fun phase(): Phase =
        if (queue.isNotEmpty()) Phase.FLASHCARD else Phase.END

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
        val current = queue.first()

        tvText.visibility = VISIBLE
        llSummary.visibility = GONE
        tvText.text = if (isRevealed) current.flashcard.back else current.flashcard.front

        if (isRevealed) {
            // Not approve / close icons - in the app those mean "save" and "delete"
            btnLeft.setIconResource(R.drawable.ic_thumb_up)
            btnRight.setIconResource(R.drawable.ic_thumb_down)
            btnMiddle.visibility = VISIBLE
            btnRight.visibility = VISIBLE
        }
        else {
            btnLeft.setIconResource(R.drawable.ic_path_reveal)
            btnRight.setIconResource(R.drawable.ic_replace_or_renew)
            btnMiddle.visibility = INVISIBLE
            // Nothing to skip to when it is the last one in the queue - INVISIBLE keeps the row height
            btnRight.visibility = if (queue.size > 1) VISIBLE else INVISIBLE
        }
        btnLeft.visibility = VISIBLE

        showProgress(done = sessionSize - queue.size, total = sessionSize)
    }

    private fun renderEndCard() {
        tvText.visibility = GONE
        llSummary.visibility = VISIBLE
        tvSummaryTitle.text = context.getString(R.string.flashcards_end_title)

        val bad = ratings[FlashcardRating.BAD] ?: 0
        val result = context.getString(
            R.string.flashcards_end_ratings,
            ratings[FlashcardRating.GOOD] ?: 0,
            ratings[FlashcardRating.OK] ?: 0,
            bad
        )
        tvSummaryResult.text =
            if (bad > 0) result + "\n" + context.getString(R.string.flashcards_end_frozen_info)
            else result

        btnLeft.visibility = INVISIBLE
        btnMiddle.visibility = INVISIBLE
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

    private fun reveal() {
        animateCardChange(flip = true) { isRevealed = true }
    }

    /** Moves the flashcard to the end of the queue - it comes back once the others are done. */
    private fun skip() {
        animateCardChange(flip = false) { queue = queue.drop(1) + queue.first() }
    }

    private fun rate(rating: FlashcardRating) {
        animateCardChange(flip = false) {
            val current = queue.first()
            ratings[rating] = (ratings[rating] ?: 0) + 1
            queue = queue.drop(1)
            isRevealed = false
            callback?.onRated(current.flashcard, rating)
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

    /** New session - the queue as FlashcardsReview planned it */
    fun setQueue(newQueue: List<FlashcardsReview.Card>) {
        resetCardAnimation()
        queue = newQueue
        sessionSize = newQueue.size
        isRevealed = false
        ratings.clear()
        render()
    }

    companion object {
        private const val FLIP_HALF_DURATION_MS = 150L
        private const val FADE_DURATION_MS = 150L
        private const val CAMERA_DISTANCE = 8000f
    }
}
