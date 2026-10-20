package pl.hexmind.mindshaper.common.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import pl.hexmind.mindshaper.R

/**
 * Pull tab hanging from the bottom edge of a card - "there is more below": label + arrow (down = expand, up = collapse).
 * Shape: the card's bottom edge waves down into a flat bottom and back up (S-curves, no corners).
 * Filled with the card's color, so it reads as a part of the card; optional outline continues the card's border.
 *
 * XML usage (right under the card, overlapping its bottom edge):
 *   <pl.hexmind.mindshaper.common.ui.views.HexPullTab
 *       android:layout_width="wrap_content"
 *       android:layout_height="wrap_content"
 *       android:layout_gravity="center_horizontal"
 *       android:layout_marginTop="-1dp"
 *       app:tabColor="@android:color/white" />
 *
 * Programmatic:
 *   pullTab.setText("Pokaż jeszcze 3")
 *   pullTab.setExpanded(true)
 */
class HexPullTab @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    companion object {
        // Wide run = gentle slope
        private const val DEFAULT_SHOULDER_WIDTH_DP = 48f
        // Control points' reach along the wave (0.5 = plain S); more = rounder bends at the card's edge and at the bottom
        private const val BEND_ROUNDNESS = 0.6f
        private const val CONTENT_PADDING_HORIZONTAL_DP = 12f
        private const val CONTENT_PADDING_VERTICAL_DP = 8f
        private const val MIN_HEIGHT_DP = 40f
        private const val ARROW_SIZE_DP = 20f
        private const val ARROW_GAP_DP = 4f
        private const val TEXT_SIZE_SP = 14f
        // Arrow drawn from ic_action_chevron_right: turned down = expand, up = collapse
        private const val ARROW_ROTATION_COLLAPSED = 90f
        private const val ARROW_ROTATION_EXPANDED = 270f
    }

    private var shoulderWidth = dp(DEFAULT_SHOULDER_WIDTH_DP)

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, android.R.color.white)
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private var hasStroke = false

    // Fill closes along the card's edge; the outline leaves that edge open - the card draws it
    private val fillPath = Path()
    private val outlinePath = Path()

    private val tvLabel: TextView
    private val ivArrow: ImageView

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER
        minimumHeight = dp(MIN_HEIGHT_DP).toInt()
        // The shape is drawn in onDraw, under the label and the arrow
        setWillNotDraw(false)
        isClickable = true
        isFocusable = true

        attrs?.let {
            val ta = context.obtainStyledAttributes(it, R.styleable.HexPullTab)
            try {
                fillPaint.color = ta.getColor(R.styleable.HexPullTab_tabColor, fillPaint.color)
                shoulderWidth = ta.getDimension(R.styleable.HexPullTab_tabShoulderWidth, shoulderWidth)
                if (ta.hasValue(R.styleable.HexPullTab_tabStrokeColor)) {
                    hasStroke = true
                    strokePaint.color = ta.getColor(R.styleable.HexPullTab_tabStrokeColor, 0)
                    strokePaint.strokeWidth = ta.getDimension(R.styleable.HexPullTab_tabStrokeWidth, dp(1f))
                }
            } finally {
                ta.recycle()
            }
        }

        // Content sits on the flat bottom, between the two waves
        val horizontalPadding = (shoulderWidth + dp(CONTENT_PADDING_HORIZONTAL_DP)).toInt()
        val verticalPadding = dp(CONTENT_PADDING_VERTICAL_DP).toInt()
        setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)

        tvLabel = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SIZE_SP)
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
        }
        ivArrow = ImageView(context).apply {
            setImageResource(R.drawable.ic_action_chevron_right)
            imageTintList = ContextCompat.getColorStateList(context, R.color.text_secondary)
            rotation = ARROW_ROTATION_COLLAPSED
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        addView(tvLabel, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        addView(
            ivArrow,
            LayoutParams(dp(ARROW_SIZE_DP).toInt(), dp(ARROW_SIZE_DP).toInt()).apply {
                marginStart = dp(ARROW_GAP_DP).toInt()
            }
        )

        // Ripple without a frame - the tab's own shape is the only edge
        val outValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true)
        foreground = ContextCompat.getDrawable(context, outValue.resourceId)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        buildPaths(w.toFloat(), h.toFloat())
    }

    /**
     * Each wave: an S-curve from the card's edge (top, horizontal tangent) to the flat bottom (horizontal tangent),
     * so there is no corner anywhere along the edge.
     */
    private fun buildPaths(width: Float, height: Float) {
        // Outline stays inside the view - half of the stroke would be cut off at the bottom
        val inset = if (hasStroke) strokePaint.strokeWidth / 2f else 0f
        val bottom = height - inset
        val shoulder = shoulderWidth.coerceAtMost(width / 2f)
        // Top bend's control point reaches forward from the card's edge, bottom bend's back from the flat bottom
        val reach = shoulder * BEND_ROUNDNESS

        outlinePath.reset()
        outlinePath.moveTo(0f, 0f)
        outlinePath.cubicTo(reach, 0f, shoulder - reach, bottom, shoulder, bottom)
        outlinePath.lineTo(width - shoulder, bottom)
        outlinePath.cubicTo(width - shoulder + reach, bottom, width - reach, 0f, width, 0f)

        fillPath.reset()
        fillPath.addPath(outlinePath)
        fillPath.close()
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawPath(fillPath, fillPaint)
        if (hasStroke) canvas.drawPath(outlinePath, strokePaint)
        super.onDraw(canvas)
    }

    private fun dp(value: Float): Float = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics
    )

    // ===========================================
    //      Public API Methods
    // ===========================================

    fun setText(text: CharSequence) {
        tvLabel.text = text
        contentDescription = text
    }

    /** Arrow down = there is more to show, up = shown all, tap collapses */
    fun setExpanded(expanded: Boolean) {
        ivArrow.rotation = if (expanded) ARROW_ROTATION_EXPANDED else ARROW_ROTATION_COLLAPSED
    }
}
