package pl.hexmind.mindshaper.common.ui.views

import android.content.Context
import android.os.Parcelable
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import kotlinx.parcelize.Parcelize
import pl.hexmind.mindshaper.R

/**
 * Reusable icon grid widget with brick/honeycomb row pattern (3-4-3-4...).
 * Single selection — selected item gets filled background via isSelected state.
 * No label, icon only.
 *
 * All items have identical fixed size (48dp square).
 * Rows with fewer items (3) align to start and leave space on the right.
 *
 * Usage:
 *   binding.igvDomains.bind(items, selectedId) { clicked -> ... }
 *   val chosenId = binding.igvDomains.selectedItemId
 */
class IconsGridView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    /** ID of the currently selected item. Null if nothing is selected. */
    var selectedItemId: Int? = null
        private set

    // Flat list of all inflated item views for fast selection updates
    private val itemViews = mutableListOf<View>()

    // Alternating column counts per row: 3, 4, 3, 4...
    private val rowPattern = listOf(3, 4)

    init {
        orientation = VERTICAL
    }

    /**
     * Populate the grid with items.
     *
     * @param items       list of items to display
     * @param selectedId  optional pre-selected item ID
     * @param onItemClick optional callback fired on every tap
     */
    fun bind(
        items: List<IconsGridItem>,
        selectedId: Int? = null,
        onItemClick: ((IconsGridItem) -> Unit)? = null
    ) {
        populate(items, selectedId, onItemClick)
    }

    /**
     * Programmatically change selection (e.g. to restore saved state).
     * Pass null to deselect all.
     */
    fun setSelectedItem(selectedId: Int?) {
        selectedItemId = selectedId
        itemViews.forEach { view ->
            view.isSelected = (view.tag as? Int == selectedId)
        }
    }

    private fun populate(
        items: List<IconsGridItem>,
        selectedId: Int?,
        onItemClick: ((IconsGridItem) -> Unit)?
    ) {
        removeAllViews()
        itemViews.clear()
        selectedItemId = selectedId

        val density = resources.displayMetrics.density
        val itemSize = (64 * density).toInt()
        val itemMargin = (5 * density).toInt()

        buildRows(items).forEach { rowItems ->
            val rowLayout = LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER_HORIZONTAL
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            }

            rowItems.forEach { item ->
                val itemView = LayoutInflater.from(context)
                    .inflate(R.layout.view_icons_grid_item, rowLayout, false)

                // Fixed square size — same for every item regardless of row column count
                // Margins preserved explicitly since we override layoutParams
                itemView.layoutParams = LayoutParams(itemSize, itemSize).apply {
                    setMargins(itemMargin, itemMargin, itemMargin, itemMargin)
                }

                itemView.tag = item.id
                itemView.isSelected = (item.id == selectedId)

                itemView.findViewById<ImageView>(R.id.iv_grid_icon)
                    .setImageResource(item.iconResId)

                itemView.setOnClickListener {
                    val newSelection = if (selectedItemId == item.id) null else item.id
                    selectedItemId = newSelection
                    setSelectedItem(newSelection)
                    onItemClick?.invoke(item)
                }

                itemViews.add(itemView)
                rowLayout.addView(itemView)
            }

            addView(rowLayout)
        }
    }

    /**
     * Splits flat items list into rows following the 3-4-3-4... pattern.
     * Last row may be shorter if items don't divide evenly.
     */
    private fun buildRows(items: List<IconsGridItem>): List<List<IconsGridItem>> {
        val rows = mutableListOf<List<IconsGridItem>>()
        var index = 0
        var patternIndex = 0

        while (index < items.size) {
            val columns = rowPattern[patternIndex % rowPattern.size]
            rows.add(items.subList(index, minOf(index + columns, items.size)))
            index += columns
            patternIndex++
        }

        return rows
    }
}

/** Item model for [IconsGridView]. Parcelable required for Bundle transfer via HexTagsBottomSheet. */
@Parcelize
data class IconsGridItem(
    val id: Int,
    @DrawableRes val iconResId: Int
) : Parcelable