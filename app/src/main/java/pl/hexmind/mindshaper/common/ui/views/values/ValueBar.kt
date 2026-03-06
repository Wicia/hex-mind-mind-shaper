package pl.hexmind.mindshaper.common.ui.views.values

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import pl.hexmind.mindshaper.R
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * UI controller class for displaying thought value circular bar with dots
 */
class ValueBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        const val MAX_FILLED_DOTS = 10
        const val MIN_FILLED_DOTS = 0
    }

    var radius: Float = 200f
        set(value) {
            field = value
            invalidate()
        }

    var currentLevel: Int = MIN_FILLED_DOTS
        set(value) {
            val newValue = value.coerceIn(MIN_FILLED_DOTS, maxLevel)
            if (newValue > field) {
                animatedDotIndex = newValue - 1
                startDotAnimation()
            }
            field = newValue
            invalidate()
            onLevelChangeListener?.invoke(field)
        }

    var maxLevel: Int = MAX_FILLED_DOTS
        set(value) {
            field = value.coerceAtLeast(MIN_FILLED_DOTS)
            invalidate()
        }

    var minDotRadius: Float = 8f
        set(value) {
            field = value
            invalidate()
        }

    var maxDotRadius: Float = 16f
        set(value) {
            field = value
            invalidate()
        }

    var dotBackgroundColor: Int = R.color._orange_lvl_2
        set(value) {
            field = value
            backgroundPaint.color = value
            invalidate()
        }

    var dotProgressColor: Int = R.color._black
        set(value) {
            field = value
            progressPaint.color = value
            invalidate()
        }

    var dotTextColor: Int = R.color.graphite_light
        set(value) {
            field = value
            textPaint.color = value
            invalidate()
        }

    var showLevelText: Boolean = true
        set(value) {
            field = value
            invalidate()
        }

    // Callback
    var onLevelChangeListener: ((Int) -> Unit)? = null

    // Paints
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = dotBackgroundColor
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = dotProgressColor
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = dotTextColor
        textSize = 32f
        textAlign = Paint.Align.CENTER
    }

    // ✨ Glow / star flash paint
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        color = ContextCompat.getColor(context, R.color._orange_lvl_3)
        alpha = 0
    }

    private var centerX = 0f
    private var centerY = 0f

    // Animation state
    private var animatedDotIndex: Int = -1
    private var animationProgress: Float = 1f

    init {
        // Loading XML attributes
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.ValueBar, 0, 0)
            try {
                radius = typedArray.getDimension(R.styleable.ValueBar_radius, 200f)
                currentLevel = typedArray.getInteger(R.styleable.ValueBar_level, MIN_FILLED_DOTS)
                maxLevel = typedArray.getInteger(R.styleable.ValueBar_maxLevel, MAX_FILLED_DOTS)
                minDotRadius = typedArray.getDimension(R.styleable.ValueBar_minDotRadius, 8f)
                maxDotRadius = typedArray.getDimension(R.styleable.ValueBar_maxDotRadius, 16f)

                dotBackgroundColor = typedArray.getColor(
                    R.styleable.ValueBar_dotBackgroundColor,
                    "#E0E0E0".toColorInt()
                )
                dotProgressColor = typedArray.getColor(
                    R.styleable.ValueBar_dotProgressColor,
                    "#00BCD4".toColorInt()
                )
                dotTextColor = typedArray.getColor(
                    R.styleable.ValueBar_dotTextColor,
                    "#607D8B".toColorInt()
                )

                showLevelText =
                    typedArray.getBoolean(R.styleable.ValueBar_showLevelText, true)
            }
            finally {
                typedArray.recycle()
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerX = w / 2f
        centerY = h / 2f

        val effectiveRadius = min(centerX, centerY) - maxDotRadius - 5f
        if (radius > effectiveRadius) {
            radius = effectiveRadius.coerceAtLeast(10f) // Minimum 10f
        }

        textPaint.textSize = (min(w, h) * 0.3f).coerceIn(20f, 72f)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredSize = ((radius + maxDotRadius + 40f) * 2).toInt()
        setMeasuredDimension(
            resolveSize(desiredSize, widthMeasureSpec),
            resolveSize(desiredSize, heightMeasureSpec)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Background dots
        for (i in 0 until maxLevel) {
            drawDot(canvas, i, backgroundPaint, 1f)
        }

        // Progress dots + animation
        for (i in 0 until currentLevel) {
            val isAnimated = (i == animatedDotIndex && animationProgress < 1f)
            val scale = if (isAnimated) 1f + (1f - animationProgress) * 0.8f else 1f
            val alpha = if (isAnimated) (animationProgress * 255).toInt() else 255

            progressPaint.alpha = alpha
            drawDot(canvas, i, progressPaint, scale)

            // ✨ Star flash
            if (isAnimated) {
                glowPaint.alpha = ((1f - animationProgress) * 180).toInt()
                drawGlow(canvas, i, scale)
            }
        }

        // Center text
        if (showLevelText) {
            val textY = centerY - (textPaint.descent() + textPaint.ascent()) / 2
            canvas.drawText("$currentLevel", centerX, textY, textPaint)
        }
    }

    private fun drawDot(canvas: Canvas, index: Int, paint: Paint, scale: Float) {
        val angle = Math.toRadians(-90 + index * 360.0 / maxLevel)
        val x = centerX + radius * cos(angle).toFloat()
        val y = centerY + radius * sin(angle).toFloat()

        val baseRadius = minDotRadius +
                (maxDotRadius - minDotRadius) * (index.toFloat() / (maxLevel - 1))

        canvas.drawCircle(x, y, baseRadius * scale, paint)
    }

    private fun drawGlow(canvas: Canvas, index: Int, scale: Float) {
        val angle = Math.toRadians(-90 + index * 360.0 / maxLevel)
        val x = centerX + radius * cos(angle).toFloat()
        val y = centerY + radius * sin(angle).toFloat()

        val glowRadius = maxDotRadius * scale * 1.4f
        canvas.drawCircle(x, y, glowRadius, glowPaint)
    }

    private fun startDotAnimation() {
        animationProgress = 0f
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 250
            addUpdateListener {
                animationProgress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }
}
