package pl.hexmind.mindshaper.activities.capture

import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.activities.CoreActivity
import pl.hexmind.mindshaper.activities.capture.handlers.RichTextHandler
import pl.hexmind.mindshaper.activities.ThoughtValidator
import pl.hexmind.mindshaper.activities.capture.handlers.RecordingHandler
import pl.hexmind.mindshaper.activities.capture.models.InitialThoughtType
import pl.hexmind.mindshaper.activities.capture.ui.RichTextCaptureView
import pl.hexmind.mindshaper.activities.capture.ui.RecordingCaptureView
import pl.hexmind.mindshaper.common.regex.convertToWords
import pl.hexmind.mindshaper.common.regex.cutIntoSentences
import pl.hexmind.mindshaper.common.validation.ValidationResult
import pl.hexmind.mindshaper.services.ThoughtsService
import pl.hexmind.mindshaper.services.dto.ThoughtDTO
import javax.inject.Inject

@AndroidEntryPoint
class ThoughtsCaptureActivity : CoreActivity() {

    companion object Params{
        const val P_INIT_THOUGHT_TYPE = "P_EXTRA_INIT_THOUGHT_TYPE"
    }

    @Inject
    lateinit var thoughtValidator: ThoughtValidator
    @Inject
    lateinit var thoughtsService: ThoughtsService

    private var currentThoughtDTO = ThoughtDTO()
    private var currentInputType = InitialThoughtType.UNKNOWN

    private lateinit var flContainerFeatures: FrameLayout
    private lateinit var btnSave: MaterialButton
    private lateinit var etEssence : TextInputEditText

    private lateinit var etThread : TextInputEditText

    private lateinit var tvEssenceWordsInfo : TextView

    private var richTextCaptureView: RichTextCaptureView? = null
    private var richTextHandler: RichTextHandler? = null

    private var recordingCaptureView: RecordingCaptureView? = null
    private var voiceHandler: RecordingHandler? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_thought_capture)
        tvEssenceWordsInfo = findViewById(R.id.tv_essence_words_info)
        etEssence = findViewById(R.id.et_essence)
        etThread = findViewById(R.id.et_thread)

        flContainerFeatures = findViewById(R.id.fl_container_features)
        btnSave = findViewById(R.id.btn_save)

        saveExtrasFromIntent()
        setupMode()
        setupListeners()
        setupUI()
    }

    private fun saveExtrasFromIntent() {
        currentInputType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(P_INIT_THOUGHT_TYPE, InitialThoughtType::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(P_INIT_THOUGHT_TYPE)
        } ?: InitialThoughtType.UNKNOWN
    }

    private fun setupMode() {
        flContainerFeatures.removeAllViews()
        when (currentInputType) {
            InitialThoughtType.RICH_TEXT -> {
                richTextCaptureView = RichTextCaptureView(this)
                flContainerFeatures.addView(richTextCaptureView)
                richTextHandler = RichTextHandler(richTextCaptureView!!, thoughtValidator).apply {
                    setOnDataChangedListener { currentThoughtDTO = it }
                }
            }
            InitialThoughtType.RECORDING -> {
                recordingCaptureView = RecordingCaptureView(this)
                flContainerFeatures.addView(recordingCaptureView)
                voiceHandler = RecordingHandler(this, recordingCaptureView!!).apply {
                    setupListeners()
                    setOnDataChangedListener { currentThoughtDTO = it }
                }
            }
            else -> { /* TODO: next modes */ }
        }
    }

    private fun setupListeners() {
        btnSave.setOnClickListener {
            lifecycleScope.launch { saveThought() }
        }
        etEssence.doAfterTextChanged {
            updateEssenceInfo()
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

    fun setupUI(){
        etEssence.hint = thoughtValidator.getEssenceDefaultTooltip()
    }

    private suspend fun saveThought() {
        val finalData = currentThoughtDTO.copy(
            essence = etEssence.text?.toString().orEmpty(),
            thread = etThread.text?.toString().orEmpty(),
            richText = richTextCaptureView?.getRichText() ?: ""
        )

        val validationResult = thoughtValidator.validateDTO(finalData)
        when (validationResult) {
            is ValidationResult.Error -> { }
            is ValidationResult.Valid -> {
                thoughtsService.addThought(finalData)
                finish()
            }
        }
    }

    fun updateEssenceInfo() {
        val text = etEssence.text.toString()
        when (val validationResult = thoughtValidator.validateEssence(text)){
            is ValidationResult.Error -> {
                tvEssenceWordsInfo.text = validationResult.message
                tvEssenceWordsInfo.setTextColor(ContextCompat.getColor(this, R.color.validation_error))
            }
            is ValidationResult.Valid -> {
                tvEssenceWordsInfo.text = validationResult.message
                tvEssenceWordsInfo.setTextColor(ContextCompat.getColor(this, R.color.validation_success))
            }
        }
    }
}