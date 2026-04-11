package pl.hexmind.mindshaper.common.ui.views.lists

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import pl.hexmind.mindshaper.R

/**
 * Horizontal tile-based radio group — each tile shows an icon + label.
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
) : LinearLayout(context, attrs, defStyleAttr) {

    data class Option(
        val id: Int,
        @StringRes val labelRes: Int,
        @DrawableRes val iconRes: Int
    )

    var onSelectionChanged: ((selectedId: Int) -> Unit)? = null

    private var selectedId: Int = -1
    private val disabledIds = mutableSetOf<Int>()

    // LinkedHashMap preserves insertion order (used for fallback: first non-disabled)
    private val tileViews = linkedMapOf<Int, View>()

    init {
        orientation = HORIZONTAL
    }

    fun setOptions(options: List<Option>) {
        removeAllViews()
        tileViews.clear()

        options.forEachIndexed { index, option ->
            val tileView = LayoutInflater.from(context)
                .inflate(R.layout.z_radio_tile_item_view, this, false)

            tileView.findViewById<ImageView>(R.id.iv_tile_icon).setImageResource(option.iconRes)
            tileView.findViewById<TextView>(R.id.tv_tile_label).setText(option.labelRes)

            // Gap between tiles (not before the first one)
            if (index > 0) {
                (tileView.layoutParams as LayoutParams).marginStart = dpToPx(8)
            }

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

        icon.setColorFilter(contentColor)
        label.setTextColor(contentColor)
    }

    // TODO: move it somewhere?
    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()
}
