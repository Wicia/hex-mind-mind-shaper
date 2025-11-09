package pl.hexmind.mindshaper.common

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.common.drawable.dpToPx

class StepProgressBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var stepCount: Int = 4
        set(value) {
            field = value
            invalidate()
        }

    var currentStep: Int = 0
        set(value) {
            field = value.coerceIn(0, stepCount - 1)
            invalidate()
        }

    var stepLabels: List<String> = emptyList()
        set(value) {
            field = value
            invalidate()
        }

    var stepIcons: List<Drawable?> = emptyList()
        set(value) {
            field = value
            invalidate()
        }

    // States colors
    var stateCompletedColor: Int = ContextCompat.getColor(context, R.color._orange_medium)
    var stateCurrentColor: Int = ContextCompat.getColor(context, R.color._orange_grim)
    var stateUncompletedColor: Int = ContextCompat.getColor(context, R.color._gray_light)

    // Line colors
    var lineCompletedColor: Int = ContextCompat.getColor(context, R.color._gray_light)
    var lineUncompletedColor: Int = ContextCompat.getColor(context, R.color._gray_light)

    // Dimensions
    var circleRadius: Float = 6.0f.dpToPx()
    var lineHeight: Float = 1.5f
    var labelTextSize: Float = 32.0f
    var labelMarginTop: Float = 20.0f
    var iconSize: Float = 36.0f.dpToPx()

    // References
    private var iconRef: Int = 0

    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }

    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.StepProgressBar, 0, 0).apply {
            try {
                stepCount = getInt(R.styleable.StepProgressBar_stepCount, stepCount)
                currentStep = getInt(R.styleable.StepProgressBar_currentStep, currentStep)
                stateCompletedColor = getColor(R.styleable.StepProgressBar_completedStateColor, stateCompletedColor)
                stateCurrentColor = getColor(R.styleable.StepProgressBar_currentStateColor, stateCurrentColor)
                stateUncompletedColor = getColor(R.styleable.StepProgressBar_uncompletedStateColor, stateUncompletedColor)
                circleRadius = getDimension(R.styleable.StepProgressBar_circleRadius, circleRadius)
                lineHeight = getDimension(R.styleable.StepProgressBar_lineHeight, lineHeight)
                labelTextSize = getDimension(R.styleable.StepProgressBar_labelTextSize, labelTextSize)
                iconSize = getDimension(R.styleable.StepProgressBar_iconSize, iconSize)
                iconRef = getResourceId(R.styleable.StepProgressBar_iconResourceId, 0)

                if (iconRef != 0) {
                    stepIcons = List(stepCount) {
                        AppCompatResources.getDrawable(context, iconRef)?.mutate()
                    }
                }
            } finally {
                recycle()
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = MeasureSpec.getSize(widthMeasureSpec)

        // Max radius is either icon or circle
        val maxRadius = (iconSize / 2).coerceAtLeast(circleRadius)

        val desiredHeight = (maxRadius * 2 + labelMarginTop + labelTextSize + paddingTop + paddingBottom).toInt()
        setMeasuredDimension(desiredWidth, desiredHeight)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (stepCount <= 0) return

        val maxRadius = (iconSize / 2).coerceAtLeast(circleRadius)

        val availableWidth = width - paddingLeft - paddingRight - (maxRadius * 2)
        val stepSpacing = if (stepCount > 1) availableWidth / (stepCount - 1) else 0f
        val centerY = paddingTop + maxRadius
        val startX = paddingLeft + maxRadius

        // Drawing: circles for all states except current, icon only for current
        for (i in 0 until stepCount) {
            val centerX = startX + i * stepSpacing

            if (i == currentStep) {
                // Current state: draw rotated icon
                stepIcons.getOrNull(i)?.let { icon ->
                    canvas.save()
                    canvas.rotate(45f, centerX, centerY)

                    icon.setBounds(
                        (centerX - iconSize / 2).toInt(),
                        (centerY - iconSize / 2).toInt(),
                        (centerX + iconSize / 2).toInt(),
                        (centerY + iconSize / 2).toInt()
                    )
                    icon.setTint(stateCurrentColor)
                    icon.draw(canvas)

                    canvas.restore()
                }
            } else {
                // Completed and uncompleted states: draw constant size circles
                circlePaint.color = if (i < currentStep) {
                    stateCompletedColor
                } else {
                    stateUncompletedColor
                }
                canvas.drawCircle(centerX, centerY, circleRadius, circlePaint)
            }
        }

        // Lines drawing
        val lineGap = 0.5f * context.resources.displayMetrics.density

        // Line before first element
        linePaint.color = lineUncompletedColor
        linePaint.strokeWidth = lineHeight
        canvas.drawLine(
            paddingLeft.toFloat(),
            centerY,
            startX - maxRadius - lineGap,
            centerY,
            linePaint
        )

        // Lines between elements
        for (i in 0 until stepCount - 1) {
            val startXPos = startX + i * stepSpacing
            val endXPos = startX + (i + 1) * stepSpacing

            linePaint.color = if (i < currentStep) lineCompletedColor else lineUncompletedColor
            linePaint.strokeWidth = lineHeight

            canvas.drawLine(
                startXPos + maxRadius + lineGap,
                centerY,
                endXPos - maxRadius - lineGap,
                centerY,
                linePaint
            )
        }

        // Line after last element
        val lastElementX = startX + (stepCount - 1) * stepSpacing
        linePaint.color = lineUncompletedColor
        linePaint.strokeWidth = lineHeight
        canvas.drawLine(
            lastElementX + maxRadius + lineGap,
            centerY,
            width - paddingRight.toFloat(),
            centerY,
            linePaint
        )

        // Labels drawing
        for (i in 0 until stepCount) {
            val centerX = startX + i * stepSpacing
            stepLabels.getOrNull(i)?.let { label ->
                textPaint.color = if (i <= currentStep) stateCurrentColor else stateUncompletedColor
                textPaint.textSize = labelTextSize
                canvas.drawText(label, centerX, centerY + maxRadius + labelMarginTop + labelTextSize / 2, textPaint)
            }
        }
    }

    fun setSteps(labels: List<String>) {
        stepCount = labels.size
        stepLabels = labels
    }

    fun goToNextStep() {
        if (currentStep < stepCount - 1) currentStep++
    }

    fun goToPreviousStep() {
        if (currentStep > 0) currentStep--
    }
}