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
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.activities.details.ThoughtDetailsActivity
import pl.hexmind.mindshaper.common.formatting.toLocalDateString
import pl.hexmind.mindshaper.services.dto.ThoughtDTO

/**
 * Adapter for thought carousel with smooth animations and automatic updates via LiveData
 */
class ThoughtCarouselAdapter
    : ListAdapter<ThoughtDTO, ThoughtCarouselAdapter.ThoughtViewHolder>(ThoughtDiffCallback()) {

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
    class ThoughtViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        GestureDetector.OnGestureListener,
        GestureDetector.OnDoubleTapListener {

        private val tvThread: TextView = itemView.findViewById(R.id.tv_thought_thread)
        private val tvEssence: TextView = itemView.findViewById(R.id.tv_thought_essence)
        private val tvCreatedAt: TextView = itemView.findViewById(R.id.tv_created_at)
        private val ivDomainIcon: ImageView = itemView.findViewById(R.id.iv_domain_icon)

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

            when {
                thought.thread.isBlank() -> {
                    tvThread.text = itemView.context.getString(R.string.carousel_thought_thread_empty)
                }
                else -> {
                    tvThread.text = thought.thread
                }
            }

            tvEssence.text = thought.essence
            tvCreatedAt.text = thought.createdAt?.toLocalDateString()

            if(thought.thread.isBlank()){
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
            TODO("Not yet implemented")
        }

        override fun onLongPress(e: MotionEvent) {
            TODO("Not yet implemented")
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