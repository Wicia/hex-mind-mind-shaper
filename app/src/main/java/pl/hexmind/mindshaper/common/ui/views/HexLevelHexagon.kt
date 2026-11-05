package pl.hexmind.mindshaper.common.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import pl.hexmind.mindshaper.R
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Progress drawn as the sides of a hexagon (pointy top), lit clockwise from the top vertex.
 *
 * XML usage:
 *   <pl.hexmind.mindshaper.common.ui.views.HexLevelHexagon
 *       android:layout_width="@dimen/flashcard_level_hexagon_size"
 *       android:layout_height="@dimen/flashcard_level_hexagon_size" />
 */
class HexLevelHexagon @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        const val SIDE_COUNT = 6

        private const val STROKE_WIDTH_DP = 2.5f
        private const val TOP_VERTEX_DEG = -90.0
        private const val SIDE_DEG = 60.0

        // Part of a side left empty at each end - keeps the sides apart despite the round caps
        private const val SIDE_GAP_FRACTION = 0.22f
    }

    var litSides = 0
        set(value) {
            field = value.coerceIn(0, SIDE_COUNT)
            invalidate()
        }

    private val strokeWidthPx = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, STROKE_WIDTH_DP, resources.displayMetrics
    )

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokeWidthPx
        strokeCap = Paint.Cap.ROUND
    }

    private val litColor = ContextCompat.getColor(context, R.color.flashcard_accent)
    private val unlitColor = ContextCompat.getColor(context, R.color.flashcard_progress_track)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val radius = (min(width, height) - strokeWidthPx) / 2f
        val centerX = width / 2f
        val centerY = height / 2f

        for (side in 0 until SIDE_COUNT) {
            val startAngle = Math.toRadians(TOP_VERTEX_DEG + side * SIDE_DEG)
            val endAngle = Math.toRadians(TOP_VERTEX_DEG + (side + 1) * SIDE_DEG)
            val vertexStartX = centerX + radius * cos(startAngle).toFloat()
            val vertexStartY = centerY + radius * sin(startAngle).toFloat()
            val vertexEndX = centerX + radius * cos(endAngle).toFloat()
            val vertexEndY = centerY + radius * sin(endAngle).toFloat()

            val gapX = (vertexEndX - vertexStartX) * SIDE_GAP_FRACTION
            val gapY = (vertexEndY - vertexStartY) * SIDE_GAP_FRACTION

            paint.color = if (side < litSides) litColor else unlitColor
            canvas.drawLine(
                vertexStartX + gapX, vertexStartY + gapY,
                vertexEndX - gapX, vertexEndY - gapY,
                paint
            )
        }
    }
}
