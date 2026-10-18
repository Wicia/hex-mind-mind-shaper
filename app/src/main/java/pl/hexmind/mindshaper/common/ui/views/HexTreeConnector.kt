package pl.hexmind.mindshaper.common.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import pl.hexmind.mindshaper.R

/**
 * Tree branch in front of a nested row: a line coming down from the parent above,
 * turning with a rounded corner into the middle of the row's left edge.
 *
 * Each nested row has its own connector. A next one (gapAbove > 0) starts above its row, just under
 * the previous branch, so the vertical lines almost join - parents must not clip children.
 *
 * Usage: first child of a horizontal row, width = HexTreeConnector.WIDTH_DP, height = match_parent.
 */
class HexTreeConnector @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        const val WIDTH_DP = 28
        private const val LINE_X_DP = 12f
        private const val CORNER_RADIUS_DP = 8f
        private const val LINE_THICKNESS_DP = 2f
        private const val END_GAP_DP = 2f
        private const val JOIN_GAP_DP = 3f
    }

    private val lineX = dp(LINE_X_DP)
    private val cornerRadius = dp(CORNER_RADIUS_DP)
    private val endGap = dp(END_GAP_DP)
    private val joinGap = dp(JOIN_GAP_DP)

    /** Gap (px) between this row and the previous one with a branch; 0 = first branch, starts at the top. */
    var gapAbove: Int = 0
        set(value) {
            field = value
            invalidate()
        }

    // Round caps keep the branch ends soft
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = dp(LINE_THICKNESS_DP)
        color = ContextCompat.getColor(context, R.color.tree_connector)
    }

    private val branchPath = Path()
    private val cornerRect = RectF()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Caps stick out by half the stroke, so both ends are pulled in to stay inside the view
        val halfStroke = linePaint.strokeWidth / 2f
        val centerY = height / 2f

        // Next branch reaches up to just under the previous one - assumes rows of the same height
        val startY = if (gapAbove > 0) -(gapAbove + centerY - halfStroke - joinGap) else halfStroke

        branchPath.reset()
        branchPath.moveTo(lineX, startY)
        branchPath.lineTo(lineX, centerY - cornerRadius)
        cornerRect.set(lineX, centerY - 2 * cornerRadius, lineX + 2 * cornerRadius, centerY)
        branchPath.arcTo(cornerRect, 180f, -90f)
        branchPath.lineTo(width - endGap - halfStroke, centerY)

        canvas.drawPath(branchPath, linePaint)
    }

    private fun dp(value: Float): Float = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics
    )
}
