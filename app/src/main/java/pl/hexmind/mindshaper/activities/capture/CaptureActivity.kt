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
import pl.hexmind.mindshaper.activities.capture.ThoughtCaptureHandler
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

    private var thoughtToSaveDTO = ThoughtDTO() // TODO: Maybe remove this state-keeping var?

    private lateinit var flContainerFeatures: FrameLayout
    private lateinit var btnSave: MaterialButton

    private lateinit var etThread : TextInputEditText

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
        etThread = findViewById(R.id.et_thread)
        flContainerFeatures = findViewById(R.id.fl_container_features)
        btnSave = findViewById(R.id.btn_save)
    }

    private fun saveExtrasFromIntent() {
        thoughtToSaveDTO.initialThoughtType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(P_INIT_THOUGHT_TYPE, InitialThoughtType::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(P_INIT_THOUGHT_TYPE)
        } ?: InitialThoughtType.UNKNOWN
    }

    private fun setupMode() {
        flContainerFeatures.removeAllViews()
        when (thoughtToSaveDTO.initialThoughtType) {
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
        etThread.doAfterTextChanged { editable ->
            editable?.let {
                val words = it.toString().convertToWords()
                if (thoughtValidator.validateThread(it.toString()) is ValidationResult.Error) {
                    val limited = words.take(ThoughtValidator.THREAD_MAX_WORDS).joinToString(" ")
                    etThread.setText(limited)
                    etThread.setSelection(limited.length)
                }
            }
        }
    }

    private suspend fun saveThought() {
        val result = thoughtCaptureHandler.performValidation()
        if(result is ValidationResult.Error){
            return
        }
        var dtoToSave = thoughtToSaveDTO.copy(
            thread = etThread.text?.toString().orEmpty(),
        )
        dtoToSave = thoughtCaptureHandler.getUpdatedDTO(dtoToSave)

        thoughtsService.addThought(dtoToSave)
        finish()
    }
}