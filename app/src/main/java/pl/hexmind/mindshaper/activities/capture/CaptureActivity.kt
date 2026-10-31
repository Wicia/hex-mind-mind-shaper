package pl.hexmind.mindshaper.activities.capture

import android.net.Uri
import android.os.Bundle
import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.activities.ThoughtManagerActivity
import pl.hexmind.mindshaper.common.ui.AddFormButtons
import pl.hexmind.mindshaper.common.onboarding.OnboardingTask
import pl.hexmind.mindshaper.common.onboarding.OnboardingTaskGuide
import pl.hexmind.mindshaper.common.onboarding.OnboardingTaskPoint
import pl.hexmind.mindshaper.common.regex.HexTagsUtils
import pl.hexmind.mindshaper.common.ui.dialogs.PhotoFullscreenDialog
import pl.hexmind.mindshaper.common.ui.dialogs.TextEditDialog
import pl.hexmind.mindshaper.common.ui.views.HexSpotlight
import pl.hexmind.mindshaper.common.ui.views.content.HexAudioView
import pl.hexmind.mindshaper.common.ui.views.content.HexPhotoView
import pl.hexmind.mindshaper.common.ui.views.content.HexTextView
import pl.hexmind.mindshaper.common.validation.ValidationResult
import pl.hexmind.mindshaper.common.validation.resolveMessage
import pl.hexmind.mindshaper.databinding.CaptureActivityBinding
import pl.hexmind.mindshaper.database.models.HexTagType
import pl.hexmind.mindshaper.services.HexTagsService
import pl.hexmind.mindshaper.services.SimilarTag
import pl.hexmind.mindshaper.common.ui.dialogs.ActionsDialog
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

    @Inject
    lateinit var hexTagsService: HexTagsService

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
        setupBackHandling()
    }

    private var defaultFormAutoOpened = false

    // Capture thought task guided from Stream only - not from Home or a launcher shortcut
    private val isTaskGuided by lazy {
        intent.getBooleanExtra(EXTRA_ONBOARDING_GUIDE, false) && onboardingManager.isGuiding(OnboardingTask.CAPTURE_THOUGHT)
    }

    private fun setupObservers() {
        // Observe draft changes (in memory, NOT from DB)
        viewModel.draftThought.observe(this) { draft ->
            updateUI(draft)
            // Auto-open only once, AFTER first updateUI resets widget states
            // ! Guided task = always the note, whatever the default form
            if (!defaultFormAutoOpened) {
                defaultFormAutoOpened = true
                if (isTaskGuided) showEditRichTextDialog() else autoOpenDefaultFormIfNeeded()
            }
        }
    }

    // ========== CAPTURE THOUGHT TASK ==========

    // Two steps after the note: the subject, then Save - closing the second one puts the cursor in the subject
    private fun showSubjectAndSaveGuide() {
        val task = OnboardingTask.CAPTURE_THOUGHT
        if (isFinishing || isDestroyed || !onboardingManager.shouldShowTaskGuide(task, OnboardingTaskGuide.SUBJECT_AND_SAVE)) return

        onboardingManager.markTaskGuideShown(task, OnboardingTaskGuide.SUBJECT_AND_SAVE)
        HexSpotlight.show(
            activity   = this,
            target     = binding.etSubject,
            text       = getString(R.string.capture_onboarding_task_subject),
            buttonText = getString(R.string.common_spotlight_btn_next),
            step       = 1,
            stepCount  = SUBJECT_AND_SAVE_GUIDE_STEPS,
            holeShape  = HexSpotlight.HoleShape.ROUNDED_RECT,
            // ! After the first one faded out - two dims at once would flash darker
            onClosed   = { window.decorView.postDelayed({ showSaveGuide() }, HexSpotlight.FADE_DURATION_MS) }
        ) {}
    }

    private fun showSaveGuide() {
        if (isFinishing || isDestroyed) return

        HexSpotlight.show(
            activity   = this,
            target     = binding.btnSave,
            text       = getString(R.string.capture_onboarding_task_save),
            buttonText = getString(R.string.common_spotlight_btn_got_it),
            step       = 2,
            stepCount  = SUBJECT_AND_SAVE_GUIDE_STEPS,
            // Tap on Save in the hole does not save - the subject comes first
            onClosed   = { focusSubject() }
        ) {}
    }

    private fun focusSubject() {
        binding.etSubject.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.etSubject, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun setupListeners() {
        binding.apply {

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
                    showEnableVoiceFormDialog()
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
                    showEnablePhotoFormDialog()
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

    /**
     * System back - a draft with any form asks before it's dropped, an empty one just closes
     */
    private fun setupBackHandling() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // An ongoing recording is kept - it counts as a form like a finished one
                binding.audioRecordingPlayback.stopRecordingIfActive()

                if (!viewModel.hasContent()) {
                    finish()
                    return
                }

                ActionsDialog.Builder(this@CaptureActivity)
                    .setTitle(getString(R.string.capture_exit_dialog_title))
                    .setDescription(getString(R.string.capture_exit_dialog_description))
                    .setSecondaryAction(getString(R.string.capture_exit_dialog_discard)) {
                        finish()
                    }
                    .setPrimaryAction(getString(R.string.capture_exit_dialog_save)) {
                        lifecycleScope.launch { saveDraft() }
                    }
                    .show()
            }
        })
    }

    private fun showFullscreenPhoto() {
        val photoUri = viewModel.getTempPhotoUri() ?: return

        lifecycleScope.launch {
            try {
                // Raw camera file keeps its rotation only in EXIF - the viewer gets it already applied,
                // at the size the saved photo will have
                val photoBytes = withContext(kotlinx.coroutines.Dispatchers.IO) {
                    thoughtsService.getFileFromUri(photoUri)
                        ?.let { thoughtsService.loadPhotoForPreview(it, maxSize = 1920) }
                }
                if (photoBytes != null) {
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
        // A launcher shortcut names the form explicitly; without it we fall back to the stored default
        val requestedForm = intent.getStringExtra(EXTRA_CAPTURE_FORM)
            ?.let { formName -> runCatching { DefaultCaptureForm.valueOf(formName) }.getOrNull() }

        when (requestedForm ?: appSettingsStorage.getDefaultCaptureForm()) {
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
            showEnableVoiceFormDialog()
            return
        }

        binding.btnRecordingAdd.visibility = View.GONE
        binding.audioRecordingPlayback.visibility = View.VISIBLE
        binding.audioRecordingPlayback.switchToRecordPlaybackMode()
        // Start recording right away - default voice form means the user wants to record now
        binding.audioRecordingPlayback.startRecordingProgrammatically()
    }

    /**
     * Opens the photo widget and launches the camera immediately,
     * as if the user tapped add-photo and then the shutter.
     */
    private fun autoOpenPhoto() {
        if (!appSettingsStorage.isPhotoFeatureEnabled()) {
            showEnablePhotoFormDialog()
            return
        }

        binding.btnPhotoAdd.visibility = View.GONE
        binding.photoDisplayView.visibility = View.VISIBLE
        // Launch the camera right away - default photo form means the user wants to shoot now.
        // Photo feature can only be ON when camera permission is granted, so takePhoto() is safe here.
        takePhoto()
    }

    private fun setupUI() {
        setupHeader(R.drawable.ic_capture_thought, R.string.capture_main_label)
        AddFormButtons.keepWidth(binding.btnRichTextAdd)
        updateAddPhotoButtonVisualState()
        updateAddRecordingButtonVisualState()
    }

    private fun showEditRichTextDialog() {
        val draft = viewModel.draftThought.value ?: return
        val currentText = draft.richText ?: ""

        // Edit header only when there is a note to edit
        val titleRes = if (currentText.isBlank()) R.string.common_new_note_header else R.string.common_edit_note_header

        // Guided task: the note shows the bullet button, saving it leads on to the subject and Save - each once per run
        val task = OnboardingTask.CAPTURE_THOUGHT
        val showBulletGuide = isTaskGuided && onboardingManager.shouldShowTaskGuide(task, OnboardingTaskGuide.NOTE_BULLET)

        val dialog = TextEditDialog(
            context = this,
            title = getString(titleRes),
            textInput = currentText,
            notesStyle = true,
            onSave = { newText ->
                viewModel.updateRichText(newText)
                // ! After the dialog and its keyboard are gone - the hole must land on the laid out screen
                if (isTaskGuided && newText.isNotBlank()) {
                    window.decorView.postDelayed({ showSubjectAndSaveGuide() }, GUIDE_AFTER_DIALOG_DELAY_MS)
                }
            }
        )
        if (showBulletGuide) {
            onboardingManager.markTaskGuideShown(task, OnboardingTaskGuide.NOTE_BULLET)
            dialog.showWithBulletSpotlight(
                text       = getString(R.string.capture_onboarding_task_bullet),
                buttonText = getString(R.string.common_spotlight_btn_got_it)
            )
        }
        else {
            dialog.show()
        }
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
        AddFormButtons.applyState(
            binding.btnPhotoAdd,
            appSettingsStorage.isPhotoFeatureEnabled()
        )
    }

    private fun updateAddRecordingButtonVisualState() {
        AddFormButtons.applyState(
            binding.btnRecordingAdd,
            appSettingsStorage.isVoiceRecordingEnabled()
        )
    }

    /**
     * Save draft to database.
     * Lone tag lines in the note (tags only, e.g. "@ola #dom") - first a question whether to drop them
     * from the note; either way their tags stay with the thought.
     * ! Not asked when the note is nothing but such lines and the only form - nothing would be left to save
     */
    private suspend fun saveDraft() {
        // Reset validation UI
        binding.tvHexTagsValidationInfo.text = ""

        val noteText = viewModel.draftThought.value?.richText
        val noteWithoutLoneTags = HexTagsUtils.removeLoneTagLines(noteText)
        val wouldLeaveNothing = noteWithoutLoneTags == null && !viewModel.hasFormsBesidesNote()

        if (!HexTagsUtils.hasLoneTagLines(noteText) || wouldLeaveNothing) {
            saveDraftWithTagsFrom(noteText)
            return
        }

        ActionsDialog.Builder(this)
            .setTitle(getString(R.string.capture_lone_tags_dialog_title))
            .setDescription(getString(R.string.capture_lone_tags_dialog_description))
            .setSecondaryAction(getString(R.string.capture_lone_tags_dialog_keep)) {
                lifecycleScope.launch { saveDraftWithTagsFrom(noteText) }
            }
            .setPrimaryAction(getString(R.string.capture_lone_tags_dialog_remove)) {
                if (noteWithoutLoneTags == null) viewModel.deleteRichText() else viewModel.updateRichText(noteWithoutLoneTags)
                lifecycleScope.launch { saveDraftWithTagsFrom(noteText) }
            }
            .show()
    }

    /**
     * The top field is the subject only - "@" and "#" there are plain text.
     * Tags come from the note: @/# markers anywhere in it, taken from [noteText] as typed, before any lone line is dropped.
     */
    private suspend fun saveDraftWithTagsFrom(noteText: String?) {
        val subject = binding.etSubject.text?.toString()?.trim()?.ifEmpty { null }
        val embeddedTags = HexTagsUtils.extractEmbeddedTags(noteText)

        // Same tag typed twice in the note - kept once
        val project = HexTagsUtils.mergeTagNames(embeddedTags.project, null)
        val person = HexTagsUtils.mergeTagNames(embeddedTags.person, null)

        // Update draft with hex tags
        viewModel.updateHexTags(
            subject = subject,
            project = project,
            person = person
        )

        // Validate
        val validationResult = viewModel.validate()

        // Show validation error
        if (validationResult is ValidationResult.Error) {
            binding.tvHexTagsValidationInfo.text = validationResult.resolveMessage(this)
            return
        }

        val similarTag = hexTagsService.findFirstSimilarTag(person, project)
        if (similarTag == null) {
            saveNewThought()
            return
        }

        ActionsDialog.Builder(this)
            .setTitle(getString(R.string.common_hex_tags_similar_title))
            .setDescription(getString(R.string.common_hex_tags_similar_description, similarTag.suggestion))
            .setSecondaryAction(getString(R.string.common_hex_tags_similar_keep_mine)) {
                lifecycleScope.launch { saveNewThought() }
            }
            .setPrimaryAction(getString(R.string.common_hex_tags_similar_use, similarTag.suggestion)) {
                applySimilarTag(subject, person, project, similarTag)
                lifecycleScope.launch { saveNewThought() }
            }
            .show()
    }

    /**
     * ! The note text is rewritten too - an inline "#powiesc" left in the note would show a tag
     * the thought is no longer linked to.
     */
    private fun applySimilarTag(subject: String?, person: String?, project: String?, similarTag: SimilarTag) {
        viewModel.updateHexTags(
            subject = subject,
            project = if (similarTag.tagType == HexTagType.PROJECT) hexTagsService.replaceTag(project, similarTag) else project,
            person = if (similarTag.tagType == HexTagType.PERSON) hexTagsService.replaceTag(person, similarTag) else person
        )

        val richText = viewModel.draftThought.value?.richText
        val correctedRichText = HexTagsUtils.replaceEmbeddedTag(
            richText, similarTag.tagType, similarTag.typedTag, similarTag.suggestion
        )
        if (correctedRichText != null && correctedRichText != richText) {
            viewModel.updateRichText(correctedRichText)
        }
    }

    private suspend fun saveNewThought() {
        val result = viewModel.saveNewThought()

        if (result.isSuccess) {
            onboardingManager.recordTaskPoints(
                OnboardingTask.CAPTURE_THOUGHT, usedTaskPoints(viewModel.draftThought.value)
            )

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
                ?: getString(R.string.capture_voice_error_save_failed)
        }
    }

    private fun usedTaskPoints(draft: ThoughtDTO?): Set<OnboardingTaskPoint> {
        if (draft == null) return emptySet()

        return buildSet {
            if (!draft.subject.isNullOrBlank()) add(OnboardingTaskPoint.SUBJECT)
            if (!draft.richText.isNullOrBlank()) add(OnboardingTaskPoint.NOTE)
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
        const val EXTRA_CAPTURE_FORM = "EXTRA_CAPTURE_FORM"
        // Opened from the Stream fab while the capture thought task is active
        const val EXTRA_ONBOARDING_GUIDE = "EXTRA_ONBOARDING_GUIDE"

        private const val SUBJECT_AND_SAVE_GUIDE_STEPS = 2
        private const val GUIDE_AFTER_DIALOG_DELAY_MS = 400L
    }
}
