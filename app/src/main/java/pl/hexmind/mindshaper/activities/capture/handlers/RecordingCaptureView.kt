package pl.hexmind.mindshaper.activities.capture.handlers

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import pl.hexmind.mindshaper.R

class RecordingCaptureView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    val btnRecordNew: MaterialButton
    val btnRecordStopPlay: MaterialButton
    private val tvRecordingStatus: TextView

    init {
        inflate(context, R.layout.capture_view_recording, this)
        orientation = VERTICAL

        btnRecordNew = findViewById(R.id.btn_record_new)
        btnRecordStopPlay = findViewById(R.id.btn_record_stop_play)
        tvRecordingStatus = findViewById(R.id.tv_recording_status)
    }

    fun updateStatus(text: String, colorRes: Int) {
        tvRecordingStatus.text = text
        tvRecordingStatus.setTextColor(ContextCompat.getColor(context, colorRes))
    }

    fun updateButtons(isRecording: Boolean, isPlaying: Boolean, hasRecording: Boolean) {
        when {
            isRecording -> {
                btnRecordStopPlay.icon = ContextCompat.getDrawable(context, R.drawable.ic_stop_circle)
                btnRecordStopPlay.text = context.getString(R.string.capture_voice_record_stop)
                btnRecordStopPlay.isEnabled = true
            }
            isPlaying -> {
                btnRecordStopPlay.icon = ContextCompat.getDrawable(context, R.drawable.ic_stop_circle)
                btnRecordStopPlay.text = context.getString(R.string.capture_voice_record_stop)
                btnRecordStopPlay.isEnabled = true
            }
            hasRecording -> {
                btnRecordStopPlay.icon = ContextCompat.getDrawable(context, R.drawable.ic_recording_play)
                btnRecordStopPlay.text = context.getString(R.string.capture_voice_play)
                btnRecordStopPlay.isEnabled = true
            }
            else -> {
                btnRecordStopPlay.icon = ContextCompat.getDrawable(context, R.drawable.ic_recording_play)
                btnRecordStopPlay.text = context.getString(R.string.capture_voice_play)
                btnRecordStopPlay.isEnabled = false
            }
        }
    }
}