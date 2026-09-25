package pl.hexmind.mindshaper.common.ui.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import pl.hexmind.mindshaper.R

/**
 * Segmented tab switch for N tabs - one joined bar with a notch pointing at the active tab.
 *
 * XML usage:
 *   <pl.hexmind.mindshaper.common.ui.views.HexTabSwitch
 *       android:layout_width="match_parent"
 *       android:layout_height="wrap_content"
 *       app:notchDepth="10dp" />
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
        private const val DEFAULT_TAB_HEIGHT_DP = 44f
        private const val DEFAULT_NOTCH_WIDTH_DP = 22f
        private const val DEFAULT_NOTCH_DEPTH_DP = 9f
        private const val DEFAULT_CORNER_RADIUS_DP = 10f
        private const val DEFAULT_ACTIVE_LIFT_DP = 4f
        private const val DEFAULT_ACTIVE_STROKE_DP = 2f
        private const val DEFAULT_TEXT_SIZE_SP = 16f
        private const val NOTCH_ANIMATION_DURATION_MS = 200L
        private const val NOTCH_OVERLAP_PX = 2f
    }

    private var tabHeight = dp(DEFAULT_TAB_HEIGHT_DP)
    private var notchWidth = dp(DEFAULT_NOTCH_WIDTH_DP)
    private var notchDepth = dp(DEFAULT_NOTCH_DEPTH_DP)
    private var cornerRadius = dp(DEFAULT_CORNER_RADIUS_DP)
    private var activeLift = dp(DEFAULT_ACTIVE_LIFT_DP)
    private var activeStrokeWidth = dp(DEFAULT_ACTIVE_STROKE_DP)

    private var activeColor = ContextCompat.getColor(context, R.color._orange_lvl_2)
    private var inactiveColor = ContextCompat.getColor(context, R.color._orange_lvl_1)
    private var activeTextColor = ContextCompat.getColor(context, R.color.text_primary)
    private var inactiveTextColor = ContextCompat.getColor(context, R.color.text_secondary)
    private var activeStrokeColor = ContextCompat.getColor(context, R.color.app_background)

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = sp(DEFAULT_TEXT_SIZE_SP)
    }

    private val path = Path()
    private val notchPath = Path()
    private val clipBounds = RectF()

    private var tabs: List<String> = emptyList()

    var selectedIndex = 0
        private set

    var onTabSelected: ((Int) -> Unit)? = null

    // Animated separately from selectedIndex so the notch can slide while the colors flip instantly
    private var notchPosition = 0f
    private var notchAnimator: ValueAnimator? = null

    init {
        attrs?.let {
            val ta = context.obtainStyledAttributes(it, R.styleable.HexTabSwitch)
            try {
                tabHeight = ta.getDimension(
                    R.styleable.HexTabSwitch_tabHeight, tabHeight
                )
                notchWidth = ta.getDimension(
                    R.styleable.HexTabSwitch_notchWidth, notchWidth
                )
                notchDepth = ta.getDimension(
                    R.styleable.HexTabSwitch_notchDepth, notchDepth
                )
                cornerRadius = ta.getDimension(
                    R.styleable.HexTabSwitch_tabCornerRadius, cornerRadius
                )
                activeLift = ta.getDimension(
                    R.styleable.HexTabSwitch_tabActiveLift, activeLift
                )
                activeStrokeWidth = ta.getDimension(
                    R.styleable.HexTabSwitch_tabActiveStrokeWidth, activeStrokeWidth
                )
                activeStrokeColor = ta.getColor(
                    R.styleable.HexTabSwitch_tabActiveStrokeColor, activeStrokeColor
                )
                activeColor = ta.getColor(
                    R.styleable.HexTabSwitch_tabActiveColor, activeColor
                )
                inactiveColor = ta.getColor(
                    R.styleable.HexTabSwitch_tabInactiveColor, inactiveColor
                )
                activeTextColor = ta.getColor(
                    R.styleable.HexTabSwitch_tabActiveTextColor, activeTextColor
                )
                inactiveTextColor = ta.getColor(
                    R.styleable.HexTabSwitch_tabInactiveTextColor, inactiveTextColor
                )
                textPaint.textSize = ta.getDimension(
                    R.styleable.HexTabSwitch_tabTextSize, textPaint.textSize
                )
            } finally {
                ta.recycle()
            }
        }

        strokePaint.strokeWidth = activeStrokeWidth
        strokePaint.color = activeStrokeColor
    }

    fun setTabs(labels: List<String>, initialIndex: Int = 0) {
        tabs = labels
        selectedIndex = initialIndex.coerceIn(0, (labels.size - 1).coerceAtLeast(0))
        notchPosition = selectedIndex.toFloat()
        updateContentDescription()
        requestLayout()
    }

    /** Moves the notch without firing onTabSelected - for restoring state. */
    fun setSelectedIndex(index: Int, animate: Boolean = false) {
        if (tabs.isEmpty()) {
            return
        }

        selectedIndex = index.coerceIn(0, tabs.size - 1)
        updateContentDescription()

        if (animate) {
            animateNotchTo(selectedIndex.toFloat())
        }
        else {
            notchAnimator?.cancel()
            notchPosition = selectedIndex.toFloat()
            invalidate()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredHeight = (tabHeight + activeLift + notchDepth).toInt()
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

        // Inactive first: the active tab is taller, so it must paint over its neighbours' edges
        tabs.forEachIndexed { index, label ->
            if (index != selectedIndex) {
                drawSegment(canvas, index, label, segmentWidth, isActive = false)
            }
        }

        tabs.getOrNull(selectedIndex)?.let { activeLabel ->
            drawSegment(canvas, selectedIndex, activeLabel, segmentWidth, isActive = true)
        }
    }

    /**
     * The active segment is drawn taller and outlined, so it reads as lifted above the strip.
     * Inactive segments start below the lift; the bar baseline is the same for all.
     */
    private fun drawSegment(
        canvas: Canvas,
        index: Int,
        label: String,
        segmentWidth: Float,
        isActive: Boolean
    ) {
        val left = index * segmentWidth
        val right = left + segmentWidth
        val top = if (isActive) 0f else activeLift
        val bottom = activeLift + tabHeight

        // Only the outer edges of the whole bar are rounded, so segments read as one joined strip
        val roundLeft = index == 0
        val roundRight = index == tabs.size - 1

        buildSegmentPath(left, right, top, bottom, roundLeft, roundRight, isActive)

        // Tab body and notch are ONE shape, so they are unioned before painting - otherwise the
        // outline would cut across the notch mouth and trace its hidden base
        if (isActive) {
            buildNotchPath(segmentWidth, bottom)
            path.op(notchPath, Path.Op.UNION)
        }

        fillPaint.color = if (isActive) activeColor else inactiveColor
        canvas.drawPath(path, fillPaint)

        if (isActive) {
            canvas.drawPath(path, strokePaint)
        }

        textPaint.color = if (isActive) activeTextColor else inactiveTextColor
        val textCenterY = (top + bottom) / 2f
        val textY = textCenterY - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(label, left + segmentWidth / 2f, textY, textPaint)
    }

    private fun buildSegmentPath(
        left: Float,
        right: Float,
        top: Float,
        bottom: Float,
        roundLeft: Boolean,
        roundRight: Boolean,
        isActive: Boolean
    ) {
        // Half the stroke sits outside the path, so the active body is inset to keep the outline inside the view
        val inset = if (isActive) activeStrokeWidth / 2f else 0f
        clipBounds.set(left + inset, top + inset, right - inset, bottom)

        // The active tab rounds its own top corners too - that lifted edge is what the outline traces
        val topLeft = if (roundLeft || isActive) cornerRadius else 0f
        val topRight = if (roundRight || isActive) cornerRadius else 0f
        val bottomRight = if (roundRight) cornerRadius else 0f
        val bottomLeft = if (roundLeft) cornerRadius else 0f

        val radii = floatArrayOf(
            topLeft, topLeft,
            topRight, topRight,
            bottomRight, bottomRight,
            bottomLeft, bottomLeft
        )

        path.reset()
        path.addRoundRect(clipBounds, radii, Path.Direction.CW)
    }

    /** Triangle overlapping the bar bottom, so the union has no seam where it meets the body. */
    private fun buildNotchPath(segmentWidth: Float, barBottom: Float) {
        val centerX = (notchPosition + 0.5f) * segmentWidth
        val halfNotch = notchWidth / 2f
        val inset = activeStrokeWidth / 2f

        notchPath.reset()
        notchPath.moveTo(centerX - halfNotch, barBottom - NOTCH_OVERLAP_PX)
        notchPath.lineTo(centerX, barBottom + notchDepth - inset)
        notchPath.lineTo(centerX + halfNotch, barBottom - NOTCH_OVERLAP_PX)
        notchPath.close()
    }

    private fun animateNotchTo(target: Float) {
        notchAnimator?.cancel()
        notchAnimator = ValueAnimator.ofFloat(notchPosition, target).apply {
            duration = NOTCH_ANIMATION_DURATION_MS
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                notchPosition = animator.animatedValue as Float
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
