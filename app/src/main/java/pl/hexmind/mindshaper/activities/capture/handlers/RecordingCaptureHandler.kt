package pl.hexmind.mindshaper.activities.capture.handlers

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.activities.capture.ThoughtCaptureHandler
import pl.hexmind.mindshaper.common.validation.ValidationResult
import pl.hexmind.mindshaper.services.dto.ThoughtDTO
import pl.hexmind.mindshaper.services.validators.ThoughtValidator
import java.io.File

class RecordingCaptureHandler(
    private val activity: Activity,
    private val view: RecordingCaptureView,
    override val validator: ThoughtValidator
) : ThoughtCaptureHandler {

    companion object {
        private const val MAX_RECORDING_DURATION_MS = 180_000L
        private const val TIMER_UPDATE_INTERVAL_MS = 100L
        private const val AMPLITUDE_UPDATE_INTERVAL_MS = 50L
        private const val SKIP_DURATION_MS = 5000L
    }

    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private var tempAudioFile: File? = null
    private var isRecording = false
    private var isPlaying = false

    private val timerHandler = Handler(Looper.getMainLooper())
    private val amplitudeHandler = Handler(Looper.getMainLooper())
    private var recordingStartTime = 0L
    private var currentRecordingDuration = 0L

    private val timerRunnable = object : Runnable {
        override fun run() {
            if (isRecording) {
                currentRecordingDuration = System.currentTimeMillis() - recordingStartTime

                if (currentRecordingDuration >= MAX_RECORDING_DURATION_MS) {
                    stopRecording()
                    view.updateStatus(
                        view.context.getString(R.string.capture_voice_status_max_duration_reached),
                        R.color.validation_error
                    )
                } else {
                    view.updateTimer(currentRecordingDuration, MAX_RECORDING_DURATION_MS)
                    timerHandler.postDelayed(this, TIMER_UPDATE_INTERVAL_MS)
                }
            }
        }
    }

    private val amplitudeRunnable = object : Runnable {
        override fun run() {
            if (isRecording && recorder != null) {
                val amplitude = recorder?.maxAmplitude ?: 0
                view.updateVisualization(amplitude)
                amplitudeHandler.postDelayed(this, AMPLITUDE_UPDATE_INTERVAL_MS)
            }
        }
    }

    private val playbackUpdateRunnable = object : Runnable {
        override fun run() {
            if (isPlaying && player != null) {
                player?.let {
                    if (it.isPlaying) {
                        val currentPosition = it.currentPosition.toLong()
                        val duration = it.duration.toLong()
                        view.updateTimer(currentPosition, duration)
                        view.updatePlaybackPosition(currentPosition.toFloat() / duration)
                        timerHandler.postDelayed(this, TIMER_UPDATE_INTERVAL_MS)
                    }
                }
            }
        }
    }

    fun setupListeners() {
        view.btnRecordNew.setOnClickListener { startRecording() }
        view.btnRecordStopPlay.setOnClickListener {
            when {
                isRecording -> stopRecording()
                isPlaying -> stopPlaying()
                else -> startPlaying()
            }
        }

        view.btnSkipBackward.setOnClickListener { skipBackward() }
        view.btnSkipForward.setOnClickListener { skipForward() }
    }

    private fun skipBackward() {
        player?.let {
            val currentPosition = it.currentPosition
            val newPosition = (currentPosition - SKIP_DURATION_MS).coerceAtLeast(0)
            it.seekTo(newPosition.toInt())
        }
    }

    private fun skipForward() {
        player?.let {
            val currentPosition = it.currentPosition
            val duration = it.duration
            val newPosition = (currentPosition + SKIP_DURATION_MS).coerceAtMost(duration.toLong())
            it.seekTo(newPosition.toInt())
        }
    }

    private fun startRecording() {
        deleteRecording()
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.RECORD_AUDIO), 100)
            return
        }

        tempAudioFile?.delete()
        view.clearVisualization()

        tempAudioFile = File(activity.cacheDir, "recording_${System.currentTimeMillis()}.m4a")

        try {
            recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(activity)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setOutputFile(tempAudioFile!!.absolutePath)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                prepare()
                start()
            }

            isRecording = true
            recordingStartTime = System.currentTimeMillis()
            currentRecordingDuration = 0L

            view.updateButtons(isRecording, isPlaying, tempAudioFile != null)
            view.showSkipButtons(false)

            view.showVisualization(show = true, isRecording = true)

            timerHandler.post(timerRunnable)
            amplitudeHandler.post(amplitudeRunnable)

        } catch (e: Exception) {
            e.printStackTrace()
            view.updateStatus(
                view.context.getString(R.string.capture_voice_error_recording),
                R.color.validation_error
            )
            cleanupRecorder()
        }
    }

    private fun stopRecording() {
        timerHandler.removeCallbacks(timerRunnable)
        amplitudeHandler.removeCallbacks(amplitudeRunnable)

        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            recorder = null
            isRecording = false
        }

        if (tempAudioFile?.exists() == true && (tempAudioFile?.length() ?: 0) > 0) {
            val durationText = formatDuration(currentRecordingDuration)
            view.updateStatus(
                view.context.getString(R.string.capture_voice_status_recording_saved),
                R.color.validation_success
            )

            view.showVisualization(show = true, isRecording = false)

        } else {
            view.updateStatus(
                view.context.getString(R.string.capture_voice_error_recording),
                R.color.validation_error
            )
            tempAudioFile?.delete()
            tempAudioFile = null
        }

        view.updateButtons(isRecording, isPlaying, tempAudioFile != null)
        view.showSkipButtons(false)
    }

    private fun startPlaying() {
        tempAudioFile?.let { file ->
            if (!file.exists()) {
                view.updateStatus(
                    view.context.getString(R.string.capture_voice_error_playback),
                    R.color.validation_error
                )
                return
            }

            startPlayingFile(file)
        }
    }

    private fun startPlayingFile(file: File) {
        try {
            player = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                start()
                setOnCompletionListener { stopPlaying() }
            }

            isPlaying = true
            view.updateButtons(isRecording, isPlaying, tempAudioFile != null)
            view.showSkipButtons(true)

            timerHandler.post(playbackUpdateRunnable)

        } catch (e: Exception) {
            e.printStackTrace()
            view.updateStatus(
                view.context.getString(R.string.capture_voice_error_playback),
                R.color.validation_error
            )
        }
    }

    private fun stopPlaying() {
        timerHandler.removeCallbacks(playbackUpdateRunnable)

        player?.release()
        player = null
        isPlaying = false

        view.updatePlaybackPosition(0f)
        view.updateButtons(isRecording, isPlaying, tempAudioFile != null)
        view.showSkipButtons(false)
    }

    private fun deleteRecording() {
        stopPlaying()

        tempAudioFile?.delete()
        tempAudioFile = null

        view.clearVisualization()
        view.updateStatus(
            view.context.getString(R.string.capture_voice_tooltip),
            R.color.text_secondary
        )
        view.updateButtons(isRecording, isPlaying, tempAudioFile != null)
        view.showVisualization(false)
    }

    private fun cleanupRecorder() {
        recorder?.release()
        recorder = null
        isRecording = false
    }

    private fun formatDuration(millis: Long): String {
        val totalSeconds = millis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    fun cleanup() {
        timerHandler.removeCallbacks(timerRunnable)
        amplitudeHandler.removeCallbacks(amplitudeRunnable)
        timerHandler.removeCallbacks(playbackUpdateRunnable)

        if (isRecording) {
            stopRecording()
        }
        if (isPlaying) {
            stopPlaying()
        }

        recorder?.release()
        player?.release()
        recorder = null
        player = null

        tempAudioFile?.delete()
    }

    fun getTempAudioFile(): File? = tempAudioFile

    override fun performTypeSpecificValidation(dto: ThoughtDTO): ValidationResult {
        if (tempAudioFile == null || tempAudioFile?.exists() != true) {
            return ValidationResult.Error(
                view.context.getString(R.string.validation_recording_missing)
            )
        }

        if ((tempAudioFile?.length() ?: 0) == 0L) {
            return ValidationResult.Error(
                view.context.getString(R.string.validation_recording_empty)
            )
        }

        return ValidationResult.Valid()
    }

    override fun getUpdatedDTO(dto: ThoughtDTO): ThoughtDTO {
        return ThoughtDTO()
    }
}