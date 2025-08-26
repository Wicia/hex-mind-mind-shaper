package pl.hexmind.fastnote.activities.capture.handlers

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
import pl.hexmind.fastnote.activities.capture.models.CapturedThought
import pl.hexmind.fastnote.activities.capture.ui.CaptureViewManager
import java.io.File
import java.io.IOException

// Handler for business logic related to VOICE RECORDING - thought type
class VoiceRecordingHandler(
    private val activity: AppCompatActivity,
    private val viewManager: CaptureViewManager
) {

    // Main objects
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var audioFilePath: String = ""

    // Permissions
    private var permissionToRecordAccepted = false

    // State & Listeners
    private var isRecording = false
    private var isCurrentlyPlaying = false
    private var hasRecording = false
    private var onDataChangedListener: ((CapturedThought) -> Unit)? = null

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
                        activity.getString(R.string.common_permission_audio_error_required),
                        Toast.LENGTH_LONG
                    ).show()

                    if (response.isPermanentlyDenied) {
                        Toast.makeText(
                            activity,
                            activity.getString(R.string.common_permission_audio_message_settings),
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
                        activity.getString(R.string.common_permission_audio_message_rationale),
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
                viewManager.updateRecordingStatusTextViews(
                    activity.getString(R.string.capture_voice_status_recording),
                    R.color.error_red
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
        viewManager.updateRecordingStatusTextViews(
            activity.getString(R.string.capture_voice_status_recording_saved),
            R.color.success_green
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
                viewManager.updateRecordingStatusTextViews(
                    activity.getString(R.string.capture_voice_status_recording_playing),
                    R.color.info_blue
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
        viewManager.updateRecordingStatusTextViews(
            activity.getString(R.string.capture_voice_status_recording_saved),
            R.color.success_green
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
    fun getCurrentData(): CapturedThought {
        val audioFile = if (hasRecording) File(audioFilePath) else null
        return CapturedThought(audioFile = audioFile)
    }

    // Sets listener for data change notifications
    fun setOnDataChangedListener(listener: (CapturedThought) -> Unit) {
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