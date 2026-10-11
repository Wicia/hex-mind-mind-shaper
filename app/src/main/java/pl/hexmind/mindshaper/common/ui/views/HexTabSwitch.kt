package pl.hexmind.mindshaper.common.ui.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import pl.hexmind.mindshaper.R

/**
 * Tab switch for N tabs - a label over its own underline; the active one is underlined with a
 * horizontal curly brace (ends curled up, tip pointing down).
 *
 * XML usage:
 *   <pl.hexmind.mindshaper.common.ui.views.HexTabSwitch
 *       android:layout_width="match_parent"
 *       android:layout_height="wrap_content"
 *       app:notchDepth="8dp" />
 *
 * Programmatic:
 *   tabSwitch.setTabs(listOf("Osoby", "Projekty"))
 *   tabSwitch.onTabSelected = { index -> … }
 */
class HexTabSwitch @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val DEFAULT_TAB_HEIGHT_DP = 36f
        private const val DEFAULT_LINE_THICKNESS_DP = 4f
        private const val DEFAULT_GAP_DP = 12f
        private const val DEFAULT_NOTCH_WIDTH_DP = 28f
        private const val DEFAULT_NOTCH_DEPTH_DP = 11f
        private const val DEFAULT_BRACE_CURL_DP = 6f
        private const val DEFAULT_TEXT_SIZE_SP = 16f
        private const val BRACE_ANIMATION_DURATION_MS = 200L
    }

    private var tabHeight = dp(DEFAULT_TAB_HEIGHT_DP)
    private var lineThickness = dp(DEFAULT_LINE_THICKNESS_DP)
    private var gap = dp(DEFAULT_GAP_DP)
    private var notchWidth = dp(DEFAULT_NOTCH_WIDTH_DP)
    private var notchDepth = dp(DEFAULT_NOTCH_DEPTH_DP)
    private var braceCurl = dp(DEFAULT_BRACE_CURL_DP)

    private var activeColor = ContextCompat.getColor(context, R.color._orange_lvl_2)
    private var inactiveColor = ContextCompat.getColor(context, R.color._orange_lvl_1)
    private var textColor = ContextCompat.getColor(context, R.color.text_primary)

    // Round cap and join keep the line ends and the brace tip soft
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = sp(DEFAULT_TEXT_SIZE_SP)
    }

    private val bracePath = Path()

    private var tabs: List<String> = emptyList()

    var selectedIndex = 0
        private set

    var onTabSelected: ((Int) -> Unit)? = null

    // Animated separately from selectedIndex so the brace can slide while the colors flip instantly
    private var bracePosition = 0f
    private var braceAnimator: ValueAnimator? = null

    init {
        attrs?.let {
            val ta = context.obtainStyledAttributes(it, R.styleable.HexTabSwitch)
            try {
                tabHeight = ta.getDimension(
                    R.styleable.HexTabSwitch_tabHeight, tabHeight
                )
                lineThickness = ta.getDimension(
                    R.styleable.HexTabSwitch_tabLineThickness, lineThickness
                )
                gap = ta.getDimension(
                    R.styleable.HexTabSwitch_tabGap, gap
                )
                notchWidth = ta.getDimension(
                    R.styleable.HexTabSwitch_notchWidth, notchWidth
                )
                notchDepth = ta.getDimension(
                    R.styleable.HexTabSwitch_notchDepth, notchDepth
                )
                braceCurl = ta.getDimension(
                    R.styleable.HexTabSwitch_tabBraceCurl, braceCurl
                )
                activeColor = ta.getColor(
                    R.styleable.HexTabSwitch_tabActiveColor, activeColor
                )
                inactiveColor = ta.getColor(
                    R.styleable.HexTabSwitch_tabInactiveColor, inactiveColor
                )
                textColor = ta.getColor(
                    R.styleable.HexTabSwitch_tabTextColor, textColor
                )
                textPaint.textSize = ta.getDimension(
                    R.styleable.HexTabSwitch_tabTextSize, textPaint.textSize
                )
            } finally {
                ta.recycle()
            }
        }

        linePaint.strokeWidth = lineThickness
    }

    fun setTabs(labels: List<String>, initialIndex: Int = 0) {
        tabs = labels
        selectedIndex = initialIndex.coerceIn(0, (labels.size - 1).coerceAtLeast(0))
        bracePosition = selectedIndex.toFloat()
        updateContentDescription()
        requestLayout()
    }

    /** Moves the brace without firing onTabSelected - for restoring state. */
    fun setSelectedIndex(index: Int, animate: Boolean = false) {
        if (tabs.isEmpty()) {
            return
        }

        selectedIndex = index.coerceIn(0, tabs.size - 1)
        updateContentDescription()

        if (animate) {
            animateBraceTo(selectedIndex.toFloat())
        }
        else {
            braceAnimator?.cancel()
            bracePosition = selectedIndex.toFloat()
            invalidate()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // The stroke is centered on the path, so half of it sticks out above the line and below the brace tip
        val desiredHeight = (tabHeight + lineThickness + notchDepth).toInt()
        setMeasuredDimension(
            getDefaultSize(suggestedMinimumWidth, widthMeasureSpec),
            resolveSize(desiredHeight, heightMeasureSpec)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (tabs.isEmpty()) {
            return
        }

        val segmentWidth = width.toFloat() / tabs.size
        val lineY = tabHeight + lineThickness / 2f

        tabs.forEachIndexed { index, label ->
            drawTab(canvas, index, label, segmentWidth, lineY, isActive = index == selectedIndex)
        }

        drawBrace(canvas, segmentWidth, lineY)
    }

    private fun drawTab(
        canvas: Canvas,
        index: Int,
        label: String,
        segmentWidth: Float,
        lineY: Float,
        isActive: Boolean
    ) {
        val left = index * segmentWidth
        val centerX = left + segmentWidth / 2f

        textPaint.color = textColor
        textPaint.typeface = if (isActive) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        val textY = tabHeight / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(label, centerX, textY, textPaint)

        // The active underline is the brace, drawn on its own so it can slide between tabs
        if (isActive) {
            return
        }

        linePaint.color = inactiveColor
        canvas.drawLine(left + lineInset(), lineY, left + segmentWidth - lineInset(), lineY, linePaint)
    }

    // Round caps stick out by half the stroke, so the ends are pulled in to keep the gap exact
    private fun lineInset(): Float = gap / 2f + lineThickness / 2f

    /**
     * Horizontal curly brace across the tab: ends curl up into the line,
     * the middle bends down into a tip under the label.
     */
    private fun drawBrace(canvas: Canvas, segmentWidth: Float, lineY: Float) {
        val left = bracePosition * segmentWidth + lineInset()
        val right = (bracePosition + 1f) * segmentWidth - lineInset()
        val centerX = (left + right) / 2f
        val halfNotch = notchWidth / 2f
        val tipY = lineY + notchDepth

        bracePath.reset()
        bracePath.moveTo(left, lineY - braceCurl)
        bracePath.quadTo(left, lineY, left + braceCurl, lineY)
        bracePath.lineTo(centerX - halfNotch, lineY)
        bracePath.quadTo(centerX, lineY, centerX, tipY)
        bracePath.quadTo(centerX, lineY, centerX + halfNotch, lineY)
        bracePath.lineTo(right - braceCurl, lineY)
        bracePath.quadTo(right, lineY, right, lineY - braceCurl)

        linePaint.color = activeColor
        canvas.drawPath(bracePath, linePaint)
    }

    private fun animateBraceTo(target: Float) {
        braceAnimator?.cancel()
        braceAnimator = ValueAnimator.ofFloat(bracePosition, target).apply {
            duration = BRACE_ANIMATION_DURATION_MS
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                bracePosition = animator.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (tabs.isEmpty()) {
            return false
        }

        if (event.action == MotionEvent.ACTION_UP) {
            val segmentWidth = width.toFloat() / tabs.size
            val tappedIndex = (event.x / segmentWidth).toInt().coerceIn(0, tabs.size - 1)

            performClick()

            if (tappedIndex != selectedIndex) {
                setSelectedIndex(tappedIndex, animate = true)
                onTabSelected?.invoke(tappedIndex)
            }
            return true
        }

        return true
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }

    private fun updateContentDescription() {
        contentDescription = tabs.getOrNull(selectedIndex)
    }

    private fun dp(value: Float): Float = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics
    )

    private fun sp(value: Float): Float = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP, value, resources.displayMetrics
    )
}
