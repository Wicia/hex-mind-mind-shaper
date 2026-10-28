package pl.hexmind.mindshaper.common.ui.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import pl.hexmind.mindshaper.R

/**
 * Tab switch for N tabs - a label over its own underline with the ends curled up; the active one
 * also has a triangular tip pointing down (isosceles, straight walls), so it reads as a brace.
 *
 * XML usage:
 *   <pl.hexmind.mindshaper.common.ui.views.HexTabSwitch
 *       android:layout_width="match_parent"
 *       android:layout_height="wrap_content"
 *       app:notchDepth="8dp" />
 *
 * Programmatic:
 *   tabSwitch.setTabs(listOf("Osoby", "Projekty"))
 *   tabSwitch.setTabs(listOf(HexTabSwitch.Tab("Cele", emoji = "⌖"), HexTabSwitch.Tab("Ścieżki", iconRes = R.drawable.ic_…)))
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
        private const val DEFAULT_NOTCH_WIDTH_DP = 22f
        private const val DEFAULT_NOTCH_DEPTH_DP = 11f
        private const val DEFAULT_BRACE_CURL_DP = 9f
        private const val DEFAULT_TEXT_SIZE_SP = 16f
        private const val DEFAULT_ICON_SIZE_DP = 18f
        private const val DEFAULT_ICON_GAP_DP = 6f
        private const val BRACE_ANIMATION_DURATION_MS = 200L
    }

    private var tabHeight = dp(DEFAULT_TAB_HEIGHT_DP)
    private var lineThickness = dp(DEFAULT_LINE_THICKNESS_DP)
    private var gap = dp(DEFAULT_GAP_DP)
    private var notchWidth = dp(DEFAULT_NOTCH_WIDTH_DP)
    private var notchDepth = dp(DEFAULT_NOTCH_DEPTH_DP)
    private var braceCurl = dp(DEFAULT_BRACE_CURL_DP)
    private val iconSize = dp(DEFAULT_ICON_SIZE_DP)
    private val iconGap = dp(DEFAULT_ICON_GAP_DP)

    private var activeColor = ContextCompat.getColor(context, R.color._palette_lvl_2)
    private var inactiveColor = ContextCompat.getColor(context, R.color._palette_lvl_1)
    private var activeTextColor = ContextCompat.getColor(context, R.color.text_primary)
    private var inactiveTextColor = ContextCompat.getColor(context, R.color.text_secondary)

    // Round cap and join keep the line ends and the brace tip soft
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        // Label and its leading icon / emoji are laid out together, from the left edge of the group
        textAlign = Paint.Align.LEFT
        textSize = sp(DEFAULT_TEXT_SIZE_SP)
    }

    private val underlinePath = Path()
    private val cornerRect = RectF()

    /** One tab: label with an optional leading icon (vector drawable, tinted like the text) or emoji - one of them, not both. */
    data class Tab(
        val label: String,
        @DrawableRes val iconRes: Int? = null,
        val emoji: String? = null
    ) {
        init {
            require(iconRes == null || emoji == null) { "Tab takes an icon or an emoji, not both" }
        }
    }

    private var tabs: List<Tab> = emptyList()
    private var icons: List<Drawable?> = emptyList()

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

        linePaint.strokeWidth = lineThickness
    }

    fun setTabs(labels: List<String>, initialIndex: Int = 0) {
        setTabs(labels.map { Tab(it) }, initialIndex)
    }

    @JvmName("setTabItems")
    fun setTabs(items: List<Tab>, initialIndex: Int = 0) {
        tabs = items
        icons = items.map { tab ->
            tab.iconRes?.let { ContextCompat.getDrawable(context, it)?.mutate() }
        }
        selectedIndex = initialIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
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

        tabs.forEachIndexed { index, tab ->
            drawTab(canvas, index, tab, segmentWidth, lineY, isActive = index == selectedIndex)
        }

        drawBrace(canvas, segmentWidth, lineY)
    }

    private fun drawTab(
        canvas: Canvas,
        index: Int,
        tab: Tab,
        segmentWidth: Float,
        lineY: Float,
        isActive: Boolean
    ) {
        val left = index * segmentWidth
        val centerX = left + segmentWidth / 2f

        textPaint.color = if (isActive) activeTextColor else inactiveTextColor
        textPaint.typeface = if (isActive) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        val textY = tabHeight / 2f - (textPaint.descent() + textPaint.ascent()) / 2f

        // The whole group (icon / emoji + gap + label) is centered in the segment
        val icon = icons.getOrNull(index)
        val leadWidth = when {
            icon != null -> iconSize
            tab.emoji != null -> textPaint.measureText(tab.emoji)
            else -> 0f
        }
        val leadGap = if (leadWidth > 0f) iconGap else 0f
        var x = centerX - (leadWidth + leadGap + textPaint.measureText(tab.label)) / 2f

        if (icon != null) {
            // Tinted on every draw, so the icon always follows the label's current color
            icon.setTint(textPaint.color)
            val top = (tabHeight - iconSize) / 2f
            icon.setBounds(x.toInt(), top.toInt(), (x + iconSize).toInt(), (top + iconSize).toInt())
            icon.draw(canvas)
        }
        else if (tab.emoji != null) {
            canvas.drawText(tab.emoji, x, textY, textPaint)
        }
        x += leadWidth + leadGap

        canvas.drawText(tab.label, x, textY, textPaint)

        // The active underline is the brace, drawn on its own so it can slide between tabs
        if (isActive) {
            return
        }

        buildUnderline(left + lineInset(), left + segmentWidth - lineInset(), lineY, withTip = false)
        linePaint.color = inactiveColor
        canvas.drawPath(underlinePath, linePaint)
    }

    // Round caps stick out by half the stroke, so the ends are pulled in to keep the gap exact
    private fun lineInset(): Float = gap / 2f + lineThickness / 2f

    /** The active tab's brace - follows the animated position, not selectedIndex. */
    private fun drawBrace(canvas: Canvas, segmentWidth: Float, lineY: Float) {
        val left = bracePosition * segmentWidth + lineInset()
        val right = (bracePosition + 1f) * segmentWidth - lineInset()

        buildUnderline(left, right, lineY, withTip = true)
        linePaint.color = activeColor
        canvas.drawPath(underlinePath, linePaint)
    }

    /**
     * Underline with its ends curled up (quarter circles of radius braceCurl) - every tab has it.
     * withTip: the middle drops into an isosceles triangle tip under the label.
     */
    private fun buildUnderline(left: Float, right: Float, lineY: Float, withTip: Boolean) {
        underlinePath.reset()
        underlinePath.moveTo(left, lineY - braceCurl)
        cornerRect.set(left, lineY - 2 * braceCurl, left + 2 * braceCurl, lineY)
        underlinePath.arcTo(cornerRect, 180f, -90f)

        if (withTip) {
            val centerX = (left + right) / 2f
            val halfNotch = notchWidth / 2f
            underlinePath.lineTo(centerX - halfNotch, lineY)
            underlinePath.lineTo(centerX, lineY + notchDepth)
            underlinePath.lineTo(centerX + halfNotch, lineY)
        }

        underlinePath.lineTo(right - braceCurl, lineY)
        cornerRect.set(right - 2 * braceCurl, lineY - 2 * braceCurl, right, lineY)
        underlinePath.arcTo(cornerRect, 90f, -90f)
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
        contentDescription = tabs.getOrNull(selectedIndex)?.label
    }

    private fun dp(value: Float): Float = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics
    )

    private fun sp(value: Float): Float = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP, value, resources.displayMetrics
    )
}
