package pl.hexmind.mindshaper.activities.carousel

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
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.common.formatting.toLocalDateString
import pl.hexmind.mindshaper.common.ui.HexTextView
import pl.hexmind.mindshaper.common.ui.ValueBar
import pl.hexmind.mindshaper.services.dto.ThoughtDTO
import pl.hexmind.mindshaper.services.validators.ThoughtValidator
import timber.log.Timber

/**
 * Adapter for thought carousel with smooth animations and automatic updates via LiveData
 */
class CarouselAdapter(
    private val onDeleteThought: (ThoughtDTO) -> Unit,
    private val onThoughtTap: (ThoughtDTO) -> Unit
) : ListAdapter<ThoughtDTO, CarouselAdapter.ThoughtViewHolder>(ThoughtDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThoughtViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.carousel_item, parent, false)
        return ThoughtViewHolder(view, onDeleteThought, onThoughtTap)
    }

    override fun onBindViewHolder(holder: ThoughtViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * ViewHolder for individual thought items with optimized binding logic
     */
    class ThoughtViewHolder(
        itemView: View,
        private val onDeleteThought: (ThoughtDTO) -> Unit,
        private val onThoughtTap: (ThoughtDTO) -> Unit
    ) : RecyclerView.ViewHolder(itemView),
        GestureDetector.OnGestureListener,
        GestureDetector.OnDoubleTapListener {

        private val tvMetadata: TextView = itemView.findViewById(R.id.tv_though_metadata)
        private val tvCreatedAt: TextView = itemView.findViewById(R.id.tv_created_at)
        private val ivDomainIcon: ImageView = itemView.findViewById(R.id.iv_domain_icon)

        private val tvRichText: HexTextView = itemView.findViewById(R.id.tv_rich_text)

        private val vbThoughtValue: ValueBar = itemView.findViewById(R.id.vb_thought_value)

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
            tvRichText.originalText = thought.richText.orEmpty()

            fillMetadataUI(thought)

            tvCreatedAt.text = thought.createdAt.toLocalDateString()

            // TODO: Load icons = refactoring :)
            ivDomainIcon.setImageResource(R.drawable.ic_domain_none)

            vbThoughtValue.maxLevel = ThoughtValidator.THOUGHT_VALUE_MAX
            vbThoughtValue.currentLevel = thought.value
        }

        fun fillMetadataUI(thought: ThoughtDTO){
            if(thought.thread.isNullOrBlank() && thought.project.isNullOrBlank()){
                tvMetadata.text = itemView.context.getString(R.string.carousel_thought_thread_empty)
            }
            else if(thought.thread.isNullOrBlank() && !thought.project.isNullOrBlank()){
                tvMetadata.text = thought.project
            }
            else{
                tvMetadata.text = thought.thread
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
                onDeleteThought(thought)
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
            viewedThoughtDTO?.let { thought ->
                onThoughtTap(thought)
            }
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