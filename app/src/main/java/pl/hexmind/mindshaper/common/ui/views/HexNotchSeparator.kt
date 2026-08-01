package pl.hexmind.mindshaper.common.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import pl.hexmind.mindshaper.R

/**
 * Horizontal separator with a V notch in the middle.
 *
 * XML usage:
 *   <pl.hexmind.mindshaper.common.ui.views.HexNotchSeparator
 *       android:layout_width="match_parent"
 *       android:layout_height="wrap_content"
 *       app:notchDepth="8dp" />
 *
 * Programmatic:
 *   separator.setSeparatorColor(R.color._gray_lvl_2)
 */
class HexNotchSeparator @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val DEFAULT_THICKNESS_DP = 4f
        private const val DEFAULT_NOTCH_WIDTH_DP = 20f
        private const val DEFAULT_NOTCH_DEPTH_DP = 8f
    }

    private var thickness = dp(DEFAULT_THICKNESS_DP)
    private var notchWidth = dp(DEFAULT_NOTCH_WIDTH_DP)
    private var notchDepth = dp(DEFAULT_NOTCH_DEPTH_DP)

    // Drawn as a single stroked path so the arms and the notch always meet cleanly
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        // Butt on the ends keeps the line flush with the edges, round join softens the V tip
        strokeCap = Paint.Cap.BUTT
        strokeJoin = Paint.Join.ROUND
    }

    private val path = Path()

    init {
        paint.color = ContextCompat.getColor(context, R.color.separator_background)

        attrs?.let {
            val ta = context.obtainStyledAttributes(it, R.styleable.HexNotchSeparator)
            try {
                thickness = ta.getDimension(
                    R.styleable.HexNotchSeparator_separatorThickness, thickness
                )
                notchWidth = ta.getDimension(
                    R.styleable.HexNotchSeparator_notchWidth, notchWidth
                )
                notchDepth = ta.getDimension(
                    R.styleable.HexNotchSeparator_notchDepth, notchDepth
                )
                paint.color = ta.getColor(
                    R.styleable.HexNotchSeparator_separatorColor, paint.color
                )
            } finally {
                ta.recycle()
            }
        }

        paint.strokeWidth = thickness
    }

    fun setSeparatorColor(colorRes: Int) {
        paint.color = ContextCompat.getColor(context, colorRes)
        invalidate()
    }

    fun setNotchDepth(depthPx: Float) {
        notchDepth = depthPx
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // The stroke is centered on the path, so half of it sticks out above and below the V tip
        val desiredHeight = (thickness + notchDepth).toInt()
        setMeasuredDimension(
            getDefaultSize(suggestedMinimumWidth, widthMeasureSpec),
            resolveSize(desiredHeight, heightMeasureSpec)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val armY = thickness / 2f
        val halfNotch = notchWidth / 2f

        path.reset()
        path.moveTo(0f, armY)
        path.lineTo(centerX - halfNotch, armY)
        path.lineTo(centerX, armY + notchDepth)
        path.lineTo(centerX + halfNotch, armY)
        path.lineTo(width.toFloat(), armY)

        canvas.drawPath(path, paint)
    }

    private fun dp(value: Float): Float = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics
    )
}
