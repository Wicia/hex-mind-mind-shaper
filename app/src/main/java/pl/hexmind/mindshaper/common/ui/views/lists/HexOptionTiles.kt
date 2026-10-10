package pl.hexmind.mindshaper.common.ui.views.lists

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import pl.hexmind.mindshaper.R
import androidx.core.view.isVisible

/**
 * Tile-based radio group — each tile shows an icon + label.
 * Tiles share one row; 4+ tiles wrap into a 2-column grid when a tile would be
 * narrower than MIN_TILE_WIDTH_DP (narrow screen, large font). Up to 3 tiles never wrap.
 * Exactly one tile is selected at a time.
 * Individual tiles can be disabled (grayed out, non-clickable).
 *
 * Usage:
 *   tileGroup.setOptions(listOf(
 *       HexOptionTiles.Option(id = 0, labelRes = R.string.foo, iconRes = R.drawable.ic_foo),
 *       ...
 *   ))
 *   tileGroup.setSelectedId(someId)
 *   tileGroup.setOptionEnabled(someId, enabled = false)
 *   tileGroup.onSelectionChanged = { selectedId -> ... }
 */
open class HexOptionTiles @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    private companion object {
        const val GAP_DP            = 8
        const val MIN_TILE_WIDTH_DP = 80
        const val GRID_COLUMNS      = 2
        const val GRID_MIN_TILES    = 4
    }

    data class Option(
        val id: Int,
        @StringRes val labelRes: Int,
        @DrawableRes val iconRes: Int? = null
    )

    var onSelectionChanged: ((selectedId: Int) -> Unit)? = null

    private var selectedId: Int = -1
    private val disabledIds = mutableSetOf<Int>()

    // LinkedHashMap preserves insertion order (used for fallback: first non-disabled)
    private val tileViews = linkedMapOf<Int, View>()

    // Resolved in onMeasure, used by onLayout
    private var columns = 1
    private var tileWidth = 0
    private val rowHeights = mutableListOf<Int>()

    fun setOptions(options: List<Option>) {
        removeAllViews()
        tileViews.clear()

        options.forEach { option ->
            val tileView = LayoutInflater.from(context)
                .inflate(R.layout.common_option_tile_item_view, this, false)

            // Icon handling
            val icon = tileView.findViewById<ImageView>(R.id.iv_tile_icon)
            if (option.iconRes != null) {
                icon.setImageResource(option.iconRes)
                icon.visibility = VISIBLE
            } else {
                icon.visibility = GONE
            }

            // Label handling
            tileView.findViewById<TextView>(R.id.tv_tile_label).setText(option.labelRes)

            tileView.setOnClickListener {
                if (option.id !in disabledIds) {
                    setSelectedId(option.id, notify = true)
                }
            }

            tileViews[option.id] = tileView
            addView(tileView)
        }

        refreshAllStates()
    }

    fun setSelectedId(id: Int, notify: Boolean = false) {
        selectedId = id
        refreshAllStates()
        if (notify) onSelectionChanged?.invoke(id)
    }

    fun getSelectedId(): Int = selectedId

    /**
     * Enable or disable an option.
     * If the currently selected option becomes disabled, selection falls back
     * to the first available (non-disabled) option and onSelectionChanged is fired.
     */
    fun setOptionEnabled(id: Int, enabled: Boolean) {
        if (enabled) {
            disabledIds.remove(id)
        } else {
            disabledIds.add(id)
            if (selectedId == id) {
                val fallback = tileViews.keys.firstOrNull { it !in disabledIds }
                if (fallback != null) {
                    selectedId = fallback
                    onSelectionChanged?.invoke(fallback)
                }
            }
        }
        refreshAllStates()
    }

    // ========== LAYOUT ==========

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val gap = dpToPx(GAP_DP)
        val count = childCount
        val contentWidth = width - paddingLeft - paddingRight

        rowHeights.clear()
        if (count == 0) {
            setMeasuredDimension(width, paddingTop + paddingBottom)
            return
        }

        val singleRowTileWidth = (contentWidth - gap * (count - 1)) / count
        columns = if (count < GRID_MIN_TILES || singleRowTileWidth >= dpToPx(MIN_TILE_WIDTH_DP)) count else GRID_COLUMNS
        tileWidth = (contentWidth - gap * (columns - 1)) / columns

        // Tiles in one row share the height of the tallest one
        val widthSpec = MeasureSpec.makeMeasureSpec(tileWidth, MeasureSpec.EXACTLY)
        for (rowStart in 0 until count step columns) {
            val row = (rowStart until minOf(rowStart + columns, count)).map { getChildAt(it) }
            row.forEach { it.measure(widthSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)) }
            val rowHeight = row.maxOf { it.measuredHeight }
            row.forEach { it.measure(widthSpec, MeasureSpec.makeMeasureSpec(rowHeight, MeasureSpec.EXACTLY)) }
            rowHeights += rowHeight
        }

        val height = paddingTop + paddingBottom + rowHeights.sum() + gap * (rowHeights.size - 1)
        setMeasuredDimension(width, resolveSize(height, heightMeasureSpec))
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val gap = dpToPx(GAP_DP)
        val rtl = layoutDirection == LAYOUT_DIRECTION_RTL
        var top = paddingTop

        rowHeights.forEachIndexed { rowIndex, rowHeight ->
            for (column in 0 until columns) {
                val child = getChildAt(rowIndex * columns + column) ?: break
                val offset = column * (tileWidth + gap)
                val left = if (rtl) width - paddingRight - offset - tileWidth else paddingLeft + offset
                child.layout(left, top, left + tileWidth, top + rowHeight)
            }
            top += rowHeight + gap
        }
    }

    // ========== PRIVATE HELPERS ==========

    private fun refreshAllStates() {
        tileViews.forEach { (id, view) ->
            applyTileState(
                view     = view,
                selected = id == selectedId,
                disabled = id in disabledIds
            )
        }
    }

    private fun applyTileState(view: View, selected: Boolean, disabled: Boolean) {
        val icon  = view.findViewById<ImageView>(R.id.iv_tile_icon)
        val label = view.findViewById<TextView>(R.id.tv_tile_label)

        val bgColor: Int
        val strokeColor: Int
        val strokeWidth: Int
        val contentColor: Int

        when {
            disabled -> {
                bgColor      = ContextCompat.getColor(context, R.color._gray_lvl_1)
                strokeColor  = ContextCompat.getColor(context, R.color._gray_lvl_2)
                strokeWidth  = dpToPx(1)
                contentColor = ContextCompat.getColor(context, R.color._gray_lvl_3)
                view.alpha       = 0.45f
                view.isClickable = false
            }
            selected -> {
                bgColor      = ContextCompat.getColor(context, R.color._orange_lvl_1)
                strokeColor  = ContextCompat.getColor(context, R.color._orange_lvl_2)
                strokeWidth  = dpToPx(2)
                contentColor = ContextCompat.getColor(context, R.color._black)
                view.alpha       = 1f
                view.isClickable = true
            }
            // unselected
            else -> {
                bgColor      = ContextCompat.getColor(context, R.color.app_surface)
                strokeColor  = ContextCompat.getColor(context, R.color._gray_lvl_2)
                strokeWidth  = dpToPx(1)
                contentColor = ContextCompat.getColor(context, R.color.text_secondary)
                view.alpha       = 1f
                view.isClickable = true
            }
        }

        view.background = GradientDrawable().apply {
            shape        = GradientDrawable.RECTANGLE
            cornerRadius = dpToPx(12).toFloat()
            setColor(bgColor)
            setStroke(strokeWidth, strokeColor)
        }

        if (icon.isVisible) {
            icon.setColorFilter(contentColor)
        }

        label.setTextColor(contentColor)
    }

    // TODO: move it somewhere?
    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()
}