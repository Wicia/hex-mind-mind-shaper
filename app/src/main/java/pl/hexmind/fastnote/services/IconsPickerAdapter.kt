package pl.hexmind.fastnote.services

import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import pl.hexmind.fastnote.R

/**
 * Adapter for displaying icons in 3-column picker dialog with scrolling
 */
class IconPickerAdapter(
    private val icons: List<Int>,
    private val iconsMap: Map<Int, Drawable>,
    private var selectedIconNumber: Int,
    private val onIconClick: (Int) -> Unit
) : RecyclerView.Adapter<IconPickerAdapter.IconViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IconViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_icon_picker, parent, false)
        return IconViewHolder(view)
    }

    override fun onBindViewHolder(holder: IconViewHolder, position: Int) {
        val iconNumber = icons[position]
        holder.bind(iconNumber, iconsMap[iconNumber], iconNumber == selectedIconNumber)
    }

    override fun getItemCount(): Int = icons.size

    inner class IconViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconView: ImageView = itemView.findViewById(R.id.icon_image)
        private val selectionIndicator: View = itemView.findViewById(R.id.selection_indicator)

        fun bind(iconNumber: Int, drawable: Drawable?, isSelected: Boolean) {
            // Set icon with fallback
            if (drawable != null) {
                iconView.setImageDrawable(drawable)
            } else {
                iconView.setImageResource(R.drawable.ic_domain_default)
            }

            // Show/hide selection indicator
            selectionIndicator.visibility = if (isSelected) View.VISIBLE else View.GONE

            // Background color for selected / ripple for others
            if (isSelected) {
                itemView.setBackgroundColor(
                    ContextCompat.getColor(itemView.context, R.color._orange_extra_dark)
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
                notifyItemChanged(icons.indexOf(oldSelected))
                notifyItemChanged(adapterPosition)
                onIconClick(iconNumber)
            }
        }
    }
}
