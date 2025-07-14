package pl.hexmind.fastnote.features.capture.handlers

import android.Manifest
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import pl.hexmind.fastnote.R
import pl.hexmind.fastnote.features.capture.models.CaptureData
import pl.hexmind.fastnote.features.capture.ui.CaptureViewManager
import java.io.File
import java.io.IOException

// Handler for business logic related to VOICE RECORDING - thought type
class VoiceRecordingHandler(
    private val activity: AppCompatActivity,
    private val viewManager: CaptureViewManager
) {

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var audioFilePath: String = ""
    private var isRecording = false
    private var isCurrentlyPlaying = false
    private var hasRecording = false
    private var permissionToRecordAccepted = false

    private var onDataChangedListener: ((CaptureData) -> Unit)? = null

    init {
        setupAudioPath()
    }

    // Sets up click listeners for recording buttons
    fun setupListeners() {
        viewManager.btnRecordNew.setOnClickListener {
            startNewRecording()
        }

        viewManager.btnRecordStopNPlay.setOnClickListener {
            when {
                isRecording -> stopRecording()
                isCurrentlyPlaying -> stopPlaying()
                hasRecording -> startPlaying()
            }
        }
    }

    // Configures the path where audio files will be stored
    private fun setupAudioPath() {
        audioFilePath = "${activity.externalCacheDir?.absolutePath}/audiorecord.3gp"
    }

    // Requests audio recording permission from user using Dexter library
    fun requestPermission() {
        Dexter.withContext(activity)
            .withPermission(Manifest.permission.RECORD_AUDIO)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(response: PermissionGrantedResponse) {
                    permissionToRecordAccepted = true
                    updateUI()
                }

                override fun onPermissionDenied(response: PermissionDeniedResponse) {
                    permissionToRecordAccepted = false
                    Toast.makeText(
                        activity,
                        activity.getString(R.string.permission_audio_error_required),
                        Toast.LENGTH_LONG
                    ).show()

                    if (response.isPermanentlyDenied) {
                        Toast.makeText(
                            activity,
                            activity.getString(R.string.permission_audio_message_settings),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permission: PermissionRequest,
                    token: PermissionToken
                ) {
                    Toast.makeText(
                        activity,
                        activity.getString(R.string.permission_audio_message_rationale),
                        Toast.LENGTH_LONG
                    ).show()
                    token.continuePermissionRequest()
                }
            })
            .check()
    }

    // Creates MediaRecorder instance compatible with current Android version
    private fun createMediaRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(activity)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
    }

    // Starts recording new audio, handling permissions and state management
    private fun startNewRecording() {
        if (!permissionToRecordAccepted) {
            requestPermission()
            return
        }

        if (isCurrentlyPlaying) {
            stopPlaying()
        }

        if (isRecording) {
            stopRecording()
        }

        deleteExistingRecording()

        mediaRecorder = createMediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setOutputFile(audioFilePath)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)

            try {
                prepare()
                start()
                isRecording = true
                hasRecording = false
                updateUI()
                viewManager.updateRecordingStatus(
                    activity.getString(R.string.capture_voice_status_recording),
                    android.R.color.holo_red_dark
                )
            } catch (e: IOException) {
                Toast.makeText(
                    activity,
                    activity.getString(R.string.capture_voice_error_recording_failed, e.message),
                    Toast.LENGTH_SHORT
                ).show()
                release()
                mediaRecorder = null
            }
        }
    }

    // Stops recording and saves the audio file
    private fun stopRecording() {
        mediaRecorder?.apply {
            try {
                stop()
                release()
            } catch (e: RuntimeException) {
                release()
            }
        }
        mediaRecorder = null
        isRecording = false
        hasRecording = true
        updateUI()
        viewManager.updateRecordingStatus(
            activity.getString(R.string.capture_voice_status_recording_saved),
            android.R.color.holo_green_dark
        )
        notifyDataChanged()
    }

    // Starts playing the recorded audio file
    private fun startPlaying() {
        if (!hasRecording) {
            Toast.makeText(
                activity,
                activity.getString(R.string.capture_voice_error_no_recording),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(audioFilePath)
                prepare()
                start()
                isCurrentlyPlaying = true
                updateUI()
                viewManager.updateRecordingStatus(
                    activity.getString(R.string.capture_voice_status_recording_playing),
                    android.R.color.holo_blue_dark
                )

                setOnCompletionListener {
                    stopPlaying()
                }
            } catch (e: IOException) {
                Toast.makeText(
                    activity,
                    activity.getString(R.string.capture_voice_error_recording_failed, e.message),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // Stops playing audio and updates UI state
    private fun stopPlaying() {
        mediaPlayer?.apply {
            stop()
            release()
        }
        mediaPlayer = null
        isCurrentlyPlaying = false
        updateUI()
        viewManager.updateRecordingStatus(
            activity.getString(R.string.capture_voice_status_recording_saved),
            android.R.color.holo_green_dark
        )
    }

    // Deletes existing audio recording file
    private fun deleteExistingRecording() {
        val file = File(audioFilePath)
        if (file.exists()) {
            file.delete()
        }
        hasRecording = false
    }

    // Updates UI elements based on current recording state
    private fun updateUI() {
        viewManager.updateRecordingButtons(isRecording, isCurrentlyPlaying, hasRecording)
    }

    // Returns current capture data with audio file if available
    fun getCurrentData(): CaptureData {
        val audioFile = if (hasRecording) File(audioFilePath) else null
        return CaptureData(audioFile = audioFile)
    }

    // Sets listener for data change notifications
    fun setOnDataChangedListener(listener: (CaptureData) -> Unit) {
        onDataChangedListener = listener
    }

    // Notifies registered listener about data changes
    private fun notifyDataChanged() {
        onDataChangedListener?.invoke(getCurrentData())
    }

    // Releases media resources to prevent memory leaks
    fun cleanup() {
        mediaRecorder?.release()
        mediaPlayer?.release()
    }
}