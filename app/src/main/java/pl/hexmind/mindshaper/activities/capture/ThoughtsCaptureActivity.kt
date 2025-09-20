package pl.hexmind.mindshaper.activities.capture

import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.activities.capture.handlers.NoteInputHandler
import pl.hexmind.mindshaper.activities.capture.handlers.ThoughtValidator
import pl.hexmind.mindshaper.activities.capture.handlers.VoiceRecordingHandler
import pl.hexmind.mindshaper.activities.capture.models.InitialThoughtType
import pl.hexmind.mindshaper.activities.capture.ui.NoteCaptureView
import pl.hexmind.mindshaper.activities.capture.ui.VoiceCaptureView
import pl.hexmind.mindshaper.activities.main.CoreActivity
import pl.hexmind.mindshaper.common.regex.convertToWords
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

    private var noteCaptureView: NoteCaptureView? = null
    private var noteInputHandler: NoteInputHandler? = null

    private var voiceCaptureView: VoiceCaptureView? = null
    private var voiceHandler: VoiceRecordingHandler? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_thought_capture)
        tvEssenceWordsInfo = findViewById(R.id.tv_essence_words_info)
        etEssence = findViewById(R.id.et_essence)
        etThread = findViewById(R.id.et_thread)

        flContainerFeatures = findViewById(R.id.fl_container_features)
        btnSave = findViewById(R.id.btn_save)

        initializeFromIntent()
        setupMode()
        setupListeners()
        setupUI()
    }

    private fun initializeFromIntent() {
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
            InitialThoughtType.NOTE -> {
                noteCaptureView = NoteCaptureView(this)
                flContainerFeatures.addView(noteCaptureView)
                noteInputHandler = NoteInputHandler(noteCaptureView!!, thoughtValidator).apply {
                    setOnDataChangedListener { currentThoughtDTO = it }
                }
            }
            InitialThoughtType.VOICE -> {
                voiceCaptureView = VoiceCaptureView(this)
                flContainerFeatures.addView(voiceCaptureView)
                voiceHandler = VoiceRecordingHandler(this, voiceCaptureView!!).apply {
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
        } // TODO: Move to validator part of this logic
        etThread.doAfterTextChanged { editable ->
            editable?.let {
                val words = it.toString().convertToWords()
                if (words.size > ThoughtValidator.THREAD_MAX_WORDS) {
                    val limited = words.take(ThoughtValidator.THREAD_MAX_WORDS).joinToString(" ")
                    etThread.setText(limited)
                    etThread.setSelection(limited.length)
                }
            }
        }
    }

    fun setupUI(){
        etEssence.hint = this.getString(R.string.capture_essence_tooltip, 16)
    }

    private suspend fun saveThought() {
        val finalData = currentThoughtDTO.copy(
            essence = etEssence.text?.toString().orEmpty(),
            thread = etThread.text?.toString().orEmpty(),
            richText = noteCaptureView?.getRichText()
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