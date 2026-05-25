package pl.hexmind.mindshaper.activities.capture

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.activities.ThoughtManagerActivity
import pl.hexmind.mindshaper.common.onboarding.OnboardingProgressStep
import pl.hexmind.mindshaper.common.regex.HexTagsUtils
import pl.hexmind.mindshaper.common.ui.dialogs.PhotoFullscreenDialog
import pl.hexmind.mindshaper.common.ui.dialogs.TextEditDialog
import pl.hexmind.mindshaper.common.ui.views.content.HexAudioView
import pl.hexmind.mindshaper.common.ui.views.content.HexPhotoView
import pl.hexmind.mindshaper.common.ui.views.content.HexTextView
import pl.hexmind.mindshaper.common.validation.ValidationResult
import pl.hexmind.mindshaper.common.validation.resolveMessage
import pl.hexmind.mindshaper.databinding.CaptureActivityBinding
import pl.hexmind.mindshaper.services.ThoughtsService
import pl.hexmind.mindshaper.services.dto.DefaultCaptureForm
import pl.hexmind.mindshaper.services.dto.ThoughtDTO
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class CaptureActivity : ThoughtManagerActivity() {

    private val viewModel: CaptureActivityViewModel by viewModels()
    private lateinit var binding: CaptureActivityBinding

    @Inject
    lateinit var thoughtsService: ThoughtsService

    private var currentPhotoUri: Uri? = null

    private val takePhotoLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && currentPhotoUri != null) {
            handlePhotoResult(currentPhotoUri!!)
        }
    }

    private val pickPhotoLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { handlePhotoResult(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = CaptureActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupListeners()
        setupObservers()

        onboardingManager.showTooltipForStep(
            OnboardingProgressStep.CAPTURE_ENTRY_TOOLTIP, this
        )
    }

    private var defaultFormAutoOpened = false

    private fun setupObservers() {
        // Observe draft changes (in memory, NOT from DB)
        viewModel.draftThought.observe(this) { draft ->
            updateUI(draft)
            // Auto-open only once, AFTER first updateUI resets widget states
            if (!defaultFormAutoOpened) {
                defaultFormAutoOpened = true
                autoOpenDefaultFormIfNeeded()
            }
        }
    }

    private fun setupListeners() {
        binding.apply {

            // Onboarding :)
            // ! tapping first time
            etHexTags.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    onboardingManager.showTooltipForStep(
                        OnboardingProgressStep.CAPTURE_HEXTAGS_TOOLTIP,
                        this@CaptureActivity
                    )
                }
            }

            // SAVE BUTTON
            btnSave.setOnClickListener {
                lifecycleScope.launch {
                    saveDraft()
                }
            }

            // RICH TEXT
            btnRichTextAdd.setOnClickListener {
                showEditRichTextDialog()
            }

            // RECORDING
            btnRecordingAdd.setOnClickListener {
                if (!appSettingsStorage.isVoiceRecordingEnabled()) {
                    showEnableAdditionalFeaturesDialog()
                    return@setOnClickListener
                }

                btnRecordingAdd.visibility = View.GONE
                audioRecordingPlayback.visibility = View.VISIBLE
                audioRecordingPlayback.switchToRecordPlaybackMode()
                audioRecordingPlayback.showStatus(
                    getString(R.string.capture_voice_tooltip),
                    R.color.validation_success
                )
            }

            // PHOTO
            btnPhotoAdd.setOnClickListener {
                if (!appSettingsStorage.isPhotoFeatureEnabled()) {
                    showEnableAdditionalFeaturesDialog()
                    return@setOnClickListener
                }

                btnPhotoAdd.visibility = View.GONE
                photoDisplayView.visibility = View.VISIBLE
                photoDisplayView.showStatus(
                    R.string.photos_no_file,
                    R.color.validation_success
                )
            }

            // PHOTO CALLBACKS
            photoDisplayView.setCallback(object : HexPhotoView.PhotoCallback {
                override fun onCameraCaptureRequested() {
                    takePhoto()
                }

                override fun onGalleryPickRequested() {
                    pickFromGallery()
                }

                override fun onPhotoDeleted() {
                    viewModel.deletePhoto()
                    // Hide widget
                    btnPhotoAdd.visibility = View.VISIBLE
                    photoDisplayView.visibility = View.GONE
                }

                override fun onPhotoClicked() {
                    showFullscreenPhoto()
                }

                override fun onError(error: String) {
                    Toast.makeText(this@CaptureActivity, error, Toast.LENGTH_SHORT).show()
                }
            })

            // AUDIO CALLBACKS
            audioRecordingPlayback.setCallback(object : HexAudioView.RecordingCallback {
                override fun onRecordingStarted() {
                    // Clear validation messages
                    tvHexTagsValidationInfo.text = ""
                }

                override fun onRecordingStopped(file: File, durationMs: Long) {
                    // Store temporarily (NOT in DB)
                    viewModel.saveAudioRecording(file, durationMs)
                }

                override fun onRecordingDeleted() {
                    viewModel.deleteAudioRecording()
                    // Hide widget
                    btnRecordingAdd.visibility = View.VISIBLE
                    audioRecordingPlayback.visibility = View.GONE
                }

                override fun onRecordingError(error: String) {
                    Toast.makeText(this@CaptureActivity, error, Toast.LENGTH_SHORT).show()
                }

                override fun onPlaybackStarted() {}
                override fun onPlaybackStopped() {}
                override fun onPermissionRequired() {}
            })

            // RICH TEXT CALLBACKS
            richTextView.setCallback(object : HexTextView.TextCallback {
                override fun onTextClicked() {
                    showEditRichTextDialog()
                }

                override fun onTextDeleted() {
                    viewModel.deleteRichText()
                    // Hide widget
                    btnRichTextAdd.visibility = View.VISIBLE
                    richTextView.visibility = View.GONE
                }
            })
        }
    }

    private fun showFullscreenPhoto() {
        val photoUri = viewModel.getTempPhotoUri() ?: return

        lifecycleScope.launch {
            try {
                val photoFile = thoughtsService.getFileFromUri(photoUri)
                if (photoFile != null) {
                    val photoBytes = photoFile.readBytes()
                    PhotoFullscreenDialog(this@CaptureActivity, photoBytes).show()
                }
            }
            catch (e: Exception) {
                Toast.makeText(
                    this@CaptureActivity,
                    "Failed to load photo",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * Auto-opens the default capture dialog / widget based on user's preference in Settings.
     */
    private fun autoOpenDefaultFormIfNeeded() {
        when (appSettingsStorage.getDefaultCaptureForm()) {
            DefaultCaptureForm.TEXT  -> showEditRichTextDialog()
            DefaultCaptureForm.VOICE -> autoOpenVoiceRecording()
            DefaultCaptureForm.PHOTO -> autoOpenPhoto()
        }
    }

    /**
     * Expands the recording widget as if the user tapped the add-recording button.
     */
    private fun autoOpenVoiceRecording() {
        if (!appSettingsStorage.isVoiceRecordingEnabled()) {
            showEnableAdditionalFeaturesDialog()
            return
        }

        binding.btnRecordingAdd.visibility = View.GONE
        binding.audioRecordingPlayback.visibility = View.VISIBLE
        binding.audioRecordingPlayback.switchToRecordPlaybackMode()
        binding.audioRecordingPlayback.showStatus(
            getString(R.string.capture_voice_tooltip),
            R.color.validation_success
        )
    }

    /**
     * Expands the photo widget as if the user tapped the add-photo button.
     */
    private fun autoOpenPhoto() {
        if (!appSettingsStorage.isPhotoFeatureEnabled()) {
            showEnableAdditionalFeaturesDialog()
            return
        }

        binding.btnPhotoAdd.visibility = View.GONE
        binding.photoDisplayView.visibility = View.VISIBLE
        binding.photoDisplayView.showStatus(
            R.string.photos_no_file,
            R.color.validation_success
        )
    }

    private fun setupUI() {
        setupHeader(R.drawable.ic_capture_thought, R.string.capture_main_label)
        updateAddPhotoButtonVisualState()
        updateAddRecordingButtonVisualState()
    }

    private fun showEditRichTextDialog() {
        val draft = viewModel.draftThought.value ?: return
        val currentText = draft.richText ?: ""

        TextEditDialog(
            context = this,
            title = getString(R.string.common_edit_note_header),
            textInput = currentText,
            notesStyle = true,
            onSave = { newText ->
                viewModel.updateRichText(newText)
            }
        ).show()
    }

    private fun takePhoto() {
        val photoUri = viewModel.createPhotoUri()
        currentPhotoUri = photoUri
        takePhotoLauncher.launch(photoUri)
    }

    private fun pickFromGallery() {
        pickPhotoLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    private fun handlePhotoResult(uri: Uri) {
        lifecycleScope.launch {
            try {
                timber.log.Timber.d("Photo result: uri=$uri")
                binding.photoDisplayView.showLoading()

                // Save URI to ViewModel (NOT to DB)
                viewModel.savePhotoUri(uri)

                // Load photo for preview display with EXIF rotation
                withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val photoFile = thoughtsService.getFileFromUri(uri)

                    if (photoFile != null) {
                        // Load with EXIF rotation applied
                        val photoBytes = thoughtsService.loadPhotoForPreview(photoFile)
                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                            binding.photoDisplayView.loadPhoto(photoBytes)
                        }
                    }
                    else {
                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                            binding.photoDisplayView.showError("Failed to load photo")
                        }
                    }
                }
            }
            catch (e: Exception) {
                timber.log.Timber.e(e, "Error handling photo result")
                binding.photoDisplayView.showError("Olaboga! Kod się wyburaczył :/")
            }
        }
    }

    private fun updateAddPhotoButtonVisualState() {
        updateAddButtonVisualState(
            binding.btnPhotoAdd,
            appSettingsStorage.isPhotoFeatureEnabled()
        )
    }

    private fun updateAddRecordingButtonVisualState() {
        updateAddButtonVisualState(
            binding.btnRecordingAdd,
            appSettingsStorage.isVoiceRecordingEnabled()
        )
    }

    /**
     * Save draft to database
     */
    private suspend fun saveDraft() {
        // Reset validation UI
        binding.tvHexTagsValidationInfo.text = ""

        // Parse hex tags from EditText
        val hexTagsInput = binding.etHexTags.text?.toString().orEmpty()
        val tags = HexTagsUtils.parseInput(hexTagsInput)

        // Update draft with hex tags
        viewModel.updateHexTags(
            subject = tags.subject,
            project = tags.project,
            soulMate = tags.soulMate
        )

        // Validate
        val validationResult = viewModel.validate()

        // Show validation error
        if (validationResult is ValidationResult.Error) {
            binding.tvHexTagsValidationInfo.text = validationResult.resolveMessage(this)
            return
        }

        // Save to DB
        val result = viewModel.saveNewThought()

        if (result.isSuccess) {
            // Success — return new thoughtId
            val thoughtId = result.getOrNull()
            if (thoughtId != null) {
                val data = android.content.Intent().putExtra(EXTRA_THOUGHT_ID, thoughtId)
                setResult(RESULT_OK, data)
            }
            finish()
        }
        else {
            // Error - show message
            binding.tvHexTagsValidationInfo.text = result.exceptionOrNull()?.message
                ?: getString(R.string.capture_voice_error_saving)
        }
    }

    private fun updateUI(draft: ThoughtDTO) {
        updateRichTextUI(draft)
        updateAudioUI(draft)
        updatePhotoUI(draft)
    }

    private fun updateRichTextUI(draft: ThoughtDTO) {
        if (draft.richText.isNullOrBlank()) {
            binding.btnRichTextAdd.visibility = View.VISIBLE
            binding.richTextView.visibility = View.GONE
        }
        else {
            binding.btnRichTextAdd.visibility = View.GONE
            binding.richTextView.visibility = View.VISIBLE
            binding.richTextView.originalText = draft.richText.orEmpty()
        }
    }

    /**
     * UI-related manager for recording and playing sound recordings
     */
    private fun updateAudioUI(draft: ThoughtDTO) {
        val featureEnabled = appSettingsStorage.isVoiceRecordingEnabled()
        updateAddRecordingButtonVisualState()

        // Feature disabled
        if (!featureEnabled) {
            binding.btnRecordingAdd.visibility = View.VISIBLE // always show disabled button
            binding.audioRecordingPlayback.visibility = View.GONE
            return
        }

        // Feature enabled
        if (draft.hasAudio) {
            binding.btnRecordingAdd.visibility = View.GONE
            binding.audioRecordingPlayback.visibility = View.VISIBLE

            // Don't reload if widget already has the recording
            // (prevents resetting visualizer after recording)
            val currentRecording = binding.audioRecordingPlayback.getCurrentRecording()
            if (currentRecording.file == null) {
                // Widget doesn't have recording yet - load it
                viewModel.getTempAudioFile()?.let { audioFile ->
                    binding.audioRecordingPlayback.loadAudioForPlayback(audioFile)
                }
            }
        }
        else {
            binding.btnRecordingAdd.visibility = View.VISIBLE
            binding.audioRecordingPlayback.visibility = View.GONE
        }
    }

    /**
     * UI-related manager for making and displaying photo
     */
    private fun updatePhotoUI(draft: ThoughtDTO) {
        val featureEnabled = appSettingsStorage.isPhotoFeatureEnabled()
        updateAddPhotoButtonVisualState()

        // Feature disabled
        if (!featureEnabled) {
            binding.btnPhotoAdd.visibility = View.VISIBLE // always show disabled button
            binding.photoDisplayView.visibility = View.GONE
            return
        }

        // Feature enabled
        if (draft.hasPhoto) {
            binding.btnPhotoAdd.visibility = View.GONE
            binding.photoDisplayView.visibility = View.VISIBLE
            // Photo already loaded in handlePhotoResult
        }
        else {
            binding.btnPhotoAdd.visibility = View.VISIBLE
            binding.photoDisplayView.visibility = View.GONE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.audioRecordingPlayback.cleanupResources()
    }

    companion object {
        const val EXTRA_THOUGHT_ID = "EXTRA_THOUGHT_ID"
    }
}