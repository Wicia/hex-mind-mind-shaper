package pl.hexmind.mindshaper.activities.carousel

import android.content.Intent
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.activities.details.ThoughtDetailsActivity
import pl.hexmind.mindshaper.common.formatting.toLocalDateString
import pl.hexmind.mindshaper.services.dto.ThoughtDTO
import timber.log.Timber

/**
 * Adapter for thought carousel with smooth animations and automatic updates via LiveData
 */
class ThoughtCarouselAdapter(
    private val onDeleteThought: (ThoughtDTO) -> Unit // TODO: Added callback for deletion
) : ListAdapter<ThoughtDTO, ThoughtCarouselAdapter.ThoughtViewHolder>(ThoughtDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThoughtViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_thought_carousel, parent, false)
        return ThoughtViewHolder(view, onDeleteThought)
    }

    override fun onBindViewHolder(holder: ThoughtViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * ViewHolder for individual thought items with optimized binding logic
     */
    class ThoughtViewHolder(
        itemView: View,
        private val onDeleteThought: (ThoughtDTO) -> Unit
    ) : RecyclerView.ViewHolder(itemView),
        GestureDetector.OnGestureListener,
        GestureDetector.OnDoubleTapListener {

        private val tvThread: TextView = itemView.findViewById(R.id.tv_thought_thread)
        private val tvCreatedAt: TextView = itemView.findViewById(R.id.tv_created_at)
        private val ivDomainIcon: ImageView = itemView.findViewById(R.id.iv_domain_icon)

        private val tvRichText: TextView = itemView.findViewById(R.id.tv_rich_text)

        private var viewedThoughtDTO : ThoughtDTO? = null

        private val gestureDetector = GestureDetector(itemView.context, this).apply {
            setOnDoubleTapListener(this@ThoughtViewHolder)
        }

        /**
         * Bind thought data to view components with null safety
         */
        fun bind(thought: ThoughtDTO) {
            viewedThoughtDTO = thought
            setViewOnTouchListener()

            //TODO: Extend by checking thought initial type
            tvRichText.text = thought.richText

            when {
                thought.thread.isNullOrBlank() -> {
                    tvThread.text = itemView.context.getString(R.string.carousel_thought_thread_empty)
                }
                else -> {
                    tvThread.text = thought.thread
                }
            }

            tvCreatedAt.text = thought.createdAt?.toLocalDateString()

            if(thought.thread.isNullOrBlank()){
                ivDomainIcon.setImageResource(R.drawable.ic_domain_none)
            }
        }

        fun setViewOnTouchListener(){
            itemView.setOnTouchListener { _, event ->
                gestureDetector.onTouchEvent(event).also { handled ->
                    if (event.action == MotionEvent.ACTION_UP && handled) itemView.performClick()
                }
            }
        }

        // === Gesture detection ===

        override fun onDown(e: MotionEvent): Boolean = true

        override fun onShowPress(e: MotionEvent) {}

        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            return false
        }

        /**
         * Handles long press gesture
         */
        override fun onLongPress(e: MotionEvent) {
            viewedThoughtDTO?.let { thought ->
                showDeleteConfirmationDialog(thought)
            } ?: run {
                Timber.w("Long press detected but no thought data available")
            }
        }

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            TODO("Not yet implemented")
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            val intent = Intent(itemView.context, ThoughtDetailsActivity::class.java)
            intent.putExtra(ThoughtDetailsActivity.P_SELECTED_THOUGHT_ID, viewedThoughtDTO)
            itemView.context.startActivity(intent)
            return true
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            TODO("Not yet implemented")
        }

        override fun onDoubleTapEvent(e: MotionEvent): Boolean {
            TODO("Not yet implemented")
        }

        private fun showDeleteConfirmationDialog(thought: ThoughtDTO) {
            MaterialAlertDialogBuilder(itemView.context)
                .setTitle(itemView.context.getString(R.string.common_deletion_dialog_title))
                .setMessage(itemView.context.getString(R.string.common_deletion_dialog_message, "myśl"))
                .setPositiveButton(itemView.context.getString(R.string.common_deletion_dialog_yes)) { dialog, _ ->
                    onDeleteThought(thought)
                    Timber.d("Thought deleted: ${thought.id}")
                    dialog.dismiss()
                }
                .setNegativeButton(itemView.context.getString(R.string.common_deletion_dialog_no)) { dialog, _ ->
                    Timber.d("Deletion cancelled")
                    dialog.dismiss()
                }
                .show()
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