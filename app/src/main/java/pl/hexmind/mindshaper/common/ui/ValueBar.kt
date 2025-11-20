package pl.hexmind.mindshaper.common.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import pl.hexmind.mindshaper.R
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import androidx.core.graphics.toColorInt

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
            field = value.coerceIn(MIN_FILLED_DOTS, maxLevel)
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

    var dotBackgroundColor: Int = "#E0E0E0".toColorInt()
        set(value) {
            field = value
            backgroundPaint.color = value
            invalidate()
        }

    var dotProgressColor: Int = "#00BCD4".toColorInt()
        set(value) {
            field = value
            progressPaint.color = value
            invalidate()
        }

    var dotTextColor: Int = "#607D8B".toColorInt()
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

    // Paints for drawing
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
        isFakeBoldText = false
    }

    private var centerX: Float = 0f
    private var centerY: Float = 0f

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

                showLevelText = typedArray.getBoolean(R.styleable.ValueBar_showLevelText, true)
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

        // Adjusting font size dynamically
        val viewSize = min(w, h)
        textPaint.textSize = (viewSize * 0.3f).coerceIn(20f, 72f)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredSize = ((radius + maxDotRadius + 40f) * 2).toInt()

        val width = resolveSize(desiredSize, widthMeasureSpec)
        val height = resolveSize(desiredSize, heightMeasureSpec)

        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw all dots
        for (i in 0 until maxLevel) {
            val angle = Math.toRadians((-90 + (i * 360.0 / maxLevel)))
            val dotX = centerX + radius * cos(angle).toFloat()
            val dotY = centerY + radius * sin(angle).toFloat()

            val dotRadius = minDotRadius + (maxDotRadius - minDotRadius) * (i.toFloat() / (maxLevel - 1))

            canvas.drawCircle(dotX, dotY, dotRadius, backgroundPaint)
        }

        // Draw filled dots
        for (i in 0 until currentLevel) {
            val angle = Math.toRadians((-90 + (i * 360.0 / maxLevel)))
            val dotX = centerX + radius * cos(angle).toFloat()
            val dotY = centerY + radius * sin(angle).toFloat()

            val dotRadius = minDotRadius + (maxDotRadius - minDotRadius) * (i.toFloat() / (maxLevel - 1))

            canvas.drawCircle(dotX, dotY, dotRadius, progressPaint)
        }

        // Text drawing
        if (showLevelText) {
            val levelText = "$currentLevel"
            val textY = centerY - (textPaint.descent() + textPaint.ascent()) / 2
            canvas.drawText(levelText, centerX, textY, textPaint)
        }
    }
}