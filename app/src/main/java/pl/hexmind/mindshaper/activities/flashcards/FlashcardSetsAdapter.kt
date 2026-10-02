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
import pl.hexmind.mindshaper.services.FlashcardsSession
import pl.hexmind.mindshaper.services.dto.FlashcardSetDTO

class FlashcardSetsAdapter(
    private val onSetTap: (FlashcardSetDTO) -> Unit,
    private val onSetLongPress: (FlashcardSetDTO) -> Unit,
    private val onReviewTap: (FlashcardSetDTO) -> Unit
) : ListAdapter<FlashcardSetDTO, FlashcardSetsAdapter.SetViewHolder>(SetDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SetViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.flashcards_set_item, parent, false)
        return SetViewHolder(view)
    }

    override fun onBindViewHolder(holder: SetViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val tvName: TextView = itemView.findViewById(R.id.tv_set_name)
        private val tvCount: TextView = itemView.findViewById(R.id.tv_set_count)
        private val tvStatus: TextView = itemView.findViewById(R.id.tv_set_status)
        private val btnReview: MaterialButton = itemView.findViewById(R.id.btn_set_review)

        fun bind(set: FlashcardSetDTO) {
            val context = itemView.context
            val flashcards = set.flashcards

            tvName.text = set.name
            tvCount.text = context.resources.getQuantityString(R.plurals.flashcards_count, flashcards.size, flashcards.size)

            // Paused session first - otherwise the result of the last one (none yet = nothing to show)
            val status = when {
                FlashcardsSession.isInProgress(flashcards) -> {
                    val left = FlashcardsSession.sessionSize(flashcards) - FlashcardsSession.answeredCount(flashcards)
                    context.getString(R.string.flashcards_session_paused, left)
                }
                else -> FlashcardsSession.masteryPercent(flashcards)?.let { mastery ->
                    context.getString(R.string.flashcards_mastery, mastery)
                }
            }
            tvStatus.visibility = if (status != null) View.VISIBLE else View.GONE
            tvStatus.text = status

            // Nothing to review in an empty set - INVISIBLE keeps the card height
            btnReview.visibility = if (flashcards.isNotEmpty()) View.VISIBLE else View.INVISIBLE

            itemView.setOnClickListener { onSetTap(set) }
            itemView.setOnLongClickListener {
                onSetLongPress(set)
                true
            }
            btnReview.setOnClickListener { onReviewTap(set) }
        }
    }

    private class SetDiffCallback : DiffUtil.ItemCallback<FlashcardSetDTO>() {
        override fun areItemsTheSame(oldItem: FlashcardSetDTO, newItem: FlashcardSetDTO): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: FlashcardSetDTO, newItem: FlashcardSetDTO): Boolean =
            oldItem == newItem
    }
}
