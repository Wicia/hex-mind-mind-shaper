package pl.hexmind.fastnote.activities.capture.ui

import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import pl.hexmind.fastnote.R
import pl.hexmind.fastnote.activities.capture.models.CapturedThoughtValidator
import pl.hexmind.fastnote.activities.capture.models.InitialThoughtType

// Manager which encapsulates UI logic (handles UI update requests from Handlers)
class CaptureViewManager(private val activity: AppCompatActivity) {

    // UI Components - Essence
    lateinit var editThoughtEssence: EditText
    lateinit var tvWordCount: TextView

    // UI Components - Detailed Note
    lateinit var makingNotesLayout: LinearLayout
    lateinit var etRichText: EditText

    // UI Components - Voice Recording
    lateinit var voiceRecordingLayout: LinearLayout
    lateinit var btnRecordNew: MaterialButton
    lateinit var btnRecordStopNPlay: MaterialButton
    lateinit var tvRecordingStatus: TextView

    lateinit var btnSave: MaterialButton
    
    // Future UI components
    // lateinit var photoCaptureLayout: LinearLayout
    // lateinit var drawingLayout: LinearLayout
    
    fun initializeViews() {
        editThoughtEssence = activity.findViewById(R.id.et_essence)
        editThoughtEssence.hint = activity.getString(R.string.capture_essence_tooltip, CapturedThoughtValidator.ESSENCE_MAX_WORDS)

        etRichText = activity.findViewById(R.id.et_rich_rotes)
        voiceRecordingLayout = activity.findViewById(R.id.ll_voice_recording)
        makingNotesLayout = activity.findViewById(R.id.ll_notes)
        btnRecordNew = activity.findViewById(R.id.btn_record_new)
        btnRecordStopNPlay = activity.findViewById(R.id.btn_record_stop_play)
        btnSave = activity.findViewById(R.id.btn_save)
        tvWordCount = activity.findViewById(R.id.tv_essence_words_info)
        tvRecordingStatus = activity.findViewById(R.id.tv_recording_status)
    }
    
    fun setupModeVisibility(inputType: InitialThoughtType) {
        // Hide all layouts first
        voiceRecordingLayout.visibility = View.GONE
        makingNotesLayout.visibility = View.GONE
        
        when (inputType) {
            InitialThoughtType.NOTE -> {
                makingNotesLayout.visibility = View.VISIBLE
            }
            InitialThoughtType.VOICE -> {
                voiceRecordingLayout.visibility = View.VISIBLE
            }
            InitialThoughtType.PHOTO -> {
                // photoCaptureLayout.visibility = View.VISIBLE
            }
            InitialThoughtType.DRAWING -> {
                // drawingLayout.visibility = View.VISIBLE
            }
            InitialThoughtType.UNKNOWN -> {
                // TODO: co wtedy?
            }
        }
    }
    
    fun updateWordsCounterTextView(text: String) {
        when (val validationResult = CapturedThoughtValidator.validateEssence(text)){
            is CapturedThoughtValidator.ValidationResult.Error -> {
                tvWordCount.text = activity.getString(validationResult.messageId, validationResult.param)
                tvWordCount.setTextColor(ContextCompat.getColor(activity, R.color.error_red))
            }
            is CapturedThoughtValidator.ValidationResult.Valid -> {
                tvWordCount.text = activity.getString(validationResult.messageId, validationResult.param)
                // TODO: Znalezc inne te android.R i czy zostawic czy zastapic swoimi?
                tvWordCount.setTextColor(ContextCompat.getColor(activity, R.color.success_green))
            }
        }
    }
    
    fun updateRecordingStatusTextViews(text: String, colorRes: Int) {
        tvRecordingStatus.text = text
        tvRecordingStatus.setTextColor(ContextCompat.getColor(activity, colorRes))
    }
    
    fun updateRecordingButtons(isRecording: Boolean, isPlaying: Boolean, hasRecording: Boolean) {
        when {
            isRecording -> {
                btnRecordStopNPlay.icon = ContextCompat.getDrawable(activity, R.drawable.ic_stop_circle)
                btnRecordStopNPlay.text = ContextCompat.getString(activity, R.string.capture_voice_record_stop)
                btnRecordStopNPlay.isEnabled = true
            }
            isPlaying -> {
                btnRecordStopNPlay.icon = ContextCompat.getDrawable(activity, R.drawable.ic_stop_circle)
                btnRecordStopNPlay.text = ContextCompat.getString(activity, R.string.capture_voice_record_stop)
                btnRecordStopNPlay.isEnabled = true
            }
            hasRecording -> {
                btnRecordStopNPlay.icon = ContextCompat.getDrawable(activity, R.drawable.ic_recording_play)
                btnRecordStopNPlay.text = ContextCompat.getString(activity, R.string.capture_voice_play)
                btnRecordStopNPlay.isEnabled = true
            }
            else -> {
                btnRecordStopNPlay.icon = ContextCompat.getDrawable(activity, R.drawable.ic_recording_play)
                btnRecordStopNPlay.text = ContextCompat.getString(activity, R.string.capture_voice_play)
                btnRecordStopNPlay.isEnabled = false
            }
        }
    }
}