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
 * - back:        good (smile) / ok (thinking) / bad (blank face) - each goes to the next flashcard
 * - end card:    the session's first ratings, tick = close the scrim
 *
 * BAD sends the flashcard to the end of the queue - it comes back until rated GOOD / OK.
 * Only the first rating of a flashcard goes out through the callback (right away, to be stored) - the returns
 * change neither its level nor its date. The queue itself is not stored, the next session builds it again
 * from the flashcards' progress (see FlashcardsReview).
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
        /** Flashcard rated for the first time in the session - its progress is to be stored. */
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
    private val tvLeftCaption: TextView
    private val tvMiddleCaption: TextView
    private val tvRightCaption: TextView
    private val llProgress: LinearLayout
    private val vProgressDone: View
    private val vProgressLeft: View

    private var callback: FlashcardsCallback? = null

    private val actionIconSize = resources.getDimensionPixelSize(R.dimen.flashcard_action_icon_size)
    private val ratingIconSize = resources.getDimensionPixelSize(R.dimen.flashcard_rating_icon_size)

    // Head = the flashcard on the card; GOOD / OK take it off the queue, skip and BAD send it to the end
    private var queue: List<FlashcardsReview.Card> = emptyList()
    private var sessionSize = 0
    private var isRevealed = false
    // First ratings only - a return after BAD counts neither here nor in the progress of the flashcard
    private val ratings = mutableMapOf<FlashcardRating, Int>()
    private val ratedCards = mutableSetOf<FlashcardsReview.Card>()

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
        tvLeftCaption = findViewById(R.id.tv_flashcard_left_caption)
        tvMiddleCaption = findViewById(R.id.tv_flashcard_middle_caption)
        tvRightCaption = findViewById(R.id.tv_flashcard_right_caption)
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
            btnLeft.setIconResource(R.drawable.ic_memory_have)
            btnRight.setIconResource(R.drawable.ic_memory_empty)
            btnLeft.iconSize = ratingIconSize
            btnRight.iconSize = ratingIconSize
            btnMiddle.visibility = VISIBLE
            btnRight.visibility = VISIBLE
            tvLeftCaption.setText(R.string.flashcards_rating_good)
            tvMiddleCaption.setText(R.string.flashcards_rating_ok)
            tvRightCaption.setText(R.string.flashcards_rating_bad)
        }
        else {
            btnLeft.setIconResource(R.drawable.ic_path_reveal)
            btnRight.setIconResource(R.drawable.ic_replace_or_renew)
            btnLeft.iconSize = actionIconSize
            btnRight.iconSize = actionIconSize
            btnMiddle.visibility = INVISIBLE
            // Nothing to skip to when it is the last one in the queue - INVISIBLE keeps the row height
            btnRight.visibility = if (queue.size > 1) VISIBLE else INVISIBLE
        }
        btnLeft.visibility = VISIBLE
        showCaptions(isRevealed)

        showProgress(done = sessionSize - queue.size, total = sessionSize)
    }

    private fun renderEndCard() {
        tvText.visibility = GONE
        llSummary.visibility = VISIBLE
        tvSummaryTitle.text = context.getString(R.string.flashcards_end_title)

        tvSummaryResult.text = context.getString(
            R.string.flashcards_end_ratings,
            ratings[FlashcardRating.GOOD] ?: 0,
            ratings[FlashcardRating.OK] ?: 0,
            ratings[FlashcardRating.BAD] ?: 0
        )

        btnLeft.visibility = INVISIBLE
        btnMiddle.visibility = INVISIBLE
        btnRight.setIconResource(R.drawable.ic_action_approve)
        btnRight.iconSize = actionIconSize
        btnRight.visibility = VISIBLE
        showCaptions(false)

        showProgress(done = 1, total = 1)
    }

    private fun showCaptions(visible: Boolean) {
        val visibility = if (visible) VISIBLE else INVISIBLE
        tvLeftCaption.visibility = visibility
        tvMiddleCaption.visibility = visibility
        tvRightCaption.visibility = visibility
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
            if (ratedCards.add(current)) {
                ratings[rating] = (ratings[rating] ?: 0) + 1
                callback?.onRated(current.flashcard, rating)
            }
            queue = if (rating == FlashcardRating.BAD) queue.drop(1) + current else queue.drop(1)
            isRevealed = false
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
        ratedCards.clear()
        render()
    }

    companion object {
        private const val FLIP_HALF_DURATION_MS = 150L
        private const val FADE_DURATION_MS = 150L
        private const val CAMERA_DISTANCE = 8000f
    }
}
