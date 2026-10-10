package pl.hexmind.mindshaper.activities.flashcards

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.services.FlashcardsReview
import pl.hexmind.mindshaper.services.dto.FlashcardSetDTO

/**
 * Set on the flashcards screen list + what the Repetitions System says about it right now.
 * ! Counts depend on the time - kept in the item, so a refresh after a while rebinds what changed
 */
data class FlashcardSetItem(
    val set: FlashcardSetDTO,
    val stats: FlashcardsReview.SetStats,
    val canReview: Boolean // Something in the set's queue right now
)

class FlashcardSetsAdapter(
    private val onSetTap: (FlashcardSetDTO) -> Unit,
    private val onSetLongPress: (FlashcardSetDTO) -> Unit,
    private val onReviewTap: (FlashcardSetDTO) -> Unit
) : ListAdapter<FlashcardSetItem, FlashcardSetsAdapter.SetViewHolder>(SetDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SetViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.flashcards_set_item, parent, false)
        return SetViewHolder(view)
    }

    override fun onBindViewHolder(holder: SetViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val tvName: TextView = itemView.findViewById(R.id.tv_set_name)
        private val tvStatus: TextView = itemView.findViewById(R.id.tv_set_status)
        private val btnReview: MaterialButton = itemView.findViewById(R.id.btn_set_review)

        fun bind(item: FlashcardSetItem) {
            val context = itemView.context
            val set = item.set

            tvName.text = context.getString(R.string.flashcards_set_name_with_count, set.name, set.flashcards.size)

            // Empty set - nothing to count
            tvStatus.visibility = if (set.flashcards.isNotEmpty()) View.VISIBLE else View.GONE
            tvStatus.text = context.getString(
                R.string.flashcards_set_stats,
                item.stats.dueCount,
                item.stats.newCount,
                item.stats.masteredCount
            )

            // Nothing to review right now - INVISIBLE keeps the card height
            btnReview.visibility = if (item.canReview) View.VISIBLE else View.INVISIBLE

            itemView.setOnClickListener { onSetTap(set) }
            itemView.setOnLongClickListener {
                onSetLongPress(set)
                true
            }
            btnReview.setOnClickListener { onReviewTap(set) }
        }
    }

    private class SetDiffCallback : DiffUtil.ItemCallback<FlashcardSetItem>() {
        override fun areItemsTheSame(oldItem: FlashcardSetItem, newItem: FlashcardSetItem): Boolean =
            oldItem.set.id == newItem.set.id

        override fun areContentsTheSame(oldItem: FlashcardSetItem, newItem: FlashcardSetItem): Boolean =
            oldItem == newItem
    }
}
