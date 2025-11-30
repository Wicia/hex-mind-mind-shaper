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
import pl.hexmind.mindshaper.activities.capture.handlers.RecordingCaptureHandler
import pl.hexmind.mindshaper.activities.capture.handlers.RecordingCaptureView
import pl.hexmind.mindshaper.activities.capture.handlers.RichTextCaptureHandler
import pl.hexmind.mindshaper.activities.capture.handlers.RichTextCaptureView
import pl.hexmind.mindshaper.activities.capture.models.InitialThoughtType
import pl.hexmind.mindshaper.common.regex.HexTagsUtils
import pl.hexmind.mindshaper.common.validation.ValidatedProperty
import pl.hexmind.mindshaper.common.validation.ValidationResult
import pl.hexmind.mindshaper.services.ThoughtsService
import pl.hexmind.mindshaper.services.dto.ThoughtDTO
import pl.hexmind.mindshaper.services.validators.ThoughtValidator
import javax.inject.Inject

@AndroidEntryPoint
class CaptureActivity : CoreActivity() {

    companion object Params {
        const val P_INIT_THOUGHT_TYPE = "P_EXTRA_INIT_THOUGHT_TYPE"
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
            }
            InitialThoughtType.RECORDING -> {
                val recordingCaptureView = RecordingCaptureView(this)
                flContainerFeatures.addView(recordingCaptureView)
                val handler = RecordingCaptureHandler(
                    activity = this,
                    view = recordingCaptureView,
                    validator = thoughtValidator
                ).apply {
                    setupListeners()
                }
                thoughtCaptureHandler = handler
                recordingHandler = handler
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
        val audioFile = recordingHandler?.getTempAudioFile()

        if (audioFile == null || !audioFile.exists()) {
            throw IllegalArgumentException("Plik audio nie istnieje")
        }

        try {
            dto.audioDurationMs = audioFile.length()
            thoughtsService.addThoughtWithAudio(dto, audioFile)
            finish()
        }
        catch (e: Exception) {
            e.printStackTrace()
            tvHexTagsValidationInfo.visibility = View.VISIBLE
            tvHexTagsValidationInfo.text = getString(R.string.capture_voice_error_saving)
        }
    }

    private fun resetValidationUI() {
        tvHexTagsValidationInfo.visibility = View.GONE
        tvHexTagsValidationInfo.text = null
    }

    private fun updateUIWithValidationResult(result: ValidationResult) {
        if (result is ValidationResult.Error) {
            val validatedProperty = result.refProperty
            if (validatedProperty == ValidatedProperty.T_THREAD) {
                tvHexTagsValidationInfo.visibility = View.VISIBLE
                tvHexTagsValidationInfo.text = result.message
            } else if (validatedProperty == ValidatedProperty.T_PROJECT) {
                tvHexTagsValidationInfo.visibility = View.VISIBLE
                tvHexTagsValidationInfo.text = result.message
            } else if (validatedProperty == ValidatedProperty.T_SOUL_MATES) {
                tvHexTagsValidationInfo.visibility = View.VISIBLE
                tvHexTagsValidationInfo.text = result.message
            } else if (validatedProperty == ValidatedProperty.T_RICH_TEXT) {
                // Skipping - already RichText has real time validation
            }
            else if (validatedProperty == ValidatedProperty.T_AUDIO) {
                tvHexTagsValidationInfo.visibility = View.VISIBLE
                tvHexTagsValidationInfo.text = result.message
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
        recordingHandler?.cleanup()
    }
}