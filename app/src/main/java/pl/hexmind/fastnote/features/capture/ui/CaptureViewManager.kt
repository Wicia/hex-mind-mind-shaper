package pl.hexmind.fastnote.features.capture.ui

import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import pl.hexmind.fastnote.R

// Manager which encapsulates UI logic (handles UI update requests from Handlers)
class CaptureViewManager(private val activity: AppCompatActivity) {
    
    // UI Components
    lateinit var editThoughtEssence: EditText
    lateinit var etRichText: EditText
    lateinit var btnRecordNew: MaterialButton
    lateinit var btnRecordStopNPlay: MaterialButton
    lateinit var btnSave: MaterialButton
    lateinit var tvWordCount: TextView
    lateinit var tvRecordingStatus: TextView
    lateinit var makingNotesLayout: LinearLayout
    lateinit var voiceRecordingLayout: LinearLayout
    
    // Future UI components
    // lateinit var photoCaptureLayout: LinearLayout
    // lateinit var drawingLayout: LinearLayout
    
    fun initializeViews() {
        editThoughtEssence = activity.findViewById(R.id.editThoughtEssence)
        etRichText = activity.findViewById(R.id.richNotes)
        voiceRecordingLayout = activity.findViewById(R.id.voiceRecordingLayout)
        makingNotesLayout = activity.findViewById(R.id.notesLayout)
        btnRecordNew = activity.findViewById(R.id.btnRecordNew)
        btnRecordStopNPlay = activity.findViewById(R.id.btnRecordStopNPlay)
        btnSave = activity.findViewById(R.id.btnSave)
        tvWordCount = activity.findViewById(R.id.tvWordCount)
        tvRecordingStatus = activity.findViewById(R.id.tvRecordingStatus)
    }
    
    fun setupModeVisibility(inputType: String) {
        // Hide all layouts first
        voiceRecordingLayout.visibility = View.GONE
        makingNotesLayout.visibility = View.GONE
        
        when (inputType) {
            "rich_text" -> {
                makingNotesLayout.visibility = View.VISIBLE
            }
            "voice" -> {
                voiceRecordingLayout.visibility = View.VISIBLE
            }
            "photo" -> {
                // photoCaptureLayout.visibility = View.VISIBLE
            }
            "drawing" -> {
                // drawingLayout.visibility = View.VISIBLE
            }
        }
    }
    
    fun updateWordCount(wordCount: Int) {
        tvWordCount.text = "$wordCount/10 words"
        
        val colorRes = if (wordCount > 10) {
            android.R.color.holo_red_dark
        } else {
            android.R.color.darker_gray
        }
        
        tvWordCount.setTextColor(ContextCompat.getColor(activity, colorRes))
    }
    
    fun updateRecordingStatus(text: String, colorRes: Int) {
        tvRecordingStatus.text = text
        tvRecordingStatus.setTextColor(ContextCompat.getColor(activity, colorRes))
    }
    
    fun updateRecordingButtons(isRecording: Boolean, isPlaying: Boolean, hasRecording: Boolean) {
        when {
            isRecording -> {
                btnRecordStopNPlay.icon = ContextCompat.getDrawable(activity, R.drawable.icon_stop_circle)
                btnRecordStopNPlay.isEnabled = true
            }
            isPlaying -> {
                btnRecordStopNPlay.icon = ContextCompat.getDrawable(activity, R.drawable.icon_stop_circle)
                btnRecordStopNPlay.isEnabled = true
            }
            hasRecording -> {
                btnRecordStopNPlay.icon = ContextCompat.getDrawable(activity, R.drawable.icon_recording_play)
                btnRecordStopNPlay.isEnabled = true
            }
            else -> {
                btnRecordStopNPlay.icon = ContextCompat.getDrawable(activity, R.drawable.icon_recording_play)
                btnRecordStopNPlay.isEnabled = false
            }
        }
    }
}