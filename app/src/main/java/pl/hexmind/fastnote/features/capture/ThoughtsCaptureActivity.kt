package pl.hexmind.fastnote.features.capture

import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import pl.hexmind.fastnote.R
import pl.hexmind.fastnote.features.capture.handlers.TextInputHandler
import pl.hexmind.fastnote.features.capture.handlers.VoiceRecordingHandler
import pl.hexmind.fastnote.features.capture.models.CapturedThought
import pl.hexmind.fastnote.features.capture.models.CapturedThoughtValidator
import pl.hexmind.fastnote.features.capture.models.InitialThoughtType
import pl.hexmind.fastnote.features.capture.ui.CaptureViewManager

// Main capturing activity
class ThoughtsCaptureActivity : AppCompatActivity() {

    companion object {
        const val INPUT_TYPE = "input_type"
    }

    private lateinit var viewManager: CaptureViewManager

    // Handlers
    private lateinit var textInputHandler: TextInputHandler
    private lateinit var voiceRecordingHandler: VoiceRecordingHandler
    // TODO
    // Future handlers
    // private lateinit var photoCaptureHandler: PhotoCaptureHandler
    // private lateinit var drawingHandler: DrawingHandler

    private var currentCapturedThought = CapturedThought()
    private var currentInputType = InitialThoughtType.UNKNOWN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.thought_capture_layout)
        initializeFromIntent()
        initializeComponents()
        setupMode()
        setupListeners()
    }

    private fun initializeFromIntent() {
        currentInputType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(INPUT_TYPE, InitialThoughtType::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(INPUT_TYPE)
        } ?: InitialThoughtType.UNKNOWN
    }

    private fun initializeComponents() {
        viewManager = CaptureViewManager(this)
        viewManager.initializeViews()

        textInputHandler = TextInputHandler(viewManager)
        voiceRecordingHandler = VoiceRecordingHandler(this, viewManager)

        // Set up data change listeners
        textInputHandler.setOnDataChangedListener { data ->
            currentCapturedThought = currentCapturedThought.copy(
                essence = data.essence,
                richText = data.richText
            )
        }

        voiceRecordingHandler.setOnDataChangedListener { data ->
            currentCapturedThought = currentCapturedThought.copy(
                audioFile = data.audioFile
            )
        }
    }

    private fun setupMode() {
        viewManager.setupModeVisibility(currentInputType)

        when (currentInputType) {
            InitialThoughtType.NOTE -> {
                textInputHandler.setupRichTextEditor()
            }
            InitialThoughtType.VOICE -> {
                voiceRecordingHandler.requestPermission()
            }
            InitialThoughtType.PHOTO -> {
                // photoCaptureHandler.requestPermission()
            }
            InitialThoughtType.DRAWING -> {
                // drawingHandler.setupDrawingCanvas()
            }
            InitialThoughtType.UNKNOWN -> {
                // TODO: co wtedy?
            }
        }
    }

    private fun setupListeners() {
        textInputHandler.setupTextWatcher()
        voiceRecordingHandler.setupListeners()
        viewManager.btnSave.setOnClickListener { saveThought() }
    }

    private fun saveThought() {
        // Merge current data from all handlers
        val finalData = currentCapturedThought.copy(
            essence = textInputHandler.getCurrentData().essence,
            richText = textInputHandler.getCurrentData().richText,
            audioFile = voiceRecordingHandler.getCurrentData().audioFile,
            initialThoughtType = currentInputType
        )

        val validationResult = CapturedThoughtValidator.validate(finalData)
        when(validationResult){
            is CapturedThoughtValidator.ValidationResult.Error -> return
            is CapturedThoughtValidator.ValidationResult.Valid -> performSaving()
        }

        finish()
    }

    private fun performSaving(){
        // TODO: Save to DB
        Toast.makeText(this, getString(R.string.capture_main_state_saved), Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceRecordingHandler.cleanup()
        // Future: photoCaptureHandler.cleanup()
        // Future: drawingHandler.cleanup()
    }
}