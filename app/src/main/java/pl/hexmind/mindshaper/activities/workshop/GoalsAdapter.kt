package pl.hexmind.mindshaper.activities.workshop

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.common.ui.dpToPx
import pl.hexmind.mindshaper.common.ui.views.GoalImportanceBadge

/**
 * Adapter for the goals list in WorkshopActivity.
 */
class GoalsAdapter(
    private val onGoalTap: (goalId: Int) -> Unit,
    private val onCycleImportance: (goalId: Int) -> Unit,
    private val onGoalLongPress: (goalId: Int) -> Unit
) : ListAdapter<Goal, GoalsAdapter.GoalViewHolder>(DiffCallback) {

    private object DiffCallback : DiffUtil.ItemCallback<Goal>() {
        override fun areItemsTheSame(old: Goal, new: Goal) =
            old.id == new.id
        override fun areContentsTheSame(old: Goal, new: Goal) =
            old == new
    }

    class GoalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val llHeader:      View     = itemView.findViewById(R.id.ll_goal_header)

        val badge: GoalImportanceBadge = itemView.findViewById(R.id.tv_importance_badge)
        val tvDescription: TextView = itemView.findViewById(R.id.tv_goal_description)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoalViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.goal_item, parent, false)
        return GoalViewHolder(view)
    }

    override fun onBindViewHolder(holder: GoalViewHolder, position: Int) {
        val goal = getItem(position)

        // importance == 0 marks an archived goal (empty badge/smaller)
        val archived = goal.importance <= 0
        val badgeSizePx = holder.badge.context.dpToPx(if (archived) 24 else 32)
        holder.badge.layoutParams = holder.badge.layoutParams.also {
            it.width = badgeSizePx
            it.height = badgeSizePx
        }

        holder.badge.setImportance(goal.importance)
        if (archived) {
            holder.badge.setOnClickListener(null)
            holder.badge.isClickable = false
        }
        else {
            holder.badge.setOnClickListener { onCycleImportance(goal.id) }
        }

        // Description:
        holder.tvDescription.text = goal.description

        // Goal:
        holder.llHeader.setOnClickListener { onGoalTap(goal.id) }
        holder.llHeader.setOnLongClickListener {
            onGoalLongPress(goal.id)
            true
        }
    }
}
