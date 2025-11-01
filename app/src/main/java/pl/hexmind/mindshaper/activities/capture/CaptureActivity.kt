package pl.hexmind.mindshaper.activities.capture

import android.os.Build
import android.os.Bundle
import android.widget.FrameLayout
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.activities.CoreActivity
import pl.hexmind.mindshaper.activities.capture.handlers.RichTextCaptureHandler
import pl.hexmind.mindshaper.services.validators.ThoughtValidator
import pl.hexmind.mindshaper.activities.capture.handlers.RecordingCaptureHandler
import pl.hexmind.mindshaper.activities.capture.models.InitialThoughtType
import pl.hexmind.mindshaper.activities.capture.handlers.RichTextCaptureView
import pl.hexmind.mindshaper.activities.capture.handlers.RecordingCaptureView
import pl.hexmind.mindshaper.common.regex.convertToWords
import pl.hexmind.mindshaper.common.validation.ValidationResult
import pl.hexmind.mindshaper.services.ThoughtsService
import pl.hexmind.mindshaper.services.dto.ThoughtDTO
import javax.inject.Inject

@AndroidEntryPoint
class CaptureActivity : CoreActivity() {

    companion object Params{
        const val P_INIT_THOUGHT_TYPE = "P_EXTRA_INIT_THOUGHT_TYPE"
    }

    @Inject
    lateinit var thoughtValidator: ThoughtValidator

    @Inject
    lateinit var thoughtsService: ThoughtsService

    private var initialThoughtType : InitialThoughtType = InitialThoughtType.UNKNOWN
    private lateinit var flContainerFeatures: FrameLayout
    private lateinit var btnSave: MaterialButton

    private lateinit var etHexTags : TextInputEditText

    // HANDLER for specific input/thought type
    private lateinit var thoughtCaptureHandler: ThoughtCaptureHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.capture_activity)
        initViews()
        saveExtrasFromIntent()
        setupListeners()
        setupMode()
    }

    private fun initViews(){
        etHexTags = findViewById(R.id.et_hex_tags)
        flContainerFeatures = findViewById(R.id.fl_container_features)
        btnSave = findViewById(R.id.btn_save)
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
            }
            InitialThoughtType.RECORDING -> {
                val recordingCaptureView = RecordingCaptureView(this)
                flContainerFeatures.addView(recordingCaptureView)
                thoughtCaptureHandler = RecordingCaptureHandler(this, recordingCaptureView).apply {
                    setupListeners()
                }
            }
            else -> { /* TODO: next modes */ }
        }
    }

    private fun setupListeners() {
        btnSave.setOnClickListener {
            lifecycleScope.launch { saveThought() }
        }

        // TODO: Rethink it and fix => there is a small bug below :)
        etHexTags.doAfterTextChanged { editable ->
            editable?.let {
                val words = it.toString().convertToWords()
                if (thoughtValidator.validateThread(it.toString()) is ValidationResult.Error) {
                    val limited = words.take(ThoughtValidator.THREAD_MAX_WORDS).joinToString(" ")
                    etHexTags.setText(limited)
                    etHexTags.setSelection(limited.length)
                }
            }
        }
    }

    private suspend fun saveThought() {
        val result = thoughtCaptureHandler.performValidation()
        if(result is ValidationResult.Error){
            return
        }
        var dtoToSave = ThoughtDTO()
        dtoToSave = thoughtCaptureHandler.getUpdatedDTO(dtoToSave)
        dtoToSave = updateDTOWithHexTags(dtoToSave)

        thoughtsService.addThought(dtoToSave)
        finish()
    }

    private fun updateDTOWithHexTags(dtoToUpdate : ThoughtDTO) : ThoughtDTO{
        val input = etHexTags.text?.toString().orEmpty()

        val soulMateRegex = Regex("@(\\S+)")
        val projectRegex = Regex("#(\\S+)")

        val soulMate = soulMateRegex.find(input)?.groupValues?.get(1)
        val project = projectRegex.find(input)?.groupValues?.get(1)

        val thread = input
            .replace(soulMateRegex, "")
            .replace(projectRegex, "")
            .trim()

        dtoToUpdate.soulMate = soulMate
        dtoToUpdate.project = project
        dtoToUpdate.thread = thread

        return dtoToUpdate
    }
}