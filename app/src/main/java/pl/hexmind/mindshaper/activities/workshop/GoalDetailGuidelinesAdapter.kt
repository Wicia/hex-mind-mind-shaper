package pl.hexmind.mindshaper.activities.workshop

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import pl.hexmind.mindshaper.R

/**
 * Adapter for the guidelines list in GoalDetailActivity.
 * ! Plain RecyclerView.Adapter (no ListAdapter/DiffUtil) — avoids async diff conflicts with drag.
 */
class GoalDetailGuidelinesAdapter(
    private val onTapText: (GoalGuideline) -> Unit,
    private val onLongPress: (guidelineId: Int) -> Unit,
    private val onToggleDone: (guidelineId: Int) -> Unit,
    private val onStartDrag: (holder: RecyclerView.ViewHolder) -> Unit
) : RecyclerView.Adapter<GoalDetailGuidelinesAdapter.ViewHolder>() {

    private val items = mutableListOf<GoalGuideline>()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cbDone: CheckBox  = itemView.findViewById(R.id.cb_guideline_done)
        val tvDesc: TextView  = itemView.findViewById(R.id.tv_guideline_description)
        val ivDrag: ImageView = itemView.findViewById(R.id.iv_guideline_drag_handle)
    }

    fun setItems(list: List<GoalGuideline>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    fun moveItem(from: Int, to: Int) {
        items.add(to, items.removeAt(from))
        notifyItemMoved(from, to)
    }

    fun getOrderedIds(): List<Int> = items.map { it.id }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.goal_detail_guideline_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val guideline = items[position]

        // Checkbox — clear listener before setting state to avoid spurious callbacks on rebind
        holder.cbDone.setOnCheckedChangeListener(null)
        holder.cbDone.isChecked = guideline.isDone
        holder.cbDone.setOnCheckedChangeListener { _, _ -> onToggleDone(guideline.id) }

        // Description:
        holder.tvDesc.text = guideline.description
        applyDoneStyle(holder.tvDesc, guideline.isDone)

        // Tap
        holder.tvDesc.setOnClickListener { onTapText(guideline) }

        // Long press
        holder.tvDesc.setOnLongClickListener {
            onLongPress(guideline.id)
            true
        }

        // Drag handle — touch starts ItemTouchHelper drag
        holder.ivDrag.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) onStartDrag(holder)
            false
        }
    }

    private fun applyDoneStyle(tv: TextView, isDone: Boolean) {
        if (isDone) {
            tv.paintFlags = tv.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            tv.setTextColor(ContextCompat.getColor(tv.context, R.color._gray_lvl_3))
        } else {
            tv.paintFlags = tv.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            tv.setTextColor(ContextCompat.getColor(tv.context, R.color.text_primary))
        }
    }
}
