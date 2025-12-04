package pl.hexmind.mindshaper.activities.capture.handlers

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.activities.capture.ThoughtCaptureHandler
import pl.hexmind.mindshaper.common.validation.ValidationResult
import pl.hexmind.mindshaper.services.dto.ThoughtDTO
import pl.hexmind.mindshaper.services.validators.ThoughtValidator
import java.io.File

/**
 * Simplified handler that uses AudioRecordingView's built-in functionality
 */
class RecordingCaptureHandler(
    private val activity: Activity,
    private val view: AudioRecordingView,
    override val validator: ThoughtValidator
) : ThoughtCaptureHandler, AudioRecordingView.RecordingCallback {

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }

    init {
        // Set mode to RECORD_PLAYBACK
        view.setMode(AudioRecordingView.Mode.RECORD_PLAYBACK)

        // Set callback to receive events from the view
        view.setRecordingCallback(this)
    }

    // ======================================================================================
    //      RecordingCallback Implementation
    // ======================================================================================

    override fun onRecordingStarted() {
        // Optional: Handle recording start event
    }

    override fun onRecordingStopped(file: File, durationMs: Long) {
        // Optional: Handle recording stop event
        // The file is already managed by the view
    }

    override fun onRecordingError(error: String) {
        view.showStatus(
            view.context.getString(R.string.capture_voice_error_recording),
            R.color.validation_error
        )
    }

    override fun onPlaybackStarted() {
        // Optional: Handle playback start event
    }

    override fun onPlaybackStopped() {
        // Optional: Handle playback stop event
    }

    override fun onPermissionRequired() {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            PERMISSION_REQUEST_CODE
        )
    }

    // ======================================================================================
    //      ThoughtCaptureHandler Implementation
    // ======================================================================================

    override fun performTypeSpecificValidation(dto: ThoughtDTO): ValidationResult {
        val recording = view.getCurrentRecording()

        if (recording.file == null) {
            return ValidationResult.Error(
                view.context.getString(R.string.validation_recording_missing)
            )
        }

        if (recording.duration == 0L) {
            return ValidationResult.Error(
                view.context.getString(R.string.validation_recording_empty)
            )
        }

        return ValidationResult.Valid()
    }

    override fun getUpdatedDTO(dto: ThoughtDTO): ThoughtDTO {
        // Return DTO with recording file path or other metadata if needed
        return dto.copy(
            // Add recording-specific fields here if needed
        )
    }

    // ===========================================
    //  Public Methods
    // ===========================================

    fun getCurrentRecording(): Recording = view.getCurrentRecording()

    fun cleanupResources() {
        view.cleanupResources()
    }

    fun onRequestPermissionsResult(
        requestCode: Int,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, user can try recording again
                view.showStatus(
                    view.context.getString(R.string.capture_voice_tooltip),
                    R.color.text_secondary
                )
            } else {
                // Permission denied
                view.showStatus(
                    "Wymagane uprawnienie do mikrofonu",
                    R.color.validation_error
                )
            }
        }
    }
}