package pl.hexmind.mindshaper.activities.capture.handlers

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.activities.capture.ui.VoiceCaptureView
import pl.hexmind.mindshaper.services.dto.ThoughtDTO
import java.io.File

class VoiceRecordingHandler(
    private val activity: Activity,
    private val voiceView: VoiceCaptureView
) {

    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private var audioFile: File? = null
    private var isRecording = false
    private var isPlaying = false

    private var onDataChangedListener: ((ThoughtDTO) -> Unit)? = null

    fun setupListeners() {
        voiceView.btnRecordNew.setOnClickListener { startRecording() }
        voiceView.btnRecordStopPlay.setOnClickListener {
            when {
                isRecording -> stopRecording()
                isPlaying -> stopPlaying()
                else -> startPlaying()
            }
        }
    }

    private fun startRecording() {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.RECORD_AUDIO), 100)
            return
        }

        audioFile = File(activity.cacheDir, "thought_${System.currentTimeMillis()}.3gp")
        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setOutputFile(audioFile!!.absolutePath)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            prepare()
            start()
        }
        isRecording = true
        voiceView.updateStatus("Nagrywanie...", R.color.error_red)
        voiceView.updateButtons(isRecording, isPlaying, audioFile != null)
    }

    private fun stopRecording() {
        recorder?.apply {
            stop()
            release()
        }
        recorder = null
        isRecording = false
        voiceView.updateStatus("Nagranie zapisane", R.color.success_green)
        voiceView.updateButtons(isRecording, isPlaying, audioFile != null)
        notifyDataChanged()
    }

    private fun startPlaying() {
        audioFile?.let {
            player = MediaPlayer().apply {
                setDataSource(it.absolutePath)
                prepare()
                start()
                setOnCompletionListener { stopPlaying() }
            }
            isPlaying = true
            voiceView.updateStatus("Odtwarzanie...", R.color.success_green)
            voiceView.updateButtons(isRecording, isPlaying, audioFile != null)
        }
    }

    private fun stopPlaying() {
        player?.release()
        player = null
        isPlaying = false
        voiceView.updateStatus("Gotowy", R.color.success_green)
        voiceView.updateButtons(isRecording, isPlaying, audioFile != null)
    }

    fun getCurrentData(): ThoughtDTO {
        return ThoughtDTO() // TODO: Initialization
    }

    fun setOnDataChangedListener(listener: (ThoughtDTO) -> Unit) {
        onDataChangedListener = listener
    }

    private fun notifyDataChanged() {
        onDataChangedListener?.invoke(getCurrentData())
    }

    fun cleanup() {
        recorder?.release()
        recorder = null
        player?.release()
        player = null
    }
}