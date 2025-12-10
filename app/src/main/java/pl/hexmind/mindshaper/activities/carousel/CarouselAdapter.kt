package pl.hexmind.mindshaper.activities.carousel

import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.RenderProcessGoneDetail
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.Visibility
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.activities.ThoughtGrowthStage
import pl.hexmind.mindshaper.activities.capture.handlers.AudioRecordingView
import pl.hexmind.mindshaper.activities.capture.models.ThoughtMainContentType
import pl.hexmind.mindshaper.activities.capture.models.ThoughtMainContentType.*
import pl.hexmind.mindshaper.common.formatting.toLocalDateString
import pl.hexmind.mindshaper.common.ui.HexTextView
import pl.hexmind.mindshaper.common.ui.ValueBar
import pl.hexmind.mindshaper.services.dto.ThoughtDTO
import pl.hexmind.mindshaper.services.validators.ThoughtValidator
import timber.log.Timber
import java.io.File

/**
 * Adapter for thought carousel with smooth animations and automatic updates via LiveData
 */
class CarouselAdapter(
    private val onDeleteThought: (ThoughtDTO) -> Unit,
    private val onThoughtTap: (ThoughtDTO) -> Unit,
    private val onLoadAudio: (thoughtId: Int, onReady: (File) -> Unit) -> Unit
) : ListAdapter<ThoughtDTO, CarouselAdapter.ThoughtViewHolder>(ThoughtDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThoughtViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.carousel_item, parent, false)
        return ThoughtViewHolder(view, onDeleteThought, onThoughtTap, onLoadAudio)
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
        private val onThoughtTap: (ThoughtDTO) -> Unit,
        private val onLoadAudio: (thoughtId: Int, onReady: (File) -> Unit) -> Unit
    ) : RecyclerView.ViewHolder(itemView),
        GestureDetector.OnGestureListener,
        GestureDetector.OnDoubleTapListener {

        private var currentAudioFile: File? = null

        private val tvMetadata: TextView = itemView.findViewById(R.id.tv_though_metadata)
        private val tvCreatedAt: TextView = itemView.findViewById(R.id.tv_created_at)
        private val ivDomainIcon: ImageView = itemView.findViewById(R.id.iv_domain_icon)

        private val tvRichText: HexTextView = itemView.findViewById(R.id.tv_rich_text)

        private val audioView : AudioRecordingView = itemView.findViewById(R.id.arv_playback)

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

            when (thought.mainContentType) {
                RECORDING -> {
                    audioView.visibility = View.VISIBLE
                    audioView.setMode(AudioRecordingView.Mode.PLAYBACK_ONLY)

                    thought.id?.let { thoughtId ->
                        onLoadAudio(thoughtId) { audioFile ->
                            currentAudioFile = audioFile
                            audioView.loadAudioForPlayback(audioFile)
                        }
                    }
                }
                RICH_TEXT -> {
                    tvRichText.visibility = View.VISIBLE
                    tvRichText.originalText = thought.richText.orEmpty()
                }
                UNKNOWN -> { /* TODO */ }
                PHOTO -> { /* TODO */ }
                DRAWING -> { /* TODO */ }
            }

            tvMetadata.text = "\\ " + getFormattedMetadataUI(thought) + " /"

            val ageLevel = ThoughtGrowthStage.newThoughtGrowthStage(thought.createdAt)
            val levelIcon = ageLevel.level.icon
            val levelName = itemView.context.getString(ageLevel.level.labelResourceId)
            val afeInDays = ageLevel.ageInDays.toString()

            tvCreatedAt.text = itemView.context.getString(
                R.string.common_thought_age_pattern, levelIcon, levelName, afeInDays
            )

            // TODO: Load icons = refactoring :)
            // ivDomainIcon.setImageResource(R.drawable.ic_domain_none)
            ivDomainIcon.visibility = View.GONE

            vbThoughtValue.maxLevel = ThoughtValidator.THOUGHT_VALUE_MAX
            vbThoughtValue.currentLevel = thought.value
        }

        fun getFormattedMetadataUI(thought: ThoughtDTO) : String{
            if(thought.thread.isNullOrBlank() && thought.project.isNullOrBlank()){
                return itemView.context.getString(R.string.carousel_thought_thread_empty)
            }
            else if(thought.thread.isNullOrBlank() && !thought.project.isNullOrBlank()){
                return thought.project.orEmpty()
            }
            else{
                return thought.thread.orEmpty()
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