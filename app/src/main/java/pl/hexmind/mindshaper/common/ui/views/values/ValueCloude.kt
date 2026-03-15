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

    // ✨ Flash animation paint
    private val flashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 8f
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

    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Cloud size calculation: base size + offset for outer circles + padding
        val cloudExtent = (squareSize * 0.22f * 2) + (squareSize / 6f * 0.83f * 2) + 20f
        val desiredSize = (squareSize + cloudExtent).toInt()
        setMeasuredDimension(
            resolveSize(desiredSize, widthMeasureSpec),
            resolveSize(desiredSize, heightMeasureSpec)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw cloud shape (9 circles - B2 variant)
        drawCloud(canvas)

        // ✨ Flash animation on cloud
        if (flashProgress < 1f) {
            flashPaint.alpha = ((1f - flashProgress) * 200).toInt()
            val flashScale = 1f + (1f - flashProgress) * 0.15f
            drawCloudFlash(canvas, flashScale)
        }

        // Center text
        if (showLevelText) {
            val textY = centerY - (textPaint.descent() + textPaint.ascent()) / 2
            canvas.drawText("$currentLevel", centerX, textY, textPaint)
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

    private fun drawCloudFlash(canvas: Canvas, scale: Float) {
        val baseRadius = squareSize / 6f * scale
        val mainRadius = baseRadius * 0.83f
        val diagonalRadius = baseRadius * 0.63f

        val mainOffset = squareSize * 0.22f
        val diagonalOffset = squareSize * 0.16f

        // Center circle
        canvas.drawCircle(centerX, centerY, baseRadius, flashPaint)

        // Main directions
        canvas.drawCircle(centerX, centerY - mainOffset, mainRadius, flashPaint)
        canvas.drawCircle(centerX, centerY + mainOffset, mainRadius, flashPaint)
        canvas.drawCircle(centerX - mainOffset, centerY, mainRadius, flashPaint)
        canvas.drawCircle(centerX + mainOffset, centerY, mainRadius, flashPaint)

        // Diagonals
        canvas.drawCircle(centerX - diagonalOffset, centerY - diagonalOffset, diagonalRadius, flashPaint)
        canvas.drawCircle(centerX + diagonalOffset, centerY - diagonalOffset, diagonalRadius, flashPaint)
        canvas.drawCircle(centerX - diagonalOffset, centerY + diagonalOffset, diagonalRadius, flashPaint)
        canvas.drawCircle(centerX + diagonalOffset, centerY + diagonalOffset, diagonalRadius, flashPaint)
    }

    private fun startFlashAnimation() {
        flashProgress = 0f
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 250
            addUpdateListener {
                flashProgress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }
}