package pl.hexmind.mindshaper.activities.stream

import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.activities.ThoughtGrowthStage
import pl.hexmind.mindshaper.common.ui.views.content.AudioRecordingView
import pl.hexmind.mindshaper.common.ui.views.lists.SortConfig
import pl.hexmind.mindshaper.common.ui.views.lists.SortProperty
import pl.hexmind.mindshaper.common.ui.views.content.HexPhotoView
import pl.hexmind.mindshaper.common.ui.views.content.HexTextView
import pl.hexmind.mindshaper.common.ui.views.values.ValueCloude
import pl.hexmind.mindshaper.services.AppSettingsStorage
import pl.hexmind.mindshaper.services.dto.ThoughtDTO
import timber.log.Timber
import java.io.File

/**
 * Adapter for vertical stream - shows ALL filled fields (text, audio, photo) in one card
 */
class StreamAdapter(
    private val appSettingsStorage: AppSettingsStorage,
    private val onDeleteThought: (ThoughtDTO) -> Unit,
    private val onThoughtTap: (ThoughtDTO) -> Unit,
    private val onLoadAudio: (thoughtId: Int, onReady: (File) -> Unit) -> Unit,
    private val onLoadPhoto: (thoughtId: Int, onReady: (ByteArray) -> Unit) -> Unit
) : ListAdapter<ThoughtDTO, StreamAdapter.ThoughtViewHolder>(ThoughtDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThoughtViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.stream_item, parent, false)
        return ThoughtViewHolder(
            view,
            appSettingsStorage,
            onDeleteThought,
            onThoughtTap,
            onLoadAudio,
            onLoadPhoto
        )
    }

    private var currentSortConfig : SortConfig = SortConfig()

    fun updateSortConfig(config: SortConfig) {
        currentSortConfig = config
        // Notify that all items need rebinding due to sortConfig change
        notifyItemRangeChanged(0, itemCount)
    }

    override fun onBindViewHolder(holder: ThoughtViewHolder, position: Int) {
        holder.bind(getItem(position), currentSortConfig)
    }

    /**
     * ViewHolder for stream items - shows ALL filled fields
     */
    class ThoughtViewHolder(
        itemView: View,
        private val appSettingsStorage: AppSettingsStorage,
        private val onDeleteThought: (ThoughtDTO) -> Unit,
        private val onThoughtTap: (ThoughtDTO) -> Unit,
        private val onLoadAudio: (thoughtId: Int, onReady: (File) -> Unit) -> Unit,
        private val onLoadPhoto: (thoughtId: Int, onReady: (ByteArray) -> Unit) -> Unit
    ) : RecyclerView.ViewHolder(itemView),
        GestureDetector.OnGestureListener,
        GestureDetector.OnDoubleTapListener {

        private var currentAudioFile: File? = null

        private val nestedScrollView: NestedScrollView = itemView.findViewById(R.id.nested_scroll_view)
        private val tvRichText: HexTextView = itemView.findViewById(R.id.rich_text_view)
        private val audioView: AudioRecordingView = itemView.findViewById(R.id.arv_playback)
        private val photoView: HexPhotoView = itemView.findViewById(R.id.pv_photo)

        private val tvLabel: TextView = itemView.findViewById(R.id.tv_label)
        private val tvThreadLabel: TextView = itemView.findViewById(R.id.tv_thread_label)

        private val tvTextIcon: TextView = itemView.findViewById(R.id.tv_text_icon)

        private val vbThoughtValue: ValueCloude = itemView.findViewById(R.id.vb_thought_value)

        private val ivDecoratorIcon: ImageView = itemView.findViewById(R.id.iv_decorator_icon)

        private val tvEmptyThought: TextView = itemView.findViewById(R.id.tv_empty_thought)

        // Data
        private var viewedThoughtDTO: ThoughtDTO? = null

        private val gestureDetector = GestureDetector(itemView.context, this).apply {
            setOnDoubleTapListener(this@ThoughtViewHolder)
        }

        /**
         * Bind thought data - show ALL filled fields (text, audio, photo)
         */
        fun bind(thought: ThoughtDTO, sortConfig: SortConfig) {
            viewedThoughtDTO = thought
            setViewOnTouchListener()

            // Reset all fields to GONE first
            tvRichText.visibility = View.GONE
            audioView.visibility = View.GONE
            photoView.visibility = View.GONE
            // extra cases
            tvThreadLabel.visibility = View.GONE
            vbThoughtValue.visibility = View.GONE
            tvTextIcon.visibility = View.GONE
            // empty thought
            tvEmptyThought.visibility = View.GONE

            var hasAnyContent = false

            // Rich text
            if (thought.hasText) {
                tvRichText.visibility = View.VISIBLE
                tvRichText.originalText = thought.richText!!
                hasAnyContent = true
            }

            // Audio
            if (thought.hasAudio && appSettingsStorage.isVoiceRecordingEnabled()) {
                thought.id?.let { thoughtId ->
                    onLoadAudio(thoughtId) { audioFile ->
                        if (audioFile.exists() && audioFile.length() > 0) {
                            audioView.visibility = View.VISIBLE
                            audioView.switchToPlaybackOnlyMode()
                            currentAudioFile = audioFile
                            audioView.loadAudioForPlayback(audioFile)
                        }
                    }
                }
                hasAnyContent = true
            }

            // Photo
            if (thought.hasPhoto && appSettingsStorage.isPhotoFeatureEnabled()) {
                thought.id?.let { thoughtId ->
                    onLoadPhoto(thoughtId) { photoData ->
                        if (photoData.isNotEmpty()) {
                            photoView.visibility = View.VISIBLE
                            photoView.loadPhoto(photoData)
                        }
                    }
                }
                hasAnyContent = true
            }

            // Show all thought's forms are empty
            if (!hasAnyContent) {
                tvEmptyThought.visibility = View.VISIBLE
            }

            vbThoughtValue.currentLevel = thought.value
            updateMetadataUI(thought, sortConfig)
        }

        fun updateMetadataUI(thought: ThoughtDTO, sortConfig: SortConfig) {
            // Showing thread label basing on sorting config
            val showThreadLabel = !thought.thread.isNullOrBlank() && sortConfig.property != SortProperty.THREAD
            tvThreadLabel.visibility = if (showThreadLabel) View.VISIBLE else View.GONE
            if (showThreadLabel) tvThreadLabel.text = "⧽  ".plus(thought.thread)

            // Cases by sorting properties
            when (sortConfig.property) {
                SortProperty.VALUE -> {
                    vbThoughtValue.visibility = View.VISIBLE
                    ivDecoratorIcon.visibility = View.GONE
                    tvLabel.visibility = View.GONE
                }
                SortProperty.CREATED_AT -> {
                    updateCreatedAtCase(thought)
                }
                SortProperty.UPDATED_AT -> {
                    updateChangedAtCase(thought)
                }
                SortProperty.THREAD -> {
                    ivDecoratorIcon.visibility = View.VISIBLE
                    ivDecoratorIcon.setImageResource(R.drawable.ic_hextags_thread)
                    tvLabel.visibility = View.VISIBLE
                    tvLabel.text = when (thought.thread.isNullOrBlank()){
                        true  -> {itemView.context.getString(R.string.stream_thought_metadata_empty)}
                        false -> {thought.thread}
                    }
                }
                SortProperty.PROJECT -> {
                    ivDecoratorIcon.visibility = View.VISIBLE
                    ivDecoratorIcon.setImageResource(R.drawable.ic_hextags_project)
                    tvLabel.visibility = View.VISIBLE
                    tvLabel.text = when (thought.project.isNullOrBlank()){
                        true -> {itemView.context.getString(R.string.stream_thought_metadata_empty)}
                        false -> {thought.project}
                    }
                }
                SortProperty.SOUL_MATE -> {
                    ivDecoratorIcon.visibility = View.VISIBLE
                    ivDecoratorIcon.setImageResource(R.drawable.ic_hextags_soul_mates)
                    tvLabel.visibility = View.VISIBLE
                    tvLabel.text = when (thought.soulMate.isNullOrBlank()){
                        true -> {itemView.context.getString(R.string.stream_thought_metadata_empty)}
                        false -> {thought.soulMate}
                    }
                }
            }
        }

        fun updateCreatedAtCase(thought : ThoughtDTO){
            val ageLevel = ThoughtGrowthStage.newThoughtGrowthStage(thought.createdAt)
            ivDecoratorIcon.visibility = View.GONE

            tvTextIcon.visibility = View.VISIBLE
            tvTextIcon.text = itemView.context.getString(ageLevel.level.iconResId)

            tvLabel.visibility = View.VISIBLE
            tvLabel.text = when (ageLevel.ageInDays) { // TODO: apply mechanism witch plurals? (see: strings.xml -> path_steps_count)
                0L -> { // Today
                    itemView.context.getString(R.string.common_thought_age_0)
                }
                1L -> { // Yesterday
                    itemView.context.getString(R.string.common_thought_age_1)
                }
                else -> {
                    itemView.context.getString(
                        R.string.common_thought_age_pattern, ageLevel.ageInDays.toString()
                    )
                }
            }
        }

        fun updateChangedAtCase(thought: ThoughtDTO) {
            ivDecoratorIcon.visibility = View.VISIBLE
            ivDecoratorIcon.setImageResource(R.drawable.ic_replace_or_renew)

            tvLabel.visibility = View.VISIBLE
            val ageInDays = ThoughtGrowthStage.getAgeInDays(thought.updatedAt)
            tvLabel.text = when (ageInDays) {
                0L -> itemView.context.getString(R.string.common_thought_updated_at_0)
                1L -> itemView.context.getString(R.string.common_thought_updated_at_1)
                else -> itemView.context.getString(R.string.common_thought_updated_at_pattern, ageInDays.toString())
            }
        }

        fun setViewOnTouchListener() {
            // Set touch listener on NestedScrollView to intercept gestures
            nestedScrollView.setOnTouchListener { view, event ->
                val handled = gestureDetector.onTouchEvent(event)
                if (event.action == MotionEvent.ACTION_UP && handled) {
                    view.performClick()
                }
                false // Return false to allow scrolling to work
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
         * Handles long press gesture - delete thought
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
            return false
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
            return false
        }

        override fun onDoubleTapEvent(e: MotionEvent): Boolean {
            return false
        }
    }

    /**
     * Optimized DiffCallback for efficient list updates
     */
    private class ThoughtDiffCallback : DiffUtil.ItemCallback<ThoughtDTO>() {
        override fun areItemsTheSame(oldItem: ThoughtDTO, newItem: ThoughtDTO): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ThoughtDTO, newItem: ThoughtDTO): Boolean {
            return oldItem == newItem
        }
    }
}