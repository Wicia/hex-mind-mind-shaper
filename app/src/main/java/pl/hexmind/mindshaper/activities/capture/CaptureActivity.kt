package pl.hexmind.mindshaper.activities.capture

import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.activities.CoreActivity
import pl.hexmind.mindshaper.activities.capture.handlers.AudioRecordingView
import pl.hexmind.mindshaper.activities.capture.handlers.RecordingCaptureHandler
import pl.hexmind.mindshaper.activities.capture.handlers.RichTextCaptureHandler
import pl.hexmind.mindshaper.activities.capture.handlers.RichTextCaptureView
import pl.hexmind.mindshaper.activities.capture.models.InitialThoughtType
import pl.hexmind.mindshaper.common.regex.HexTagsUtils
import pl.hexmind.mindshaper.common.validation.ValidatedProperty
import pl.hexmind.mindshaper.common.validation.ValidationResult
import pl.hexmind.mindshaper.services.ThoughtsService
import pl.hexmind.mindshaper.services.dto.ThoughtDTO
import pl.hexmind.mindshaper.services.validators.ThoughtValidator
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class CaptureActivity : CoreActivity(), AudioRecordingView.RecordingCallback {

    companion object Params {
        const val P_INIT_THOUGHT_TYPE = "P_EXTRA_INIT_THOUGHT_TYPE"
        private const val TAG = "CaptureActivity"
    }

    @Inject
    lateinit var thoughtValidator: ThoughtValidator

    @Inject
    lateinit var thoughtsService: ThoughtsService

    private var initialThoughtType: InitialThoughtType = InitialThoughtType.UNKNOWN
    private lateinit var flContainerFeatures: FrameLayout
    private lateinit var btnSave: FloatingActionButton
    private lateinit var etHexTags: TextInputEditText
    private lateinit var tvHexTagsValidationInfo: TextView

    // HANDLER for specific input/thought type
    private lateinit var thoughtCaptureHandler: ThoughtCaptureHandler

    // Recording handler reference (for audio saving)
    private var recordingHandler: RecordingCaptureHandler? = null

    // Recording view reference (for direct access to recording functionality)
    private var audioRecordingView: AudioRecordingView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.capture_activity)
        initViews()
        saveExtrasFromIntent()
        setupListeners()
        setupMode()
    }

    private fun initViews() {
        etHexTags = findViewById(R.id.et_hex_tags)
        flContainerFeatures = findViewById(R.id.fl_container_features)
        btnSave = findViewById(R.id.btn_save)
        tvHexTagsValidationInfo = findViewById(R.id.tv_hex_tags_validation_info)
        setupHeader(R.drawable.ic_catching_thought, R.string.capture_main_label)
    }

    private fun saveExtrasFromIntent() {
        initialThoughtType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(P_INIT_THOUGHT_TYPE, InitialThoughtType::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(P_INIT_THOUGHT_TYPE)
        } ?: InitialThoughtType.UNKNOWN
    }

    private fun setupMode() {
        flContainerFeatures.removeAllViews()
        when (initialThoughtType) {
            InitialThoughtType.RICH_TEXT -> {
                val richTextCaptureView = RichTextCaptureView(this)
                flContainerFeatures.addView(richTextCaptureView)
                thoughtCaptureHandler = RichTextCaptureHandler(richTextCaptureView, thoughtValidator).apply {
                    setupListeners()
                }
                recordingHandler = null
                audioRecordingView = null
            }
            InitialThoughtType.RECORDING -> {
                val captureRecordingView = AudioRecordingView(this)
                captureRecordingView.setMode(AudioRecordingView.Mode.RECORD_PLAYBACK)
                captureRecordingView.setRecordingCallback(this) // Set activity as callback
                flContainerFeatures.addView(captureRecordingView)

                val handler = RecordingCaptureHandler(
                    activity = this,
                    view = captureRecordingView,
                    validator = thoughtValidator
                )

                thoughtCaptureHandler = handler
                recordingHandler = handler
                audioRecordingView = captureRecordingView
            }
            else -> { /* TODO: next modes */
            }
        }
    }

    private fun setupListeners() {
        btnSave.setOnClickListener {
            lifecycleScope.launch { saveThought() }
        }

        // TODO: Rethink it and fix => there is a small bug below :)
        // ! info: code below prevents adding more than 3 separate words in input field
//        etHexTags.doAfterTextChanged { editable ->
//            editable?.let {
//                val words = it.toString().convertToWords()
//                if (thoughtValidator.validateThread(it.toString()) is ValidationResult.Error) {
//                    val limited = words.take(ThoughtValidator.THREAD_MAX_WORDS).joinToString(" ")
//                    etHexTags.setText(limited)
//                    etHexTags.setSelection(limited.length)
//                }
//            }
//        }
    }

    // ===========================================
    // AudioRecordingView.RecordingCallback Implementation
    // ===========================================

    override fun onRecordingStarted() {
        // Optional: Update UI or perform actions when recording starts
        resetValidationUI()
    }

    override fun onRecordingStopped(file: File, durationMs: Long) {
        // Optional: Update UI or perform actions when recording stops
    }

    override fun onRecordingError(error: String) {
        Timber.tag(TAG).e("Recording error: $error")
        tvHexTagsValidationInfo.visibility = View.VISIBLE
        tvHexTagsValidationInfo.text = getString(R.string.capture_voice_error_recording)
    }

    override fun onPlaybackStarted() {

    }

    override fun onPlaybackStopped() {

    }

    override fun onPermissionRequired() {
        Timber.tag(TAG).d("Permission required - will be handled by RecordingCaptureHandler")
        // Permission handling is done by RecordingCaptureHandler
    }

    // ===========================================
    // Permissions Handling
    // ===========================================

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        recordingHandler?.onRequestPermissionsResult(requestCode, grantResults)
    }

    // ===========================================
    //      Save Thought Logic
    // ===========================================

    private suspend fun saveThought() {
        resetValidationUI()
        val dto = ThoughtDTO()
        val updatedDto = thoughtCaptureHandler.getUpdatedDTO(dto)
        val dtoToSave = updateDTOWithHexTags(updatedDto)

        val validationResult = thoughtCaptureHandler.performValidation(dtoToSave)
        updateUIWithValidationResult(validationResult)

        if (validationResult is ValidationResult.Valid) {
            when (initialThoughtType) {
                InitialThoughtType.RECORDING -> {
                    saveThoughtWithAudio(dtoToSave)
                }
                else -> {
                    thoughtsService.addThought(dtoToSave)
                    finish()
                }
            }
        }
    }

    private suspend fun saveThoughtWithAudio(dto: ThoughtDTO) {
        val recording = recordingHandler?.getCurrentRecording()

        if (recording == null || !recording.fileExists()) {
            Timber.tag(TAG).e("Audio file does not exist")
            tvHexTagsValidationInfo.visibility = View.VISIBLE
            tvHexTagsValidationInfo.text = getString(R.string.validation_recording_missing)
            return
        }

        try {
            // Get actual audio duration from MediaPlayer if possible
            val durationMs = getDurationFromFile(recording.file!!)
            dto.audioDurationMs = durationMs

            thoughtsService.addThoughtWithAudio(dto, recording.file)
            finish()
        }
        catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error saving thought with audio")
            tvHexTagsValidationInfo.visibility = View.VISIBLE
            tvHexTagsValidationInfo.text = getString(R.string.capture_voice_error_saving)
        }
    }

    /**
     * Get audio duration from file using MediaPlayer
     */
    private fun getDurationFromFile(file: File): Long {
        return try {
            android.media.MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
            }.let { player ->
                val duration = player.duration.toLong()
                player.release()
                duration
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error getting duration from file, using file size as fallback")
            file.length() // Fallback to file size
        }
    }

    private fun resetValidationUI() {
        tvHexTagsValidationInfo.visibility = View.GONE
        tvHexTagsValidationInfo.text = null
    }

    private fun updateUIWithValidationResult(result: ValidationResult) {
        if (result is ValidationResult.Error) {
            val validatedProperty = result.refProperty
            when (validatedProperty) {
                ValidatedProperty.T_THREAD,
                ValidatedProperty.T_PROJECT,
                ValidatedProperty.T_SOUL_MATES,
                ValidatedProperty.T_AUDIO -> {
                    tvHexTagsValidationInfo.visibility = View.VISIBLE
                    tvHexTagsValidationInfo.text = result.message
                }
                ValidatedProperty.T_RICH_TEXT -> {
                    // Skipping - RichText has real-time validation
                }
                else -> {
                    // Handle other validation properties if needed
                }
            }
        } else {
            tvHexTagsValidationInfo.visibility = View.GONE
        }
    }

    private fun updateDTOWithHexTags(dtoToUpdate: ThoughtDTO): ThoughtDTO {
        val input = etHexTags.text?.toString().orEmpty()
        val tags = HexTagsUtils.parseInput(input)

        dtoToUpdate.soulMate = tags.soulMate
        dtoToUpdate.project = tags.project
        dtoToUpdate.thread = tags.thread

        return dtoToUpdate
    }

    override fun onDestroy() {
        super.onDestroy()
        // Perform clean-up of recording handler resources
        recordingHandler?.cleanupResources()
        // AudioRecordingView will clean up automatically in onDetachedFromWindow
    }
}