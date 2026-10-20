package pl.hexmind.mindshaper.common.ui.views.content

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.services.FlashcardsReview
import pl.hexmind.mindshaper.services.dto.FlashcardDTO
import pl.hexmind.mindshaper.services.dto.FlashcardRating
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Landscape review session (FlashcardsSessionActivity): the deck in the middle, the rating piles as pieces of the screen edges
 * (left = BAD, top = OK, right = GOOD).
 *
 * - front:     tap = flip / "Później" = to the end of the queue; no drag - nothing to rate yet
 * - back:      the card thrown towards a pile (the direction counts, the pile needs no hitting) or a tap on the pile
 * - end card:  the session's first ratings, "Gotowe" = close
 *
 * Queue rules as in HexFlashcardView: BAD sends the flashcard to the end of the queue, it comes back until rated GOOD / OK.
 * Only the first rating of a flashcard goes out through the callback (right away, to be stored).
 * Undo: the last rating can be taken back for a moment - the flashcard comes back revealed, a first rating is undone in the callback.
 */
class HexFlashcardsSessionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private enum class Phase {
        FLASHCARD,
        END
    }

    interface SessionCallback {
        /** Flashcard rated for the first time in the session - its progress is to be stored. */
        fun onRated(flashcard: FlashcardDTO, rating: FlashcardRating) {}

        /** First rating taken back - [flashcard] holds the progress from before that rating. */
        fun onRatingUndone(flashcard: FlashcardDTO) {}

        /** "Przerwij" or "Gotowe" on the end card. */
        fun onSessionClosed() {}
    }

    /** Idle = dashed frame on a faint fill of the pile's color, lit = filled with it while the card is held over */
    private class Pile(
        val zone: LinearLayout,
        val face: ImageView,
        val label: TextView,
        val count: TextView,
        @ColorRes val frameColor: Int,
        @ColorRes val fillColor: Int,
        @ColorRes val faceColor: Int,
        @ColorRes val litColor: Int,
        @ColorRes val onLitColor: Int,
        @ColorRes val stampColor: Int,
        @StringRes val captionRes: Int
    )

    /** Last rating - can be taken back until the undo window runs out or the queue moves by a skip */
    private class Move(
        val card: FlashcardsReview.Card,
        val rating: FlashcardRating,
        val isFirstRating: Boolean,
        val queueBefore: List<FlashcardsReview.Card>
    )

    private val card: LinearLayout
    private val tvSide: TextView
    private val tvStamp: TextView
    private val tvQuestion: TextView
    private val tvText: TextView
    private val tvFooter: TextView
    private val vGrip: View
    private val llCenter: LinearLayout
    private val flDeck: FrameLayout
    private val flDeckNext: FrameLayout
    private val llLater: LinearLayout
    private val tvHint: TextView
    private val llDone: LinearLayout
    private val llPause: LinearLayout
    private val llUndo: LinearLayout
    private val tvDoneCount: TextView
    private val tvTotal: TextView
    private val llProgress: LinearLayout
    private val vProgressDone: View
    private val vProgressLeft: View

    // Top one first - a shorter queue shows fewer of them
    private val deckLayers: List<View>
    private val piles: Map<FlashcardRating, Pile>

    private var callback: SessionCallback? = null

    private val zoneDepth = resources.getDimension(R.dimen.flashcard_zone_depth)
    private val zoneRadius = resources.getDimension(R.dimen.flashcard_zone_radius)
    private val zoneFrameWidth = resources.getDimensionPixelSize(R.dimen.flashcard_zone_frame_width)
    private val zoneDash = resources.getDimension(R.dimen.flashcard_zone_dash)
    private val zoneDashGap = resources.getDimension(R.dimen.flashcard_zone_dash_gap)
    private val throwSide = resources.getDimension(R.dimen.flashcard_session_throw_side)
    private val throwUp = resources.getDimension(R.dimen.flashcard_session_throw_up)
    private val stampRadius = resources.getDimension(R.dimen.flashcard_session_stamp_radius)
    private val stampFrameWidth = resources.getDimensionPixelSize(R.dimen.flashcard_session_stamp_frame_width)
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val density = resources.displayMetrics.density

    // Head = the flashcard on the card; GOOD / OK take it off the queue, skip and BAD send it to the end
    private var queue: List<FlashcardsReview.Card> = emptyList()
    private var sessionSize = 0
    private var isRevealed = false
    // First ratings only - a return after BAD counts neither here nor in the progress of the flashcard
    private val ratings = mutableMapOf<FlashcardRating, Int>()
    private val ratedCards = mutableSetOf<FlashcardsReview.Card>()

    // One animation at a time - a second tap mid-flip / mid-throw would rate twice
    private var isAnimating = false

    // Drag of the back
    private var isDragging = false
    private var dragStartX = 0f
    private var dragStartY = 0f
    private var hotPile: FlashcardRating? = null

    private var lastMove: Move? = null
    private val hideUndo = Runnable { clearUndo() }

    init {
        inflate(context, R.layout.common_flashcards_session_view, this)
        setBackgroundColor(color(R.color.flashcard_session_background))

        card = findViewById(R.id.ll_session_card)
        tvSide = findViewById(R.id.tv_card_side)
        tvStamp = findViewById(R.id.tv_card_stamp)
        tvQuestion = findViewById(R.id.tv_card_question)
        tvText = findViewById(R.id.tv_card_text)
        tvFooter = findViewById(R.id.tv_card_footer)
        vGrip = findViewById(R.id.v_card_grip)
        llCenter = findViewById(R.id.ll_session_center)
        flDeck = findViewById(R.id.fl_session_deck)
        flDeckNext = findViewById(R.id.fl_deck_next)
        llLater = findViewById(R.id.ll_session_later)
        tvHint = findViewById(R.id.tv_session_hint)
        llDone = findViewById(R.id.ll_session_done)
        llPause = findViewById(R.id.ll_session_pause)
        llUndo = findViewById(R.id.ll_session_undo)
        tvDoneCount = findViewById(R.id.tv_session_done_count)
        tvTotal = findViewById(R.id.tv_session_total)
        llProgress = findViewById(R.id.ll_session_progress)
        vProgressDone = findViewById(R.id.v_session_progress_done)
        vProgressLeft = findViewById(R.id.v_session_progress_left)

        deckLayers = listOf(
            findViewById(R.id.v_deck_layer_1),
            findViewById(R.id.v_deck_layer_2),
            findViewById(R.id.v_deck_layer_3),
            findViewById(R.id.v_deck_layer_4)
        )

        piles = mapOf(
            FlashcardRating.GOOD to Pile(
                zone = findViewById(R.id.ll_zone_good),
                face = findViewById(R.id.iv_zone_good_face),
                label = findViewById(R.id.tv_zone_good_label),
                count = findViewById(R.id.tv_zone_good_count),
                frameColor = R.color.flashcard_zone_good,
                fillColor = R.color.flashcard_zone_good_fill,
                faceColor = R.color.flashcard_zone_good,
                litColor = R.color.flashcard_zone_good,
                onLitColor = R.color.graphite_dark,
                stampColor = R.color.flashcard_stamp_good,
                captionRes = R.string.flashcards_rating_good
            ),
            FlashcardRating.OK to Pile(
                zone = findViewById(R.id.ll_zone_ok),
                face = findViewById(R.id.iv_zone_ok_face),
                label = findViewById(R.id.tv_zone_ok_label),
                count = findViewById(R.id.tv_zone_ok_count),
                frameColor = R.color.flashcard_zone_ok_frame,
                fillColor = R.color.flashcard_zone_ok_fill,
                faceColor = R.color.flashcard_zone_ok_face,
                litColor = R.color.flashcard_zone_ok_face,
                onLitColor = R.color.text_primary,
                stampColor = R.color.flashcard_stamp_ok,
                captionRes = R.string.flashcards_rating_ok
            ),
            FlashcardRating.BAD to Pile(
                zone = findViewById(R.id.ll_zone_bad),
                face = findViewById(R.id.iv_zone_bad_face),
                label = findViewById(R.id.tv_zone_bad_label),
                count = findViewById(R.id.tv_zone_bad_count),
                frameColor = R.color.flashcard_zone_bad_frame,
                fillColor = R.color.flashcard_zone_bad_fill,
                faceColor = R.color.flashcard_zone_bad_face,
                litColor = R.color.flashcard_zone_bad_frame,
                onLitColor = R.color.flashcard_session_text,
                stampColor = R.color.flashcard_stamp_bad,
                captionRes = R.string.flashcards_rating_bad
            )
        )

        // Without it the Y rotation looks like the card is bursting out of the screen
        card.cameraDistance = CAMERA_DISTANCE * density

        setupListeners()
        setupCardDrag()
        render()
    }

    private fun setupListeners() {
        card.setOnClickListener {
            if (phase() == Phase.FLASHCARD && !isRevealed && !isAnimating) reveal()
        }

        piles.forEach { (rating, pile) ->
            pile.zone.setOnClickListener {
                if (canRate()) throwTo(rating)
            }
        }

        llLater.setOnClickListener {
            if (phase() == Phase.FLASHCARD && !isRevealed && !isAnimating && queue.size > 1) skip()
        }

        llUndo.setOnClickListener { undo() }
        llPause.setOnClickListener { callback?.onSessionClosed() }
        llDone.setOnClickListener { callback?.onSessionClosed() }
    }

    /**
     * Only the back is dragged - on the front the touch falls through to the click (= flip).
     * ! A tap on the back does nothing: the rating is a throw or a tap on a pile
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun setupCardDrag() {
        card.setOnTouchListener { _, event ->
            if (!isDragging && !canRate()) return@setOnTouchListener false

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    dragStartX = event.rawX
                    dragStartY = event.rawY
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - dragStartX
                    val dy = event.rawY - dragStartY
                    if (!isDragging && hypot(dx, dy) > touchSlop) {
                        isDragging = true
                        parent.requestDisallowInterceptTouchEvent(true)
                    }
                    if (isDragging) {
                        card.translationX = dx
                        card.translationY = dy
                        card.rotation = (dx / density * DRAG_ROTATION_PER_DP).coerceIn(-MAX_DRAG_ROTATION, MAX_DRAG_ROTATION)
                        setHot(pileFor(dx, dy))
                    }
                }
                MotionEvent.ACTION_UP -> {
                    if (isDragging) {
                        isDragging = false
                        hotPile?.let { rating -> throwTo(rating) } ?: springBack()
                    }
                }
                MotionEvent.ACTION_CANCEL -> {
                    if (isDragging) {
                        isDragging = false
                        springBack()
                    }
                }
            }
            true
        }
    }

    private fun phase(): Phase =
        if (queue.isNotEmpty()) Phase.FLASHCARD else Phase.END

    private fun canRate(): Boolean =
        phase() == Phase.FLASHCARD && isRevealed && !isAnimating

    /** Direction of the drag -> pile; up needs to be mostly up, sideways only far enough */
    private fun pileFor(dx: Float, dy: Float): FlashcardRating? =
        when {
            dy < -throwUp && -dy > abs(dx) * UP_DIRECTION_RATIO -> FlashcardRating.OK
            dx > throwSide                                       -> FlashcardRating.GOOD
            dx < -throwSide                                      -> FlashcardRating.BAD
            else                                                 -> null
        }

    // ===========================================
    //      Rendering
    // ===========================================

    private fun render() {
        when (phase()) {
            Phase.FLASHCARD -> renderFlashcard()
            Phase.END       -> renderEndCard()
        }
        renderPiles()
        renderStamp()
        renderSlot()
        renderDeck()
        renderCounter()
    }

    private fun renderFlashcard() {
        val current = queue.first().flashcard

        tvSide.visibility = VISIBLE
        tvSide.setText(if (isRevealed) R.string.flashcards_session_back else R.string.flashcards_session_front)
        tvQuestion.visibility = if (isRevealed) VISIBLE else GONE
        tvQuestion.text = current.front
        tvText.text = if (isRevealed) current.back else current.front
        tvFooter.visibility = if (isRevealed) GONE else VISIBLE
        tvFooter.setText(R.string.flashcards_session_tap_to_flip)
        vGrip.visibility = if (isRevealed) VISIBLE else GONE
    }

    private fun renderEndCard() {
        tvSide.visibility = INVISIBLE
        tvQuestion.visibility = GONE
        tvText.setText(R.string.flashcards_end_title)
        tvFooter.visibility = VISIBLE
        tvFooter.text = context.getString(
            R.string.flashcards_end_ratings,
            ratings[FlashcardRating.GOOD] ?: 0,
            ratings[FlashcardRating.OK] ?: 0,
            ratings[FlashcardRating.BAD] ?: 0
        )
        vGrip.visibility = GONE
    }

    /** Piles are active on the back only; while the card is held over one, it is lit and the others step back */
    private fun renderPiles() {
        val isActive = phase() == Phase.FLASHCARD && isRevealed
        // A return after BAD is not counted again
        val countsNow = isActive && !ratedCards.contains(queue.first())

        piles.forEach { (rating, pile) ->
            val isLit = isActive && hotPile == rating
            val contentColor = color(if (isLit) pile.onLitColor else R.color.flashcard_session_text)
            val count = ratings[rating] ?: 0

            pile.zone.background = zoneBackground(pile, isLit)
            pile.face.imageTintList = ColorStateList.valueOf(if (isLit) contentColor else color(pile.faceColor))
            pile.label.setTextColor(contentColor)
            pile.count.setTextColor(contentColor)
            pile.count.text =
                if (isLit && countsNow) context.getString(R.string.flashcards_session_count_next, count, count + 1)
                else count.toString()
            pile.zone.alpha = when {
                !isActive                  -> ALPHA_INACTIVE
                hotPile != null && !isLit  -> ALPHA_DIMMED
                else                       -> 1f
            }
        }
    }

    private fun zoneBackground(pile: Pile, isLit: Boolean): GradientDrawable =
        GradientDrawable().apply {
            cornerRadius = zoneRadius
            if (isLit) {
                setColor(color(pile.litColor))
                setStroke(zoneFrameWidth, color(pile.litColor))
            }
            else {
                setColor(color(pile.fillColor))
                setStroke(zoneFrameWidth, color(pile.frameColor), zoneDash, zoneDashGap)
            }
        }

    private fun renderStamp() {
        val pile = hotPile?.takeIf { isRevealed }?.let { rating -> piles.getValue(rating) }
        if (pile == null) {
            tvStamp.visibility = GONE
            return
        }
        val stampColor = color(pile.stampColor)
        tvStamp.visibility = VISIBLE
        tvStamp.setText(pile.captionRes)
        tvStamp.setTextColor(stampColor)
        tvStamp.background = GradientDrawable().apply {
            cornerRadius = stampRadius
            setStroke(stampFrameWidth, stampColor)
        }
    }

    private fun renderSlot() {
        val isFlashcard = phase() == Phase.FLASHCARD
        // Nothing to skip to when it is the last one in the queue
        llLater.visibility = if (isFlashcard && !isRevealed && queue.size > 1) VISIBLE else GONE
        llDone.visibility = if (isFlashcard) GONE else VISIBLE

        tvHint.visibility = if (isFlashcard && isRevealed) VISIBLE else GONE
        val hot = hotPile
        if (hot != null) {
            tvHint.text = context.getString(R.string.flashcards_session_hint_release, context.getString(piles.getValue(hot).captionRes))
            tvHint.setTextColor(color(R.color.flashcard_session_hint_active))
        }
        else {
            tvHint.setText(R.string.flashcards_session_hint_throw)
            tvHint.setTextColor(color(R.color.flashcard_session_text_secondary))
        }
    }

    /** Flashcards left under the current one -> layers of the deck, the deck gets thinner towards the end */
    private fun renderDeck() {
        val underneath = if (phase() == Phase.FLASHCARD) (queue.size - 1).coerceIn(0, deckLayers.size) else 0
        deckLayers.forEachIndexed { index, layer ->
            layer.visibility = if (index < underneath) VISIBLE else GONE
        }
        flDeckNext.visibility = if (underneath > 0) VISIBLE else GONE
    }

    private fun renderCounter() {
        val done = sessionSize - queue.size
        tvDoneCount.text = done.toString()
        tvTotal.text = context.getString(R.string.flashcards_session_total, sessionSize)

        val doneRatio = if (sessionSize == 0) 0f else done.toFloat() / sessionSize
        (vProgressDone.layoutParams as LinearLayout.LayoutParams).weight = doneRatio
        (vProgressLeft.layoutParams as LinearLayout.LayoutParams).weight = 1f - doneRatio
        llProgress.requestLayout()
    }

    private fun setHot(rating: FlashcardRating?) {
        if (rating == hotPile) return
        hotPile = rating
        if (rating != null) card.performHapticFeedback(HapticFeedbackConstants.SEGMENT_TICK)
        renderPiles()
        renderStamp()
        renderSlot()
    }

    // ===========================================
    //      Session actions
    // ===========================================

    private fun reveal() {
        animateCardChange(flip = true) { isRevealed = true }
    }

    /** Moves the flashcard to the end of the queue - it comes back once the others are done. */
    private fun skip() {
        // The queue moves - the last rating can no longer be put back in its place
        clearUndo()
        animateCardChange(flip = false) { queue = queue.drop(1) + queue.first() }
    }

    private fun rate(rating: FlashcardRating) {
        val current = queue.first()
        val isFirstRating = ratedCards.add(current)
        if (isFirstRating) {
            ratings[rating] = (ratings[rating] ?: 0) + 1
            callback?.onRated(current.flashcard, rating)
        }
        lastMove = Move(current, rating, isFirstRating, queueBefore = queue)
        queue = if (rating == FlashcardRating.BAD) queue.drop(1) + current else queue.drop(1)
        isRevealed = false
        showUndo()
    }

    /** The last rating taken back - its flashcard returns to the head of the queue, revealed, to be rated again */
    private fun undo() {
        val move = lastMove ?: return
        if (isAnimating) return
        clearUndo()
        animateCardChange(flip = false) {
            if (move.isFirstRating) {
                ratedCards.remove(move.card)
                ratings[move.rating] = ((ratings[move.rating] ?: 1) - 1).coerceAtLeast(0)
                callback?.onRatingUndone(move.card.flashcard)
            }
            queue = move.queueBefore
            isRevealed = true
        }
    }

    private fun showUndo() {
        llUndo.visibility = VISIBLE
        removeCallbacks(hideUndo)
        postDelayed(hideUndo, UNDO_WINDOW_MS)
    }

    private fun clearUndo() {
        lastMove = null
        removeCallbacks(hideUndo)
        llUndo.visibility = GONE
    }

    // ===========================================
    //      Animations
    // ===========================================

    /** The card flies onto the pile and shrinks; the next one comes up from the deck */
    private fun throwTo(rating: FlashcardRating) {
        if (isAnimating) return
        isAnimating = true
        // Lights the pile also when it was tapped instead of thrown at
        setHot(rating)
        card.performHapticFeedback(HapticFeedbackConstants.CONFIRM)

        val (targetX, targetY) = throwTarget(rating)
        val targetRotation = when (rating) {
            FlashcardRating.GOOD -> MAX_DRAG_ROTATION
            FlashcardRating.BAD  -> -MAX_DRAG_ROTATION
            FlashcardRating.OK   -> card.rotation
        }

        card.animate()
            .translationX(targetX)
            .translationY(targetY)
            .rotation(targetRotation)
            .scaleX(THROWN_SCALE)
            .scaleY(THROWN_SCALE)
            .alpha(0f)
            .setDuration(THROW_DURATION_MS)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction {
                rate(rating)
                hotPile = null
                resetCardTransform()
                render()
                pulse(piles.getValue(rating).count)

                card.alpha = 0f
                card.scaleX = ENTER_SCALE
                card.scaleY = ENTER_SCALE
                card.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(ENTER_DURATION_MS)
                    .setInterpolator(DecelerateInterpolator())
                    .withEndAction { isAnimating = false }
                    .start()
            }
            .start()
    }

    /** From the card's resting place to the middle of the pile's visible part */
    private fun throwTarget(rating: FlashcardRating): Pair<Float, Float> {
        val cardCenterX = llCenter.left + flDeck.left + flDeck.width / 2f
        val cardCenterY = llCenter.top + flDeck.top + flDeck.height / 2f
        return when (rating) {
            FlashcardRating.GOOD -> Pair(width - zoneDepth / 2f - cardCenterX, 0f)
            FlashcardRating.BAD  -> Pair(zoneDepth / 2f - cardCenterX, 0f)
            FlashcardRating.OK   -> Pair(0f, zoneDepth / 2f - cardCenterY)
        }
    }

    /** Released too close - back to the deck with a little overshoot */
    private fun springBack() {
        setHot(null)
        card.animate()
            .translationX(0f)
            .translationY(0f)
            .rotation(0f)
            .setDuration(SPRING_DURATION_MS)
            .setInterpolator(OvershootInterpolator(SPRING_TENSION))
            .start()
    }

    private fun pulse(view: View) {
        view.animate()
            .scaleX(PULSE_SCALE)
            .scaleY(PULSE_SCALE)
            .setDuration(PULSE_DURATION_MS)
            .withEndAction {
                view.animate().scaleX(1f).scaleY(1f).setDuration(PULSE_DURATION_MS).start()
            }
            .start()
    }

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

    private fun resetCardTransform() {
        card.animate().cancel()
        card.translationX = 0f
        card.translationY = 0f
        card.rotation = 0f
        card.rotationY = 0f
        card.scaleX = 1f
        card.scaleY = 1f
        card.alpha = 1f
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // A cancelled animation never runs its end action - without the reset the card stays half-turned
        resetCardTransform()
        isAnimating = false
        isDragging = false
        removeCallbacks(hideUndo)
    }

    private fun color(@ColorRes colorRes: Int): Int =
        ContextCompat.getColor(context, colorRes)

    // ===========================================
    //      Public API Methods
    // ===========================================

    fun setCallback(callback: SessionCallback) {
        this.callback = callback
    }

    /** Set already - the session goes on (the plan is not applied again) */
    val hasQueue: Boolean
        get() = sessionSize > 0

    /** New session - the queue as FlashcardsReview planned it */
    fun setQueue(newQueue: List<FlashcardsReview.Card>) {
        resetCardTransform()
        isAnimating = false
        queue = newQueue
        sessionSize = newQueue.size
        isRevealed = false
        hotPile = null
        ratings.clear()
        ratedCards.clear()
        clearUndo()
        render()
    }

    companion object {
        private const val FLIP_HALF_DURATION_MS = 150L
        private const val FADE_DURATION_MS = 150L
        private const val THROW_DURATION_MS = 260L
        private const val ENTER_DURATION_MS = 200L
        private const val SPRING_DURATION_MS = 280L
        private const val PULSE_DURATION_MS = 120L
        private const val UNDO_WINDOW_MS = 4000L
        private const val CAMERA_DISTANCE = 8000f

        private const val DRAG_ROTATION_PER_DP = 0.06f
        private const val MAX_DRAG_ROTATION = 14f
        // Up = OK only when the drag is mostly upwards - a diagonal throw goes to the side pile
        private const val UP_DIRECTION_RATIO = 0.7f
        private const val SPRING_TENSION = 1.4f
        private const val THROWN_SCALE = 0.35f
        private const val ENTER_SCALE = 0.9f
        private const val PULSE_SCALE = 1.3f
        private const val ALPHA_INACTIVE = 0.35f
        private const val ALPHA_DIMMED = 0.4f
    }
}
