package pl.hexmind.mindshaper.activities.workshop

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import pl.hexmind.mindshaper.R

/**
 * Adapter for sub-items inside an expanded goal row.
 * Modes: "read-only" and "edit"
 */
class GuidelinesAdapter(
    private val onToggleDone: (subItemId: Int) -> Unit,
    private val onEditTap: (subItem: GoalGuideline) -> Unit,
    private val onDeleteTap: (subItemId: Int) -> Unit,
    internal val onReorder: (from: Int, to: Int) -> Unit
) : RecyclerView.Adapter<GuidelinesAdapter.SubItemViewHolder>() {

    private var items: List<GoalGuideline> = emptyList()
    var isEditMode: Boolean = false

    // Set by the parent adapter so the drag handle can trigger drag
    var touchHelper: ItemTouchHelper? = null

    fun submitList(newItems: List<GoalGuideline>) {
        items = newItems
        notifyDataSetChanged()
    }

    /** Called during drag to update the visual order immediately. */
    fun moveItem(from: Int, to: Int) {
        val mutable = items.toMutableList()
        val moved = mutable.removeAt(from)
        mutable.add(to, moved)
        items = mutable
        notifyItemMoved(from, to)
    }

    inner class SubItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivDragHandle: ImageView = itemView.findViewById(R.id.iv_sub_drag_handle)
        val cbDone: CheckBox = itemView.findViewById(R.id.cb_subitem_done)
        val tvDescription: TextView = itemView.findViewById(R.id.tv_subitem_description)
        val ivDelete: ImageView = itemView.findViewById(R.id.iv_sub_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.goal_guideline_item, parent, false)
        return SubItemViewHolder(view)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: SubItemViewHolder, position: Int) {
        val sub = items[position]
        val context = holder.itemView.context

        holder.tvDescription.text = sub.description

        if (isEditMode) {
            // ── Edit mode ──────────────────────────────────────────────────────
            holder.cbDone.visibility = View.GONE
            holder.ivDragHandle.visibility = View.VISIBLE
            holder.ivDelete.visibility = View.VISIBLE

            // Keep done text color so user can still see which steps are done
            holder.tvDescription.setTextColor(
                if (sub.isDone) context.getColor(R.color._gray_lvl_3)
                else context.getColor(R.color.text_primary)
            )

            // Tap row (anywhere except delete) → open edit dialog
            holder.itemView.setOnClickListener { onEditTap(sub) }

            // Drag handle — start drag on touch down
            holder.ivDragHandle.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    touchHelper?.startDrag(holder)
                }
                false
            }

            // Delete
            holder.ivDelete.setOnClickListener { onDeleteTap(sub.id) }

        } else {
            // ── Normal mode ────────────────────────────────────────────────────
            holder.cbDone.visibility = View.VISIBLE
            holder.ivDragHandle.visibility = View.GONE
            holder.ivDelete.visibility = View.GONE

            holder.cbDone.isChecked = sub.isDone
            // Detach listener before setting checked state to avoid callback loop
            holder.cbDone.setOnCheckedChangeListener(null)

            val textColor = if (sub.isDone) context.getColor(R.color._gray_lvl_2)
                else context.getColor(R.color.text_primary)
            holder.tvDescription.setTextColor(textColor)

            // Toggle done: tap row or tap checkbox
            val toggle = { onToggleDone(sub.id) }
            holder.itemView.setOnClickListener { toggle() }
            holder.cbDone.setOnClickListener { toggle() }

            holder.ivDelete.setOnClickListener(null)
            holder.ivDragHandle.setOnTouchListener(null)
        }
    }
}