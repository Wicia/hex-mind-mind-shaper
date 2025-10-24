package pl.hexmind.mindshaper.common.ui

import android.graphics.Canvas
import android.graphics.Paint
import android.text.Layout
import android.text.style.LeadingMarginSpan

/**
 * Class representing bullet points in HexTextView
 */
class UnicodeSymbolSpan(
    private val gapWidth: Int = 40,
    private val symbol: String = "●",
    private val color: Int = 0,
    private val textSize: Float = 1.2f // 1.2 = 120% of text size
) : LeadingMarginSpan {

    override fun getLeadingMargin(first: Boolean): Int {
        return gapWidth
    }

    override fun drawLeadingMargin(
        canvas: Canvas,
        paint: Paint,
        x: Int,
        dir: Int,
        top: Int,
        baseline: Int,
        bottom: Int,
        text: CharSequence,
        start: Int,
        end: Int,
        first: Boolean,
        layout: Layout
    ) {
        if (!first) return

        val oldColor = paint.color
        val oldTextSize = paint.textSize

        if (color != 0) {
            paint.color = color
        }

        // Increase text size
        paint.textSize = oldTextSize * textSize

        // Drawing Unicode symbol
        canvas.drawText(
            symbol,
            x + dir * 8f,
            baseline.toFloat(),
            paint
        )

        paint.color = oldColor
        paint.textSize = oldTextSize
    }
}