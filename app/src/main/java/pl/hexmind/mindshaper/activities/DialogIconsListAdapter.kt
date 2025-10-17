package pl.hexmind.mindshaper.activities

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import pl.hexmind.mindshaper.R

data class CommonItemsListDialogAdapter(
    val drawable: Drawable,
    val label: String,
    val isSelected: Boolean = false
)

class DialogIconsListAdapter(
    private val icons: List<CommonItemsListDialogAdapter>,
    private val onIconClick: (CommonItemsListDialogAdapter) -> Unit
) : RecyclerView.Adapter<DialogIconsListAdapter.IconViewHolder>() {

    private var selectedPosition = -1

    // ViewHolder - "holding" references to views
    inner class IconViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivIcon: ImageView = itemView.findViewById(R.id.iv_icon)
        val tvLabel: TextView = itemView.findViewById(R.id.tv_icon_label)
        val vSelector: View = itemView.findViewById(R.id.v_selector)

        fun bind(commonItemsListDialogAdapter: CommonItemsListDialogAdapter, position: Int) {
            ivIcon.setImageDrawable(commonItemsListDialogAdapter.drawable)
            tvLabel.text = commonItemsListDialogAdapter.label

            // Show or hide selector for specific element
            vSelector.visibility = if (position == selectedPosition) View.VISIBLE else View.GONE

            // Listeners
            itemView.setOnClickListener {
                val previousPosition = selectedPosition
                selectedPosition = position

                // ! Refresh previous and current element
                notifyItemChanged(previousPosition)
                notifyItemChanged(selectedPosition)

                // Callback
                onIconClick(commonItemsListDialogAdapter)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IconViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.common_list_item, parent, false)
        return IconViewHolder(view)
    }

    override fun onBindViewHolder(holder: IconViewHolder, position: Int) {
        holder.bind(icons[position], position)
    }

    override fun getItemCount(): Int = icons.size
}