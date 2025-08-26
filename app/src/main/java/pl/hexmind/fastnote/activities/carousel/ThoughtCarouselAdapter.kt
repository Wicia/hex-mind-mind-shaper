package pl.hexmind.fastnote.activities.carousel

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import pl.hexmind.fastnote.R

/**
 * Adapter for thought carousel with smooth animations and different thought types
 */
class ThoughtCarouselAdapter : ListAdapter<ThoughtItem, ThoughtCarouselAdapter.ThoughtViewHolder>(ThoughtDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThoughtViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_thought_carousel, parent, false)
        return ThoughtViewHolder(view)
    }

    override fun onBindViewHolder(holder: ThoughtViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * ViewHolder for individual thought items with binding logic
     */
    class ThoughtViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val titleText: TextView = itemView.findViewById(R.id.tv_thought_title)
        private val contentText: TextView = itemView.findViewById(R.id.tv_thought_content)

        /**
         * Bind thought data to view components
         */
        fun bind(thought: ThoughtItem) {
            titleText.text = thought.tags
            contentText.text = thought.content
        }
    }

    /**
     * DiffCallback for efficient list updates
     */
    private class ThoughtDiffCallback : DiffUtil.ItemCallback<ThoughtItem>() {
        override fun areItemsTheSame(oldItem: ThoughtItem, newItem: ThoughtItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ThoughtItem, newItem: ThoughtItem): Boolean {
            return oldItem == newItem
        }
    }
}