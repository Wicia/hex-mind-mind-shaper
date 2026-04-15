package pl.hexmind.mindshaper.common.ui.views.values

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import pl.hexmind.mindshaper.R

/**
 * UI controller class for displaying thought value with cloud shape (9 circles)
 * TODO: Make cloud shape.xml instead + text view :) = SIMPLE
 */
class ValueCloude @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        const val DEFAULT_LEVEL = 0
    }

    var squareSize: Float = 120f
        set(value) {
            field = value
            invalidate()
        }

    var currentLevel: Int = DEFAULT_LEVEL
        set(value) {
            if (value != field) {
                startFlashAnimation()
            }
            field = value
            invalidate()
            onLevelChangeListener?.invoke(field)
        }

    var squareColor: Int = R.color._orange_lvl_2
        set(value) {
            field = value
            squarePaint.color = ContextCompat.getColor(context, value)
            invalidate()
        }

    var textColor: Int = R.color._black
        set(value) {
            field = value
            textPaint.color = ContextCompat.getColor(context, value)
            invalidate()
        }

    var textSize: Float = 14f
        set(value) {
            field = value
            textPaint.textSize = value
            invalidate()
        }

    var showLevelText: Boolean = true
        set(value) {
            field = value
            invalidate()
        }

    var isLocked: Boolean = false // for "slow mode"
        set(value) {
            field = value
            squarePaint.color = ContextCompat.getColor(
                context, if (value) R.color._gray_lvl_2 else squareColor
            )
            invalidate()
        }

    // Callback
    var onLevelChangeListener: ((Int) -> Unit)? = null

    // Paints
    private val squarePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, squareColor)
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, textColor)
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14f, context.resources.displayMetrics)
        textAlign = Paint.Align.CENTER
        typeface = ResourcesCompat.getFont(context, R.font.baloo2)
    }

    // Animating glow effect when changing thought's value
    private val glowRings = listOf(
        Triple(1.05f,  5f, 210),   // scale, strokeWidth, maxAlpha — inner: sharp bright ring
        Triple(1.13f,  9f, 120),   // middle: softer halo
        Triple(1.23f, 14f,  50),   // outer: wide diffuse bloom
    )
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = ContextCompat.getColor(context, R.color._orange_lvl_3)
        alpha = 0
    }

    private var centerX = 0f
    private var centerY = 0f

    // Animation state
    private var flashProgress: Float = 1f

    init {
        // Loading XML attributes
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.ValueCloude, 0, 0)
            try {
                squareSize = typedArray.getDimension(R.styleable.ValueCloude_squareSize, 120f)
                currentLevel = typedArray.getInteger(R.styleable.ValueCloude_level, DEFAULT_LEVEL)

                squareColor = typedArray.getResourceId(
                    R.styleable.ValueCloude_squareColor,
                    R.color._orange_lvl_2
                )
                textColor = typedArray.getResourceId(
                    R.styleable.ValueCloude_textColor,
                    R.color._black
                )

                textSize = typedArray.getDimension(
                    R.styleable.ValueCloude_textSize,
                    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14f, context.resources.displayMetrics)
                )

                showLevelText = typedArray.getBoolean(R.styleable.ValueCloude_showLevelText, true)
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
        squareSize = minOf(w, h).toFloat()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desired = squareSize.toInt()
        setMeasuredDimension(
            resolveSize(desired, widthMeasureSpec),
            resolveSize(desired, heightMeasureSpec)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // To ensure that animation is displayed properly
        if (flashProgress < 1f) {
            val fade = 1f - flashProgress
            glowRings.forEach { (scale, strokeW, maxAlpha) ->
                glowPaint.strokeWidth = strokeW
                glowPaint.alpha = (fade * maxAlpha).toInt()
                drawCloudGlowRing(canvas, scale)
            }
        }

        // Draw cloud shape on top — covers internal ring intersections
        drawCloud(canvas)

        // Center text
        if (showLevelText) {
            val textY = centerY - (textPaint.descent() + textPaint.ascent()) / 2
            val label = if (isLocked) "?" else "$currentLevel"
            canvas.drawText(label, centerX, textY, textPaint)
        }
    }

    private fun drawCloud(canvas: Canvas) {
        val baseRadius = squareSize / 6f // B2: center radius = 30 when squareSize = 180
        val mainRadius = baseRadius * 0.75f // Main directions slightly smaller
        val diagonalRadius = baseRadius * 0.65f // Diagonal circles smaller

        val mainOffset = squareSize * 0.18f // B2: offset = 40 when squareSize = 180
        val diagonalOffset = squareSize * 0.15f // B2: offset = 29 when squareSize = 180

        // Center circle
        canvas.drawCircle(centerX, centerY, baseRadius, squarePaint)

        // Main directions (top, bottom, left, right)
        canvas.drawCircle(centerX, centerY - mainOffset, mainRadius, squarePaint) // Top
        canvas.drawCircle(centerX, centerY + mainOffset, mainRadius, squarePaint) // Bottom
        canvas.drawCircle(centerX - mainOffset, centerY, mainRadius, squarePaint) // Left
        canvas.drawCircle(centerX + mainOffset, centerY, mainRadius, squarePaint) // Right

        // Diagonals
        canvas.drawCircle(centerX - diagonalOffset, centerY - diagonalOffset, diagonalRadius, squarePaint) // Top-left
        canvas.drawCircle(centerX + diagonalOffset, centerY - diagonalOffset, diagonalRadius, squarePaint) // Top-right
        canvas.drawCircle(centerX - diagonalOffset, centerY + diagonalOffset, diagonalRadius, squarePaint) // Bottom-left
        canvas.drawCircle(centerX + diagonalOffset, centerY + diagonalOffset, diagonalRadius, squarePaint) // Bottom-right
    }

    // Draw the cloud outline for cool tapping effect :)
    private fun drawCloudGlowRing(canvas: Canvas, scale: Float) {
        val baseRadius     = squareSize / 6f
        val mainRadius     = baseRadius * 0.75f
        val diagonalRadius = baseRadius * 0.65f

        val mainOffset     = squareSize * 0.18f   // same as drawCloud
        val diagonalOffset = squareSize * 0.15f   // same as drawCloud

        canvas.drawCircle(centerX, centerY, baseRadius * scale, glowPaint)

        canvas.drawCircle(centerX, centerY - mainOffset, mainRadius * scale, glowPaint)
        canvas.drawCircle(centerX, centerY + mainOffset, mainRadius * scale, glowPaint)
        canvas.drawCircle(centerX - mainOffset, centerY, mainRadius * scale, glowPaint)
        canvas.drawCircle(centerX + mainOffset, centerY, mainRadius * scale, glowPaint)

        canvas.drawCircle(centerX - diagonalOffset, centerY - diagonalOffset, diagonalRadius * scale, glowPaint)
        canvas.drawCircle(centerX + diagonalOffset, centerY - diagonalOffset, diagonalRadius * scale, glowPaint)
        canvas.drawCircle(centerX - diagonalOffset, centerY + diagonalOffset, diagonalRadius * scale, glowPaint)
        canvas.drawCircle(centerX + diagonalOffset, centerY + diagonalOffset, diagonalRadius * scale, glowPaint)
    }

    private fun startFlashAnimation() {
        flashProgress = 0f
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 400  // slightly longer so the outer bloom is visible
            addUpdateListener {
                flashProgress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }
}