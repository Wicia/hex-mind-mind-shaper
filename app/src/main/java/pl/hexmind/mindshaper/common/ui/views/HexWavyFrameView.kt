package pl.hexmind.mindshaper.common.ui.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.os.SystemClock
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import pl.hexmind.mindshaper.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Glowing frame of "Do powtórki" whose line ripples like a water surface while [isWaving] ("something to do").
 * Not waving = a still, straight frame; the ripples fade in / out.
 *
 * - line = rounded rect sampled every few dp, each point pushed along its outward normal
 * - ripples = two sines travelling in opposite directions - irregular, never visibly repeating
 * - wavelengths fitted to the perimeter - no seam where the line closes
 * - glow = the same line stroked several times, wider and fainter (flashcard_review_glow_1..5)
 * - animates only while visible and attached; animations off in the system -> still ripples
 * - ! not a blur: BlurMaskFilter re-rasterizes the path every frame - layered strokes are cheap and look the same
 *
 * XML usage: behind the content, match_parent in a FrameLayout - draws the frame itself (no background needed).
 */
class HexWavyFrameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        // Line of the frame from the view's edge - room for the outer glow + the ripples
        private const val LINE_INSET_DP = 10f
        private const val CORNER_RADIUS_DP = 14f

        // Distance between the points of the line - smooth enough, cheap enough
        private const val SAMPLE_STEP_DP = 2f

        // Ripples: height at full waving, wave lengths (fitted to the perimeter), speed along the line
        // ! 2.5dp rose and fell too much - a gentle ripple, not waves
        private const val AMPLITUDE_DP = 1.2f
        private const val WAVELENGTH_1_DP = 64f
        private const val WAVELENGTH_2_DP = 38f
        private const val SPEED_1_DP_PER_S = 22f
        private const val SPEED_2_DP_PER_S = 15f

        private const val FADE_DURATION_MS = 600f
    }

    // Widest + faintest first, the sharp line last
    private val glowLayers: List<Paint> = listOf(
        16f to R.color.flashcard_review_glow_5,
        12f to R.color.flashcard_review_glow_4,
        8f  to R.color.flashcard_review_glow_3,
        4f  to R.color.flashcard_review_glow_2,
        1.5f to R.color.flashcard_review_glow_1
    ).map { (widthDp, colorRes) ->
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeWidth = dp(widthDp)
            color = ContextCompat.getColor(context, colorRes)
        }
    }

    /** Ripples on / off - fade over [FADE_DURATION_MS] */
    var isWaving: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            lastFrameMs = 0L
            scheduleFrame()
        }

    private var intensity = 0f // 0 = straight, 1 = full ripples - follows isWaving
    private var lastFrameMs = 0L

    // Straight line of the frame: points + outward normals, sampled once per size
    private var baseX = FloatArray(0)
    private var baseY = FloatArray(0)
    private var normalX = FloatArray(0)
    private var normalY = FloatArray(0)
    private var arcLength = FloatArray(0)
    private var perimeter = 0f
    private var waveNumber1 = 0f // 2 pi / wavelength, fitted to the perimeter
    private var waveNumber2 = 0f

    private val linePath = Path()

    init {
        // Pure decoration - taps go to the content on top
        isClickable = false
        isFocusable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    /**
     * ! A plain View takes all the AT_MOST space offered - in a wrap_content FrameLayout under a fillViewport
     * scroll that was the whole screen height. Pure decoration has no size of its own: 0, unless told exactly
     * (FrameLayout measures match_parent children again with its final size)
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(resolveSize(0, widthMeasureSpec), resolveSize(0, heightMeasureSpec))
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        sampleFrameLine(w.toFloat(), h.toFloat())
    }

    /** Rounded rect -> points every SAMPLE_STEP_DP with outward normals, clockwise from the top left corner's end */
    private fun sampleFrameLine(width: Float, height: Float) {
        val inset = dp(LINE_INSET_DP)
        val left = inset
        val top = inset
        val right = width - inset
        val bottom = height - inset
        val radius = dp(CORNER_RADIUS_DP).coerceAtMost(minOf(right - left, bottom - top) / 2).coerceAtLeast(0f)
        if (right - left <= 2 * radius || bottom - top <= 2 * radius) {
            perimeter = 0f
            return
        }

        val straightW = right - left - 2 * radius
        val straightH = bottom - top - 2 * radius
        val arc = (PI / 2).toFloat() * radius
        perimeter = 2 * straightW + 2 * straightH + 4 * arc

        val count = (perimeter / dp(SAMPLE_STEP_DP)).roundToInt().coerceAtLeast(16)
        baseX = FloatArray(count)
        baseY = FloatArray(count)
        normalX = FloatArray(count)
        normalY = FloatArray(count)
        arcLength = FloatArray(count)

        for (i in 0 until count) {
            var s = perimeter * i / count
            arcLength[i] = s

            // Walk the segments: top, top-right corner, right, bottom-right, bottom, bottom-left, left, top-left
            val point: FloatArray = when {
                s < straightW -> floatArrayOf(left + radius + s, top, 0f, -1f)
                (s - straightW).also { s = it } < arc -> corner(right - radius, top + radius, radius, -90f + 90f * s / arc)
                (s - arc).also { s = it } < straightH -> floatArrayOf(right, top + radius + s, 1f, 0f)
                (s - straightH).also { s = it } < arc -> corner(right - radius, bottom - radius, radius, 90f * s / arc)
                (s - arc).also { s = it } < straightW -> floatArrayOf(right - radius - s, bottom, 0f, 1f)
                (s - straightW).also { s = it } < arc -> corner(left + radius, bottom - radius, radius, 90f + 90f * s / arc)
                (s - arc).also { s = it } < straightH -> floatArrayOf(left, bottom - radius - s, -1f, 0f)
                else -> corner(left + radius, top + radius, radius, 180f + 90f * (s - straightH) / arc)
            }
            baseX[i] = point[0]
            baseY[i] = point[1]
            normalX[i] = point[2]
            normalY[i] = point[3]
        }

        // Whole number of waves around - the line closes without a kink
        waveNumber1 = 2 * PI.toFloat() * (perimeter / dp(WAVELENGTH_1_DP)).roundToInt().coerceAtLeast(1) / perimeter
        waveNumber2 = 2 * PI.toFloat() * (perimeter / dp(WAVELENGTH_2_DP)).roundToInt().coerceAtLeast(1) / perimeter
    }

    /** Point on a corner arc at the given angle (0 = right, clockwise) + its outward normal */
    private fun corner(centerX: Float, centerY: Float, radius: Float, angleDegrees: Float): FloatArray {
        val angle = Math.toRadians(angleDegrees.toDouble())
        val nx = cos(angle).toFloat()
        val ny = sin(angle).toFloat()
        return floatArrayOf(centerX + radius * nx, centerY + radius * ny, nx, ny)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        updateIntensity()
        if (perimeter <= 0f) return

        // Animations off in the system - still ripples instead of moving ones
        val timeS = if (ValueAnimator.areAnimatorsEnabled()) SystemClock.uptimeMillis() / 1000f else 0f
        val amplitude = dp(AMPLITUDE_DP) * intensity
        val shift1 = waveNumber1 * dp(SPEED_1_DP_PER_S) * timeS
        val shift2 = waveNumber2 * dp(SPEED_2_DP_PER_S) * timeS

        linePath.rewind()
        for (i in baseX.indices) {
            // Opposite directions -> the ripples meet and change shape, like water
            val offset = amplitude * (0.6f * sin(waveNumber1 * arcLength[i] - shift1) + 0.4f * sin(waveNumber2 * arcLength[i] + shift2))
            val x = baseX[i] + normalX[i] * offset
            val y = baseY[i] + normalY[i] * offset
            if (i == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
        }
        linePath.close()

        glowLayers.forEach { paint -> canvas.drawPath(linePath, paint) }

        // ! Next frame also at intensity 0 while starting - the first frame of the fade-in always starts at 0
        // Straight (not waving, faded) or still (animations off) needs no next one
        if (ValueAnimator.areAnimatorsEnabled() && (isWaving || intensity > 0f)) scheduleFrame()
    }

    /** Moves the intensity towards isWaving - time based, so the fade lasts the same on every device */
    private fun updateIntensity() {
        val now = SystemClock.uptimeMillis()
        val elapsed = if (lastFrameMs == 0L) 0L else now - lastFrameMs
        lastFrameMs = now

        val step = if (ValueAnimator.areAnimatorsEnabled()) elapsed / FADE_DURATION_MS else 1f
        intensity = if (isWaving) (intensity + step).coerceAtMost(1f) else (intensity - step).coerceAtLeast(0f)
    }

    private fun scheduleFrame() {
        // Off screen - onVisibilityAggregated starts it again
        if (isAttachedToWindow && isShown) postInvalidateOnAnimation()
    }

    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)
        if (isVisible) {
            lastFrameMs = 0L
            scheduleFrame()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        lastFrameMs = 0L
        scheduleFrame()
    }

    private fun dp(value: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics)
}
