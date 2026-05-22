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
 * Adapter for the guidelines list in GoalDetailActivity.
 * #! Plain RecyclerView.Adapter — keeps things simple for small guidelines list to avoid
 * async ListAdapter/DiffUtil mechanism conflicts.
 */
class GoalDetailGuidelinesAdapter(
    private val onTapText: (GoalGuideline) -> Unit,
    private val onLongPressText: (guidelineId: Int) -> Unit,
    private val onTapRing: (guidelineId: Int) -> Unit,
    private val onLongPressRing: (guidelineId: Int) -> Unit,
    private val onMenuClick: (anchor: View, guideline: GoalGuideline, isFirst: Boolean, isLast: Boolean) -> Unit,
    private val onThoughtChipClick: (thoughtId: Int) -> Unit,
    private val onThoughtChipLongPress: (guidelineId: Int) -> Unit
) : RecyclerView.Adapter<GoalDetailGuidelinesAdapter.ViewHolder>() {

    private val items = mutableListOf<GoalGuideline>()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val flRing: FrameLayout                = itemView.findViewById(R.id.fl_guideline_ring)
        val cpiRing: CircularProgressIndicator = itemView.findViewById(R.id.cpi_guideline_ring)
        val tvRingLabel: TextView              = itemView.findViewById(R.id.tv_ring_label)
        val tvDesc: TextView                   = itemView.findViewById(R.id.tv_guideline_description)
        val ivMore: ImageView                  = itemView.findViewById(R.id.iv_guideline_more)
        val btnLinkedThought: MaterialButton   = itemView.findViewById(R.id.btn_linked_thought)
    }

    fun setItems(list: List<GoalGuideline>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.goal_detail_guideline_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val guideline = items[position]
        val isFirst = position == 0
        val isLast = position == items.size - 1

        bindRing(holder, guideline)
        bindDescription(holder, guideline)
        bindMenu(holder, guideline, isFirst, isLast)
        bindLinkedThought(holder, guideline)
    }

    // ── Bindings ───────────────────────────────────────────────────────────────

    private fun bindRing(holder: ViewHolder, guideline: GoalGuideline) {
        val context = holder.cpiRing.context
        val done = guideline.isCompleted

        val progress = when {
            guideline.maxRepetitions == 0 -> 0
            else -> (guideline.currentRepetitions * 100 / guideline.maxRepetitions).coerceIn(0, 100)
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
            guideline.maxRepetitions > 1 -> guideline.currentRepetitions.toString()
            else                         -> ""  // single checkbox — no label when empty
        }

        // Ring -> Tap
        holder.flRing.setOnClickListener { onTapRing(guideline.id) }

        // Ring -> Long press
        holder.flRing.setOnLongClickListener {
            onLongPressRing(guideline.id)
            true
        }
    }

    private fun bindDescription(holder: ViewHolder, guideline: GoalGuideline) {
        holder.tvDesc.text = guideline.description
        holder.tvDesc.setOnClickListener { onTapText(guideline) }
        holder.tvDesc.setOnLongClickListener {
            onLongPressText(guideline.id)
            true
        }
    }

    private fun bindMenu(holder: ViewHolder, guideline: GoalGuideline, isFirst: Boolean, isLast: Boolean) {
        holder.ivMore.setOnClickListener {
            onMenuClick(holder.ivMore, guideline, isFirst, isLast)
        }
    }

    private fun bindLinkedThought(holder: ViewHolder, guideline: GoalGuideline) {
        if (!guideline.hasLinkedThought) {
            holder.btnLinkedThought.visibility = View.GONE
            return
        }
        holder.btnLinkedThought.visibility = View.VISIBLE

        // Fallback when thread is empty/null
        holder.btnLinkedThought.text = guideline.thoughtThread
            ?.takeIf { it.isNotBlank() }
            ?: holder.btnLinkedThought.context.getString(R.string.workshop_guideline_linked_thought_fallback)

        holder.btnLinkedThought.setOnClickListener {
            guideline.thoughtId?.let { onThoughtChipClick(it) }
        }
        holder.btnLinkedThought.setOnLongClickListener {
            onThoughtChipLongPress(guideline.id)
            true
        }
    }
}
