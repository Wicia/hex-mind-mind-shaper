package pl.hexmind.mindshaper.activities.capture.handlers

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.common.audio.AudioVisualizerView

open class RecordingCaptureView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    val btnRecordNew: MaterialButton
    val btnRecordStopPlay: MaterialButton
    val btnSkipBackward: MaterialButton
    val btnSkipForward: MaterialButton
    private val tvRecordingStatus: TextView
    private val tvRecordingTimer: TextView
    private val audioVisualizer: AudioVisualizerView

    init {
        inflate(context, R.layout.capture_view_recording, this)
        orientation = VERTICAL

        btnRecordNew = findViewById(R.id.btn_record_new)
        btnRecordStopPlay = findViewById(R.id.btn_record_stop_play)
        btnSkipBackward = findViewById(R.id.btn_skip_backward)
        btnSkipForward = findViewById(R.id.btn_skip_forward)
        tvRecordingStatus = findViewById(R.id.tv_recording_status)
        tvRecordingTimer = findViewById(R.id.tv_recording_timer)
        audioVisualizer = findViewById(R.id.audio_visualizer)
    }

    fun updateStatus(text: String, colorRes: Int) {
        tvRecordingStatus.text = text
        tvRecordingStatus.setTextColor(ContextCompat.getColor(context, colorRes))
    }

    fun updateTimer(currentMs: Long, totalMs: Long) {
        val currentSeconds = currentMs / 1000
        val totalSeconds = totalMs / 1000

        val currentMinutes = currentSeconds / 60
        val currentSecs = currentSeconds % 60
        val totalMinutes = totalSeconds / 60
        val totalSecs = totalSeconds % 60

        tvRecordingTimer.text = String.format(
            "%02d:%02d / %02d:%02d",
            currentMinutes, currentSecs,
            totalMinutes, totalSecs
        )
    }

    fun showSkipButtons(show: Boolean) {
        btnSkipBackward.visibility = if (show) View.VISIBLE else View.INVISIBLE
        btnSkipForward.visibility = if (show) View.VISIBLE else View.INVISIBLE
    }

    fun showVisualization(show: Boolean, isRecording: Boolean = false) {
        when {
            isRecording -> {
                audioVisualizer.visibility = View.INVISIBLE
                audioVisualizer.setRecordingMode(true)
            }
            show -> {
                audioVisualizer.visibility = View.VISIBLE
                audioVisualizer.setRecordingMode(false)
            }
            else -> {
                audioVisualizer.visibility = View.INVISIBLE
                audioVisualizer.setRecordingMode(false)
            }
        }
    }

    fun updateVisualization(amplitude: Int) {
        audioVisualizer.addAmplitude(amplitude)
    }

    fun clearVisualization() {
        audioVisualizer.clear()
    }

    fun updatePlaybackPosition(position: Float) {
        audioVisualizer.setPlaybackPosition(position)
    }

    fun updateButtons(isRecording: Boolean, isPlaying: Boolean, hasRecording: Boolean) {
        when {
            isRecording -> {
                btnRecordNew.isEnabled = false
                btnRecordNew.alpha = 0.5f
                btnRecordNew.setBackgroundColor(ContextCompat.getColor(context, R.color.button_disabled_background))

                btnRecordStopPlay.isEnabled = true
                btnRecordStopPlay.alpha = 1f
                btnRecordStopPlay.icon = AppCompatResources.getDrawable(context, R.drawable.ic_stop_circle)
                btnRecordStopPlay.setBackgroundColor(ContextCompat.getColor(context, R.color.button_primary))
            }
            isPlaying -> {
                btnRecordNew.isEnabled = false
                btnRecordNew.alpha = 0.5f

                btnRecordStopPlay.isEnabled = true
                btnRecordStopPlay.alpha = 1f
                btnRecordStopPlay.icon = AppCompatResources.getDrawable(context, R.drawable.ic_stop_circle)
                btnRecordStopPlay.setBackgroundColor(ContextCompat.getColor(context, R.color.button_primary))
            }
            hasRecording -> { // Has recording, ready to play
                btnRecordNew.isEnabled = true
                btnRecordNew.alpha = 1f
                btnRecordNew.setBackgroundColor(ContextCompat.getColor(context, R.color.button_primary))

                btnRecordStopPlay.isEnabled = true
                btnRecordStopPlay.alpha = 1f
                btnRecordStopPlay.icon = AppCompatResources.getDrawable(context, R.drawable.ic_recording_play)
            }
            else -> { // Initial state, no recording
                btnRecordNew.isEnabled = true
                btnRecordNew.alpha = 1f

                btnRecordStopPlay.isEnabled = false
                btnRecordStopPlay.alpha = 0.5f
                btnRecordStopPlay.icon = AppCompatResources.getDrawable(context, R.drawable.ic_recording_play)
                btnRecordStopPlay.setBackgroundColor(ContextCompat.getColor(context, R.color.button_disabled_background))
            }
        }
    }
}