package pl.hexmind.mindshaper.common.ui.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.SystemClock
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import pl.hexmind.mindshaper.R
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Confetti shot from one or more points in turn, every few seconds - e.g. out of the top corners of a done task's dialog.
 * Pieces fly out in a fan, fall under gravity, spin and fade out; only the view's own area is drawn.
 *
 * Runs only while attached; with animations turned off in the system nothing is shot.
 *
 * XML: the last child of a FrameLayout, match_parent over the content - not clickable, taps go through.
 * Usage: confettiView.start(Origin(0f, 0f, 35.0), Origin(1f, 0f, 145.0))  // points within this view, 0..1
 */
class HexConfettiView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    companion object {
        private const val BURST_INTERVAL_MS = 1500L
        private const val FIRST_BURST_DELAY_MS = 300L
        private const val PIECES_PER_BURST = 16
        private const val PIECE_LIFE_MS = 1400L
        private const val PIECE_FADE_PART = 0.4f          // last part of the life fades out

        private const val FAN_SPREAD_DEG = 40.0

        private const val SPEED_MIN_DP = 220f            // per second
        private const val SPEED_MAX_DP = 400f
        private const val GRAVITY_DP = 320f              // per second squared
        private const val SPIN_MAX_DEG = 540f            // per second, either way

        private const val PIECE_LONG_DP = 6f
        private const val PIECE_SHORT_DP = 3f
    }

    /**
     * Point the pieces are shot from
     * @param xFraction, yFraction position within this view, 0..1
     * @param fanCenterDeg direction of the fan - 0 = right, -90 = up
     */
    class Origin(val xFraction: Float, val yFraction: Float, val fanCenterDeg: Double)

    private class Piece(
        val origin: Origin,
        val startMs: Long,
        val velocityX: Float,
        val velocityY: Float,
        val spin: Float,
        val color: Int,
        val isRound: Boolean
    )

    private val pieces = mutableListOf<Piece>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    private val colors = listOf(
        R.color._palette_lvl_2, R.color._palette_lvl_3, R.color.spotlight_arrow, R.color.progress_ring_done
    ).map { colorRes -> ContextCompat.getColor(context, colorRes) }

    private var origins: List<Origin> = emptyList()
    private var nextOriginIndex = 0
    private var isRunning = false

    // Origins take turns, evenly spread - each one still shoots every BURST_INTERVAL_MS, never two at once
    private val burstRunnable = object : Runnable {
        override fun run() {
            shoot(origins[nextOriginIndex])
            nextOriginIndex = (nextOriginIndex + 1) % origins.size
            postDelayed(this, BURST_INTERVAL_MS / origins.size)
        }
    }

    fun start(vararg origins: Origin) {
        if (origins.isEmpty()) return
        this.origins = origins.toList()
        nextOriginIndex = 0
        isRunning = true
        if (isAttachedToWindow) scheduleBursts()
    }

    fun stop() {
        isRunning = false
        removeCallbacks(burstRunnable)
        pieces.clear()
        invalidate()
    }

    // ! No size of its own - a View would take all the room offered and stretch a wrap_content dialog to the full screen;
    // the FrameLayout measures it again with the content's exact size
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(resolveSize(0, widthMeasureSpec), resolveSize(0, heightMeasureSpec))
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (isRunning) scheduleBursts()
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(burstRunnable)
        pieces.clear()
        super.onDetachedFromWindow()
    }

    private fun scheduleBursts() {
        // ! Animations off in the system = no confetti at all, the icon alone says it
        if (!ValueAnimator.areAnimatorsEnabled()) return

        removeCallbacks(burstRunnable)
        postDelayed(burstRunnable, FIRST_BURST_DELAY_MS)
    }

    private fun shoot(origin: Origin) {
        val now = SystemClock.uptimeMillis()
        pieces.removeAll { piece -> now - piece.startMs > PIECE_LIFE_MS }

        repeat(PIECES_PER_BURST) {
            val angle = Math.toRadians(origin.fanCenterDeg + (Random.nextDouble() - 0.5) * FAN_SPREAD_DEG)
            val speed = dp(SPEED_MIN_DP + Random.nextFloat() * (SPEED_MAX_DP - SPEED_MIN_DP))
            pieces.add(
                Piece(
                    origin    = origin,
                    startMs   = now,
                    velocityX = (cos(angle) * speed).toFloat(),
                    velocityY = (sin(angle) * speed).toFloat(),
                    spin      = (Random.nextFloat() * 2f - 1f) * SPIN_MAX_DEG,
                    color     = colors.random(),
                    isRound   = Random.nextInt(3) == 0
                )
            )
        }
        postInvalidateOnAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (pieces.isEmpty()) return

        val now = SystemClock.uptimeMillis()
        val gravity = dp(GRAVITY_DP)
        val halfLong = dp(PIECE_LONG_DP) / 2f
        val halfShort = dp(PIECE_SHORT_DP) / 2f

        var anyAlive = false
        pieces.forEach { piece ->
            val ageMs = now - piece.startMs
            if (ageMs > PIECE_LIFE_MS) return@forEach
            anyAlive = true

            val t = ageMs / 1000f
            val x = width * piece.origin.xFraction + piece.velocityX * t
            val y = height * piece.origin.yFraction + piece.velocityY * t + gravity * t * t / 2f

            val lifePart = ageMs.toFloat() / PIECE_LIFE_MS
            val fadeStart = 1f - PIECE_FADE_PART
            val alpha = if (lifePart < fadeStart) 1f else 1f - (lifePart - fadeStart) / PIECE_FADE_PART

            paint.color = piece.color
            paint.alpha = (alpha * 255).toInt()

            canvas.save()
            canvas.translate(x, y)
            canvas.rotate(piece.spin * t)
            if (piece.isRound) canvas.drawCircle(0f, 0f, halfShort, paint)
            else canvas.drawRect(-halfLong, -halfShort, halfLong, halfShort, paint)
            canvas.restore()
        }

        if (anyAlive) postInvalidateOnAnimation() else pieces.clear()
    }

    private fun dp(value: Float): Float = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics
    )
}
