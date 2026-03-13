package pl.hexmind.mindshaper.activities.workshop

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import pl.hexmind.mindshaper.R

/**
 * Adapter for the goals list in WorkshopActivity.
 * Modes: "read-only" and "edit"
 */
class GoalsAdapter(
    // TODO: move callbacks to interface?
    private val onToggleExpand: (goalId: Int) -> Unit,
    private val onCyclePriority: (goalId: Int) -> Unit,
    private val onGoalEditTap: (goal: Goal) -> Unit,
    private val onGoalDeleteTap: (goalId: Int) -> Unit,
    private val onToggleSubItemDone: (goalId: Int, subItemId: Int) -> Unit,
    private val onSubItemEditTap: (goalId: Int, subItem: GoalGuideline) -> Unit,
    private val onSubItemDeleteTap: (goalId: Int, subItemId: Int) -> Unit,
    private val onSubItemReorder: (goalId: Int, from: Int, to: Int) -> Unit,
    private val onAddSubItemTap: (goalId: Int) -> Unit
) : ListAdapter<Goal, GoalsAdapter.GoalViewHolder>(DiffCallback) {

    var isEditMode: Boolean = false
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    private object DiffCallback : DiffUtil.ItemCallback<Goal>() {
        override fun areItemsTheSame(old: Goal, new: Goal) =
            old.id == new.id
        override fun areContentsTheSame(old: Goal, new: Goal) =
            old == new
    }

    inner class GoalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val llHeader: View = itemView.findViewById(R.id.ll_goal_header)
        val tvPriority: TextView = itemView.findViewById(R.id.tv_priority_badge)
        val tvDescription: TextView = itemView.findViewById(R.id.tv_goal_description)
        val ivChevron: ImageView = itemView.findViewById(R.id.iv_goal_chevron)
        val ivDelete: ImageView = itemView.findViewById(R.id.iv_goal_delete)
        val divider: View = itemView.findViewById(R.id.divider_goal)
        val rvSubItems: RecyclerView = itemView.findViewById(R.id.rv_subitems)
        val llSubItemsFooter: View = itemView.findViewById(R.id.ll_subitems_footer)
        val tvAddSubItem: TextView = itemView.findViewById(R.id.tv_add_subitem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoalViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_goal, parent, false)
        return GoalViewHolder(view)
    }

    override fun onBindViewHolder(holder: GoalViewHolder, position: Int) {
        val goal = getItem(position)

        // Priority badge
        holder.tvPriority.text = goal.priority.toString()
        applyPriorityBadgeStyle(holder.tvPriority, goal.priority)
        holder.tvPriority.setOnClickListener { onCyclePriority(goal.id) }

        holder.tvDescription.text = goal.description

        if (isEditMode) {
            bindEditMode(holder, goal)
        } else {
            bindNormalMode(holder, goal)
        }
    }

    // ── Normal mode ────────────────────────────────────────────────────────────

    private fun bindNormalMode(holder: GoalViewHolder, goal: Goal) {
        // Chevron visible, trash hidden
        holder.ivChevron.visibility = View.VISIBLE
        holder.ivDelete.visibility = View.GONE
        holder.llSubItemsFooter.visibility = View.GONE

        // Chevron rotation reflects expanded state
        holder.ivChevron.rotation = if (goal.isExpanded) 90f else 270f

        // Tap header (except badge) → expand/collapse
        holder.llHeader.setOnClickListener { onToggleExpand(goal.id) }

        // Sub-items visibility
        val subVisible = goal.isExpanded && goal.subItems.isNotEmpty()
        holder.divider.visibility = if (subVisible) View.VISIBLE else View.GONE
        holder.rvSubItems.visibility = if (subVisible) View.VISIBLE else View.GONE

        if (subVisible) {
            setupSubItemsRecyclerView(holder, goal, editMode = false)
        }
    }

    // ── Edit mode ──────────────────────────────────────────────────────────────

    private fun bindEditMode(holder: GoalViewHolder, goal: Goal) {
        // Chevron hidden, trash visible
        holder.ivChevron.visibility = View.GONE
        holder.ivDelete.visibility = View.VISIBLE

        // Tap header (except badge + trash) → open edit dialog
        holder.llHeader.setOnClickListener { onGoalEditTap(goal) }

        // Delete with confirmation handled in Activity
        holder.ivDelete.setOnClickListener { onGoalDeleteTap(goal.id) }

        // Sub-items always shown in edit mode
        val hasSubItems = goal.subItems.isNotEmpty()
        holder.divider.visibility = View.VISIBLE
        holder.rvSubItems.visibility = if (hasSubItems) View.VISIBLE else View.GONE

        // Footer "+ Dodaj krok" always visible in edit mode
        holder.llSubItemsFooter.visibility = View.VISIBLE
        holder.tvAddSubItem.setOnClickListener { onAddSubItemTap(goal.id) }

        if (hasSubItems) {
            setupSubItemsRecyclerView(holder, goal, editMode = true)
        }
    }

    private fun setupSubItemsRecyclerView(
        holder: GoalViewHolder,
        goal: Goal,
        editMode: Boolean
    ) {
        val subAdapter = GuidelinesAdapter(
            onToggleDone = { subId -> onToggleSubItemDone(goal.id, subId) },
            onEditTap = { sub -> onSubItemEditTap(goal.id, sub) },
            onDeleteTap = { subId -> onSubItemDeleteTap(goal.id, subId) },
            onReorder = { from, to -> onSubItemReorder(goal.id, from, to) }
        ).apply {
            isEditMode = editMode
            submitList(goal.subItems)
        }

        holder.rvSubItems.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = subAdapter
            // Prevent nested scroll conflict — parent NestedScrollView owns scrolling
            isNestedScrollingEnabled = false
        }

        // Attach ItemTouchHelper for drag & drop (edit mode only)
        if (editMode) {
            val touchHelper = ItemTouchHelper(SubItemTouchCallback(subAdapter))
            touchHelper.attachToRecyclerView(holder.rvSubItems)
            subAdapter.touchHelper = touchHelper
        }
    }

    private fun applyPriorityBadgeStyle(badge: TextView, priority: Int) {
        val context = badge.context
        val (bgColorRes, textColorRes) = when (priority) {
            1 -> Pair(R.color.priority_high, R.color.graphite_medium)
            2 -> Pair(R.color.priority_medium, R.color.graphite_medium)
            else -> Pair(R.color.priority_low, R.color.graphite_medium)
        }
        badge.backgroundTintList = ColorStateList.valueOf(context.getColor(bgColorRes))
        badge.setTextColor(context.getColor(textColorRes))
    }

    // ── ItemTouchHelper callback for sub-item drag & drop ─────────────────────

    private inner class SubItemTouchCallback(
        private val adapter: GuidelinesAdapter
    ) : ItemTouchHelper.SimpleCallback(
        ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
    ) {
        // Track the original position when drag starts and current target position
        private var dragFrom: Int = RecyclerView.NO_ID.toInt()
        private var dragTo: Int = RecyclerView.NO_ID.toInt()

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            val from = viewHolder.adapterPosition
            val to = target.adapterPosition
            if (dragFrom == RecyclerView.NO_ID.toInt()) dragFrom = from
            dragTo = to
            adapter.moveItem(from, to)
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            // No swipe actions
        }

        // Only drag via the handle, not long press
        override fun isLongPressDragEnabled() = false

        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            super.clearView(recyclerView, viewHolder)
            // Persist final order to ViewModel once drag is complete
            if (dragFrom != RecyclerView.NO_ID.toInt() && dragFrom != dragTo) {
                adapter.onReorder(dragFrom, dragTo)
            }
            dragFrom = RecyclerView.NO_ID.toInt()
            dragTo = RecyclerView.NO_ID.toInt()
        }
    }
}