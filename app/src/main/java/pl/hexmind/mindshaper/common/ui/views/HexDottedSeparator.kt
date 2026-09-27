package pl.hexmind.mindshaper.common.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import pl.hexmind.mindshaper.R

/**
 * Horizontal separator made of round dots.
 *
 * Drawn by hand - a dashed line shape drawable proved unreliable (dash effect silently dropped).
 *
 * XML usage:
 *   <pl.hexmind.mindshaper.common.ui.views.HexDottedSeparator
 *       style="@style/AppCardView_Divider" />
 */
class HexDottedSeparator @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val DOT_DIAMETER_DP = 2f
        private const val DOT_GAP_DP = 6f
    }

    private val dotRadius = dp(DOT_DIAMETER_DP) / 2f
    private val dotStep = dp(DOT_DIAMETER_DP) + dp(DOT_GAP_DP)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color._orange_lvl_1)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // wrap_content height = one dot
        val height = resolveSize(Math.ceil(dp(DOT_DIAMETER_DP).toDouble()).toInt(), heightMeasureSpec)
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val usableWidth = width - paddingLeft - paddingRight
        if (usableWidth <= 0) return

        val centerY = height / 2f

        // Centre the row so both ends leave the same margin
        val dotCount = ((usableWidth - dotRadius * 2) / dotStep).toInt() + 1
        val rowWidth = (dotCount - 1) * dotStep + dotRadius * 2
        var centerX = paddingLeft + (usableWidth - rowWidth) / 2f + dotRadius

        repeat(dotCount) {
            canvas.drawCircle(centerX, centerY, dotRadius, paint)
            centerX += dotStep
        }
    }

    private fun dp(value: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics)
}
