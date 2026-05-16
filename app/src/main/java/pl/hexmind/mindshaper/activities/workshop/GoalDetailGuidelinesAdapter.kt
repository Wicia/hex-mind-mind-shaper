package pl.hexmind.mindshaper.activities.workshop

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.CircularProgressIndicator
import pl.hexmind.mindshaper.R

/**
 * Adapter for the guidelines list in GoalDetailActivity.
 * ! Plain RecyclerView.Adapter (no ListAdapter/DiffUtil) — avoids async diff conflicts with drag.
 */
class GoalDetailGuidelinesAdapter(
    private val onTapText: (GoalGuideline) -> Unit,
    private val onLongPressText: (guidelineId: Int) -> Unit,
    private val onTapRing: (guidelineId: Int) -> Unit,
    private val onLongPressRing: (guidelineId: Int) -> Unit,
    private val onStartDrag: (holder: RecyclerView.ViewHolder) -> Unit
) : RecyclerView.Adapter<GoalDetailGuidelinesAdapter.ViewHolder>() {

    private val items = mutableListOf<GoalGuideline>()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val flRing: FrameLayout                = itemView.findViewById(R.id.fl_guideline_ring)
        val cpiRing: CircularProgressIndicator = itemView.findViewById(R.id.cpi_guideline_ring)
        val tvRingLabel: TextView              = itemView.findViewById(R.id.tv_ring_label)
        val tvDesc: TextView                   = itemView.findViewById(R.id.tv_guideline_description)
        val ivDrag: ImageView                  = itemView.findViewById(R.id.iv_guideline_drag_handle)
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

        bindRing(holder, guideline)
        holder.flRing.setOnClickListener { onTapRing(guideline.id) }
        holder.flRing.setOnLongClickListener {
            onLongPressRing(guideline.id)
            true
        }

        holder.tvDesc.text = guideline.description
        applyCompletedStyle(holder.tvDesc, guideline.isCompleted)
        holder.tvDesc.setOnClickListener { onTapText(guideline) }
        holder.tvDesc.setOnLongClickListener {
            onLongPressText(guideline.id)
            true
        }

        holder.ivDrag.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) onStartDrag(holder)
            false
        }
    }

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
    }

    private fun applyCompletedStyle(tv: TextView, isCompleted: Boolean) {
        if (isCompleted) {
            tv.setTextColor(ContextCompat.getColor(tv.context, R.color._gray_lvl_3))
        }
        else {
            tv.setTextColor(ContextCompat.getColor(tv.context, R.color.text_primary))
        }
    }
}
