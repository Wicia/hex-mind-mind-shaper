package pl.hexmind.mindshaper.activities.settings

import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import pl.hexmind.mindshaper.R

/**
 * Adapter for displaying icons in 4-column picker dialog with scrolling
 */
class IconPickerAdapter(
    private val iconsIds: List<Int>,
    private val iconsMap: Map<Int, Drawable>,
    private var selectedIconNumber: Int,
    private val onIconClick: (Int) -> Unit
) : RecyclerView.Adapter<IconPickerAdapter.IconViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IconViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.common_dialog_icons_list_item, parent, false)
        return IconViewHolder(view)
    }

    override fun onBindViewHolder(holder: IconViewHolder, position: Int) {
        // ! Database icon id = position on the grid
        val iconNumber = iconsIds[position]
        holder.bind(iconNumber, iconsMap[iconNumber], iconNumber == selectedIconNumber)
    }

    override fun getItemCount(): Int = iconsIds.size

    inner class IconViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivIcon: ImageView = itemView.findViewById(R.id.iv_icon)
        private val vSelector: View = itemView.findViewById(R.id.v_selector)

        fun bind(iconNumber: Int, drawable: Drawable?, isSelected: Boolean) {
            // Set icon with fallback
            if (drawable != null) {
                ivIcon.setImageDrawable(drawable)
                ivIcon.imageTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(itemView.context, R.color._black)
                )
            } else {
                ivIcon.setImageResource(R.drawable.ic_domain_default)
            }

            // Show/hide selection indicator
            vSelector.visibility = if (isSelected) View.VISIBLE else View.GONE

            // Background color for selected / ripple for others
            if (isSelected) {
                itemView.setBackgroundColor(
                    ContextCompat.getColor(itemView.context, R.color.button_primary)
                )
            } else {
                val outValue = TypedValue()
                itemView.context.theme.resolveAttribute(
                    android.R.attr.selectableItemBackground,
                    outValue,
                    true
                )
                itemView.setBackgroundResource(outValue.resourceId)
            }

            // Handle click — update selection and notify
            itemView.setOnClickListener {
                val oldSelected = selectedIconNumber
                selectedIconNumber = iconNumber
                notifyItemChanged(iconsIds.indexOf(oldSelected))
                notifyItemChanged(adapterPosition)
                onIconClick(iconNumber)
            }
        }
    }
}
