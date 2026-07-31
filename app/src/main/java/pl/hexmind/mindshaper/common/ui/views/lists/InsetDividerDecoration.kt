package pl.hexmind.mindshaper.common.ui.views.lists

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import pl.hexmind.mindshaper.R

/**
 * Thin divider drawn between list items
 *
 * ! Drawn as a decoration (not a View in the item layout) so the last item
 * never gets a trailing line and item layouts stay untouched.
 */
class InsetDividerDecoration(
    context: Context,
    startInsetDp: Int,
    endInsetDp: Int = 0,
    // ! Overridable because the default tone is invisible on a _gray_lvl_1 surface (archive panel)
    dividerColorRes: Int = R.color._gray_lvl_1
) : RecyclerView.ItemDecoration() {

    private val density = context.resources.displayMetrics.density
    private val startInset = startInsetDp * density
    private val endInset = endInsetDp * density

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, dividerColorRes)
        strokeWidth = density  // 1dp
    }

    override fun onDrawOver(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val left = parent.paddingLeft + startInset
        val right = parent.width - parent.paddingRight - endInset

        // Skip the last child so no line hangs below the list
        for (index in 0 until parent.childCount - 1) {
            val child: View = parent.getChildAt(index)
            val y = child.bottom + child.translationY
            canvas.drawLine(left, y, right, y, paint)
        }
    }
}
