package pl.hexmind.fastnote.features.capture

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import pl.hexmind.fastnote.R
import pl.hexmind.fastnote.features.capture.handlers.TextInputHandler
import pl.hexmind.fastnote.features.capture.handlers.VoiceRecordingHandler
import pl.hexmind.fastnote.features.capture.models.CaptureData
import pl.hexmind.fastnote.features.capture.ui.CaptureViewManager

// Main capturing activity
class ThoughtsCaptureActivity : AppCompatActivity() {

    companion object {
        const val INPUT_TYPE = "input_type"
        const val TYPE_UNKNOWN = "???"
        const val TYPE_NOTE = "rich_text"
        const val TYPE_VOICE = "voice"
        const val TYPE_PHOTO = "photo"
        const val TYPE_DRAWING = "drawing"
    }

    private lateinit var viewManager: CaptureViewManager
    private lateinit var textInputHandler: TextInputHandler
    private lateinit var voiceRecordingHandler: VoiceRecordingHandler
    // Future handlers
    // private lateinit var photoCaptureHandler: PhotoCaptureHandler
    // private lateinit var drawingHandler: DrawingHandler

    private var currentCaptureData = CaptureData()
    private var currentInputType = TYPE_UNKNOWN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.thought_capture_layout)

        currentInputType = intent.getStringExtra(INPUT_TYPE) ?: TYPE_UNKNOWN

        initializeComponents()
        setupMode()
        setupListeners()
    }

    private fun initializeComponents() {
        viewManager = CaptureViewManager(this)
        viewManager.initializeViews()

        textInputHandler = TextInputHandler(viewManager)
        voiceRecordingHandler = VoiceRecordingHandler(this, viewManager)

        // Set up data change listeners
        textInputHandler.setOnDataChangedListener { data ->
            currentCaptureData = currentCaptureData.copy(
                essence = data.essence,
                richText = data.richText
            )
        }

        voiceRecordingHandler.setOnDataChangedListener { data ->
            currentCaptureData = currentCaptureData.copy(
                audioFile = data.audioFile
            )
        }
    }

    private fun setupMode() {
        viewManager.setupModeVisibility(currentInputType)

        when (currentInputType) {
            TYPE_NOTE -> {
                textInputHandler.setupRichTextEditor()
            }
            TYPE_VOICE -> {
                voiceRecordingHandler.requestPermission()
            }
            TYPE_PHOTO -> {
                // photoCaptureHandler.requestPermission()
            }
            TYPE_DRAWING -> {
                // drawingHandler.setupDrawingCanvas()
            }
        }
    }

    private fun setupListeners() {
        textInputHandler.setupTextWatcher()
        voiceRecordingHandler.setupListeners()

        viewManager.btnSave.setOnClickListener {
            saveThought()
        }
    }

    private fun saveThought() {
        // Merge current data from all handlers
        val finalData = currentCaptureData.copy(
            essence = textInputHandler.getCurrentData().essence,
            richText = textInputHandler.getCurrentData().richText,
            audioFile = voiceRecordingHandler.getCurrentData().audioFile,
            inputType = currentInputType
        )

        if (!finalData.isValid()) {
            if (finalData.essence.isEmpty()) {
                Toast.makeText(this, getString(R.string.capture_essence_error_empty), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, getString(R.string.capture_essence_error_too_long), Toast.LENGTH_SHORT).show()
            }
            return
        }

        // TODO: Save to database/storage
        // saveToDatabase(finalData)

        Toast.makeText(this, getString(R.string.capture_main_state_saved), Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceRecordingHandler.cleanup()
        // Future: photoCaptureHandler.cleanup()
        // Future: drawingHandler.cleanup()
    }
}