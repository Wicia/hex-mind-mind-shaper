package pl.hexmind.mindshaper.activities.capture.handlers

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.activities.ThoughtCaptureHandler
import pl.hexmind.mindshaper.common.validation.ValidationResult
import pl.hexmind.mindshaper.services.dto.ThoughtDTO
import java.io.File

class RecordingCaptureHandler(
    private val activity: Activity,
    private val view: RecordingCaptureView
) : ThoughtCaptureHandler {

    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private var audioFile: File? = null
    private var isRecording = false
    private var isPlaying = false

    fun setupListeners() {
        view.btnRecordNew.setOnClickListener { startRecording() }
        view.btnRecordStopPlay.setOnClickListener {
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
        val string = view.context.getString(R.string.capture_voice_status_recording)
        view.updateStatus(string, R.color.validation_error)
        view.updateButtons(isRecording, isPlaying, audioFile != null)
    }

    private fun stopRecording() {
        recorder?.apply {
            stop()
            release()
        }
        recorder = null
        isRecording = false
        val string = view.context.getString(R.string.capture_voice_status_recording_saved)
        view.updateStatus(string, R.color.validation_success)
        view.updateButtons(isRecording, isPlaying, audioFile != null)
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
            val string = view.context.getString(R.string.capture_voice_status_recording_playing)
            view.updateStatus(string, R.color.validation_success)
            view.updateButtons(isRecording, isPlaying, audioFile != null)
        }
    }

    private fun stopPlaying() {
        player?.release()
        player = null
        isPlaying = false
        view.updateStatus("???", R.color.validation_success)
        view.updateButtons(isRecording, isPlaying, audioFile != null)
    }

    override fun performValidation(): ValidationResult {
        TODO("Not yet implemented")
    }

    override fun getUpdatedDTO(dto: ThoughtDTO): ThoughtDTO {
        TODO("Not yet implemented")
    }
}