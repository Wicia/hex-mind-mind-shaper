package pl.hexmind.mindshaper.activities.workshop

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import pl.hexmind.mindshaper.R

/**
 * Adapter for the goals list in WorkshopActivity.
 */
class GoalsAdapter(
    private val onGoalTap: (goalId: Int) -> Unit,
    private val onCyclePriority: (goalId: Int) -> Unit,
    private val onGoalLongPress: (goalId: Int) -> Unit
) : ListAdapter<Goal, GoalsAdapter.GoalViewHolder>(DiffCallback) {

    private object DiffCallback : DiffUtil.ItemCallback<Goal>() {
        override fun areItemsTheSame(old: Goal, new: Goal) =
            old.id == new.id
        override fun areContentsTheSame(old: Goal, new: Goal) =
            old == new
    }

    class GoalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val llHeader: View = itemView.findViewById(R.id.ll_goal_header)
        val tvPriority: TextView = itemView.findViewById(R.id.tv_priority_badge)
        val tvDescription: TextView = itemView.findViewById(R.id.tv_goal_description)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoalViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.goal_item, parent, false)
        return GoalViewHolder(view)
    }

    override fun onBindViewHolder(holder: GoalViewHolder, position: Int) {
        val goal = getItem(position)

        // Priority badge:
        holder.tvPriority.text = goal.priority.toString()
        applyPriorityBadgeStyle(holder.tvPriority, goal.priority)
        // - tap cycles through 1 → 2 → 3 → 1
        holder.tvPriority.setOnClickListener { onCyclePriority(goal.id) }

        // Description:
        holder.tvDescription.text = goal.description

        // Goal:
        holder.llHeader.setOnClickListener { onGoalTap(goal.id) }
        holder.llHeader.setOnLongClickListener {
            onGoalLongPress(goal.id)
            true
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
}