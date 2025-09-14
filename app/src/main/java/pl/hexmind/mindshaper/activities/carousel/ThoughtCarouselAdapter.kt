package pl.hexmind.mindshaper.ui.carousel

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.services.dto.ThoughtDTO

/**
 * Adapter for thought carousel with smooth animations and automatic updates via LiveData
 */
class ThoughtCarouselAdapter : ListAdapter<ThoughtDTO, ThoughtCarouselAdapter.ThoughtViewHolder>(ThoughtDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThoughtViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_thought_carousel, parent, false)
        return ThoughtViewHolder(view)
    }

    override fun onBindViewHolder(holder: ThoughtViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * ViewHolder for individual thought items with optimized binding logic
     */
    class ThoughtViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val tvThread: TextView = itemView.findViewById(R.id.tv_thought_thread)

        private val tvEssence: TextView = itemView.findViewById(R.id.tv_thought_essence)

        private val tvCreatedAt: TextView = itemView.findViewById(R.id.tv_created_at)

        private val ivIcon: ImageView = itemView.findViewById(R.id.iv_domain_icon)

        /**
         * Bind thought data to view components with null safety
         */
        fun bind(thought: ThoughtDTO) {
            when {
                thought.thread.isNullOrBlank() -> {
                    tvThread.text = itemView.context.getString(R.string.carousel_thought_thread_empty)
                }
                else -> {
                    tvThread.text = thought.thread
                }
            }

            tvEssence.text = thought.essence
            tvCreatedAt.text = thought.createdAt.toString()
            //ivIcon.setImageResource(-1) // TODO
        }
    }

    /**
     * Optimized DiffCallback for efficient list updates with proper comparison
     */
    private class ThoughtDiffCallback : DiffUtil.ItemCallback<ThoughtDTO>() {
        override fun areItemsTheSame(oldItem: ThoughtDTO, newItem: ThoughtDTO): Boolean {
            // Compare by ID for item identity
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ThoughtDTO, newItem: ThoughtDTO): Boolean {
            // Compare full objects for content changes - triggers smooth animations
            return oldItem == newItem
        }
    }
}