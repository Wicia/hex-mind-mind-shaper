package pl.hexmind.mindshaper.common.ui.views.lists

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.RecyclerView
import pl.hexmind.mindshaper.R

class CommonIconsListAdapter(
    private val icons: List<CommonIconsListItem>,
    private val onIconClick: (CommonIconsListItem) -> Unit
) : RecyclerView.Adapter<CommonIconsListAdapter.IconViewHolder>() {

    private var selectedPosition = -1

    // ViewHolder - "holding" references to views
    inner class IconViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivIcon: ImageView = itemView.findViewById(R.id.iv_icon)
        val tvLabel: TextView = itemView.findViewById(R.id.tv_icon_label)
        val vSelector: View = itemView.findViewById(R.id.v_selector)

        fun bind(itemList: CommonIconsListItem, position: Int) {
            ivIcon.setImageResource(itemList.iconResourceId)
            tvLabel.text = itemList.labelText
            if(itemList.highlightItem){
                tvLabel.setTypeface(null, Typeface.BOLD)
            }

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
                onIconClick(itemList)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IconViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.z_icons_list_item_dialog, parent, false)
        return IconViewHolder(view)
    }

    override fun onBindViewHolder(holder: IconViewHolder, position: Int) {
        holder.bind(icons[position], position)
    }

    override fun getItemCount(): Int = icons.size
}

data class CommonIconsListItem(
    // Icon
    @DrawableRes
    val iconResourceId: Int, // Android drawable resource id
    val iconEntityId : Int? = null, // Can be e.g. domain ID

    // Text under icon
    val labelText: String,
    val labelEntityId : Int? = null,

    val isSelected: Boolean = false,

    val highlightItem : Boolean = false
)