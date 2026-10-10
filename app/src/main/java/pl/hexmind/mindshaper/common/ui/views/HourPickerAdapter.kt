package pl.hexmind.mindshaper.common.ui.views

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import pl.hexmind.mindshaper.R

/**
 * Slots of the horizontal time-of-day picker.
 * Center item is always the selected one; side items scale down via [HexHourPickerView].
 * Centering of first/last entries is handled by RecyclerView edge padding, not ghost items.
 */
class HourPickerAdapter(
    private val slots: List<String>
) : RecyclerView.Adapter<HourPickerAdapter.SlotViewHolder>() {

    class SlotViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dot: View = view.findViewById(R.id.v_hour_dot)
        val label: TextView = view.findViewById(R.id.tv_hour_label)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlotViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.hour_picker_item, parent, false)
        return SlotViewHolder(view)
    }

    override fun onBindViewHolder(holder: SlotViewHolder, position: Int) {
        holder.label.text = slots[position]
    }

    override fun getItemCount(): Int = slots.size
}