package pl.hexmind.mindshaper.activities.workshop

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import pl.hexmind.mindshaper.R

/**
 * Adapter for the steps list in GoalDetailActivity.
 * #! Plain RecyclerView.Adapter — keeps things simple for small steps list to avoid
 * async ListAdapter/DiffUtil mechanism conflicts.
 */
class GoalDetailStepsAdapter(
    private val onTapText: (GoalStep) -> Unit,
    private val onLongPressText: (stepId: Int) -> Unit,
    private val onTapRing: (stepId: Int) -> Unit,
    private val onLongPressRing: (stepId: Int) -> Unit,
    private val onMenuClick: (anchor: View, step: GoalStep, isFirst: Boolean, isLast: Boolean) -> Unit,
    private val onThoughtChipClick: (thoughtId: Int) -> Unit,
    private val onThoughtChipLongPress: (stepId: Int) -> Unit
) : RecyclerView.Adapter<GoalDetailStepsAdapter.ViewHolder>() {

    private val items = mutableListOf<GoalStep>()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val flRing: FrameLayout                = itemView.findViewById(R.id.fl_step_ring)
        val cpiRing: CircularProgressIndicator = itemView.findViewById(R.id.cpi_step_ring)
        val tvRingLabel: TextView              = itemView.findViewById(R.id.tv_ring_label)
        val tvDesc: TextView                   = itemView.findViewById(R.id.tv_step_description)
        val ivMore: ImageView                  = itemView.findViewById(R.id.iv_step_more)
        val btnLinkedThought: MaterialButton   = itemView.findViewById(R.id.btn_linked_thought)
    }

    fun setItems(list: List<GoalStep>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.goal_detail_step_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val step = items[position]
        val isFirst = position == 0
        val isLast = position == items.size - 1

        bindRing(holder, step)
        bindDescription(holder, step)
        bindMenu(holder, step, isFirst, isLast)
        bindLinkedThought(holder, step)
    }

    // ── Bindings ───────────────────────────────────────────────────────────────

    private fun bindRing(holder: ViewHolder, step: GoalStep) {
        val context = holder.cpiRing.context
        val done = step.isCompleted

        val progress = when {
            step.maxRepetitions == 0 -> 0
            else -> (step.currentRepetitions * 100 / step.maxRepetitions).coerceIn(0, 100)
        }
        // Disable animation on rebind to avoid flicker during scroll / setItems
        holder.cpiRing.setProgressCompat(progress, false)

        val indicatorColor = ContextCompat.getColor(
            context, if (done) R.color.importance_low else R.color._orange_lvl_3
        )
        val trackColor = ContextCompat.getColor(
            context, if (done) R.color.importance_low else R.color._orange_lvl_1
        )
        holder.cpiRing.setIndicatorColor(indicatorColor)
        holder.cpiRing.trackColor = trackColor

        holder.tvRingLabel.setTextColor(
            if (done) ContextCompat.getColor(context, R.color.importance_low)
            else ContextCompat.getColor(context, R.color._orange_lvl_3)
        )
        holder.tvRingLabel.text = when {
            done                         -> "✓"
            step.maxRepetitions > 1 -> step.currentRepetitions.toString()
            else                         -> ""  // single checkbox — no label when empty
        }

        // Ring -> Tap
        holder.flRing.setOnClickListener { onTapRing(step.id) }

        // Ring -> Long press
        holder.flRing.setOnLongClickListener {
            onLongPressRing(step.id)
            true
        }
    }

    private fun bindDescription(holder: ViewHolder, step: GoalStep) {
        holder.tvDesc.text = step.description
        holder.tvDesc.setOnClickListener { onTapText(step) }
        holder.tvDesc.setOnLongClickListener {
            onLongPressText(step.id)
            true
        }
    }

    private fun bindMenu(holder: ViewHolder, step: GoalStep, isFirst: Boolean, isLast: Boolean) {
        holder.ivMore.setOnClickListener {
            onMenuClick(holder.ivMore, step, isFirst, isLast)
        }
    }

    private fun bindLinkedThought(holder: ViewHolder, step: GoalStep) {
        if (!step.hasLinkedThought) {
            holder.btnLinkedThought.visibility = View.GONE
            return
        }
        holder.btnLinkedThought.visibility = View.VISIBLE

        // Fallback when subject is empty/null
        holder.btnLinkedThought.text = step.thoughtSubject
            ?.takeIf { it.isNotBlank() }
            ?: holder.btnLinkedThought.context.getString(R.string.workshop_step_linked_thought_fallback)

        holder.btnLinkedThought.setOnClickListener {
            step.thoughtId?.let { onThoughtChipClick(it) }
        }
        holder.btnLinkedThought.setOnLongClickListener {
            onThoughtChipLongPress(step.id)
            true
        }
    }
}
