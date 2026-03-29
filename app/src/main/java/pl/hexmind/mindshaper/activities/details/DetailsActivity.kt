package pl.hexmind.mindshaper.activities.details

import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.activities.ThoughtManagerActivity
import pl.hexmind.mindshaper.common.onboarding.OnboardingProgressStep
import pl.hexmind.mindshaper.common.ui.dialogs.HexTags
import pl.hexmind.mindshaper.common.ui.dialogs.HexTagsBottomSheet
import pl.hexmind.mindshaper.common.ui.dialogs.PhotoFullscreenDialog
import pl.hexmind.mindshaper.common.ui.dialogs.TextEditDialog
import pl.hexmind.mindshaper.common.ui.views.IconsGridItem
import pl.hexmind.mindshaper.common.ui.views.content.AudioRecordingView
import pl.hexmind.mindshaper.common.ui.views.content.HexPhotoView
import pl.hexmind.mindshaper.common.ui.views.content.HexTextView
import pl.hexmind.mindshaper.databinding.DetailsEditActivityBinding
import pl.hexmind.mindshaper.services.dto.ThoughtDTO
import pl.hexmind.mindshaper.services.validators.ThoughtValidator
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class DetailsActivity : ThoughtManagerActivity() {

    private val viewModel: DetailsViewModel by viewModels()
    private lateinit var binding: DetailsEditActivityBinding

    @Inject
    lateinit var thoughtValidator: ThoughtValidator

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

    companion object PARAMS {
        const val P_SELECTED_THOUGHT_ID = "P_SELECTED_THOUGHT_ID"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DetailsEditActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val thoughtId = intent.getIntExtra(P_SELECTED_THOUGHT_ID, -1)
        if (thoughtId == -1) {
            showErrorAndFinish(R.string.details_edit_thought_invalid_id)
            return
        }

        setupUI()
        setupToolbarButtonWidths()
        setupListeners()
        setupObservers()

        viewModel.loadThought(thoughtId)
        viewModel.loadDomains()

        onboardingManager.showTooltipForStep(
            OnboardingProgressStep.DETAILS_TOOLTIP, this
        )
    }

    private fun setupObservers() {
        // ! Observe changes in thoughtDetails & update UI if needed
        viewModel.thoughtDetails.observe(this) { thought ->
            if (thought != null) {
                updateUI(thought)
            }
            else {
                showErrorAndFinish(R.string.details_edit_thought_not_found)
            }
        }
    }

    /**
     * Setup adding new thought content buttons with 30% of screen width
     */
    private fun setupToolbarButtonWidths() {
        val screenWidth = resources.displayMetrics.widthPixels
        val buttonWidth = (screenWidth * 0.30f).toInt() // Here

        binding.btnRichTextAdd.layoutParams =
            (binding.btnRichTextAdd.layoutParams as ViewGroup.MarginLayoutParams).apply {
                width = buttonWidth
            }

        binding.btnRecordingAdd.layoutParams =
            (binding.btnRecordingAdd.layoutParams as ViewGroup.MarginLayoutParams).apply {
                width = buttonWidth
            }

        binding.btnPhotoAdd.layoutParams =
            (binding.btnPhotoAdd.layoutParams as ViewGroup.MarginLayoutParams).apply {
                width = buttonWidth
            }
    }

    private fun setupListeners() {
        binding.apply {

            // VALUE - Increase / Decrease
            btnValueIncrease.setOnClickListener {
                viewModel.increaseValue()
            }
            btnValueDecrease.setOnClickListener {
                viewModel.decreaseValue()
            }
            vbThoughtValue.setOnClickListener {
                viewModel.increaseValue()
            }

            // THREAD
            tvThread.setOnClickListener {
                showEditThreadDialog()
            }
            btnThreadPlaceholder.setOnClickListener {
                showEditThreadDialog()
            }

            // HEX TAGS
            btnHexTags.setOnClickListener {
                showHexTagsBottomSheet()
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

                binding.btnRecordingAdd.visibility = View.GONE
                binding.audioRecordingPlayback.visibility = View.VISIBLE
                binding.audioRecordingPlayback.switchToRecordPlaybackMode()
                binding.audioRecordingPlayback.cleanupResources(cancelCoroutines = false)
                binding.audioRecordingPlayback.showStatus(
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

                // Show photo widget inline
                binding.btnPhotoAdd.visibility = View.GONE
                binding.photoDisplayView.visibility = View.VISIBLE
                binding.photoDisplayView.showStatus(
                    R.string.photos_no_file,
                    R.color.validation_success
                )
            }

            photoDisplayView.setCallback(object : HexPhotoView.PhotoCallback {
                override fun onCameraCaptureRequested() {
                    takePhoto()
                }

                override fun onGalleryPickRequested() {
                    pickFromGallery()
                }

                override fun onPhotoDeleted() {
                    viewModel.deletePhoto()
                }

                override fun onPhotoClicked() {
                    showFullscreenPhoto()
                }

                override fun onError(error: String) {
                    Toast.makeText(this@DetailsActivity, error, Toast.LENGTH_SHORT).show()
                }
            })

            audioRecordingPlayback.setCallback(object : AudioRecordingView.RecordingCallback {
                override fun onRecordingStarted() {}

                override fun onRecordingStopped(file: File, durationMs: Long) {
                    viewModel.saveAudioRecording(file, durationMs)
                }

                override fun onRecordingDeleted() {
                    viewModel.deleteAudioRecording()
                }

                override fun onRecordingError(error: String) {
                    Toast.makeText(this@DetailsActivity, error, Toast.LENGTH_SHORT).show()
                }

                override fun onPlaybackStarted() {}
                override fun onPlaybackStopped() {}
                override fun onPermissionRequired() {}
            })

            richTextView.setCallback(object : HexTextView.TextCallback {
                override fun onTextClicked() {
                    showEditRichTextDialog()
                }

                override fun onTextDeleted() {
                    viewModel.deleteRichText()
                }
            })
        }
    }

    private fun showFullscreenPhoto() {
        val thought = viewModel.thoughtDetails.value ?: return
        if (!thought.hasPhoto) return

        lifecycleScope.launch {
            try {
                viewModel.loadPhotoForDisplay(thought.id ?: return@launch) { photoData ->
                    PhotoFullscreenDialog(this@DetailsActivity, photoData).show()
                }
            }
            catch (e: Exception) {
                Toast.makeText(this@DetailsActivity, "Failed to load photo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupUI() {
        // No header -> skip
        updateAddPhotoButtonVisualState()
        updateAddRecordingButtonVisualState()
    }

    private fun showEditThreadDialog() {
        val thought = viewModel.thoughtDetails.value ?: return
        val currentText = thought.thread ?: ""

        TextEditDialog(
            context = this,
            textInput = currentText,
            title = getString(R.string.common_hex_tag_thread),
            notesStyle = false,
            onSave = { newText ->
                viewModel.updateThread(newText)
            }
        ).show()
    }

    private fun showEditRichTextDialog() {
        val thought = viewModel.thoughtDetails.value ?: return
        val currentText = thought.richText ?: ""

        TextEditDialog(
            context = this,
            textInput = currentText,
            title = getString(R.string.common_edit_note_header),
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
                binding.photoDisplayView.showLoading()
                viewModel.savePhotoFromUri(uri)
            }
            catch (e: Exception) {
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

    private fun updateUI(thought: ThoughtDTO) {
        updateRichTextUI(thought)
        updateThreadUI(thought)
        updateValueUI(thought)
        updateAudioUI(thought)
        updatePhotoUI(thought)
        updateHexTagsUI(thought)
    }

    private fun updateHexTagsUI(thought: ThoughtDTO) {
        val count = listOfNotNull(
            thought.soulMate?.takeIf { it.isNotBlank() },
            thought.project?.takeIf { it.isNotBlank() },
            thought.domainId
        ).size

        binding.btnHexTags.text =
            if (count > 0)
                count.toString()
            else
                getString(R.string.common_btn_create)
    }

    private fun updateRichTextUI(thought: ThoughtDTO) {
        if (thought.richText.isNullOrBlank()) {
            binding.btnRichTextAdd.visibility = View.VISIBLE
            binding.richTextView.visibility = View.GONE
        }
        else {
            binding.btnRichTextAdd.visibility = View.GONE
            binding.richTextView.visibility = View.VISIBLE
            binding.richTextView.originalText = thought.richText.orEmpty()
        }
    }

    private fun updateThreadUI(thought: ThoughtDTO) {
        if (thought.thread.isNullOrBlank()) {
            binding.btnThreadPlaceholder.visibility = View.VISIBLE
            binding.tvThread.visibility = View.GONE
        }
        else {
            binding.btnThreadPlaceholder.visibility = View.GONE
            binding.tvThread.visibility = View.VISIBLE
            binding.tvThread.text = thought.thread
        }
    }

    /**
     * whole UI update for recording and playing sound recordings widget
     */
    private fun updateAudioUI(thought: ThoughtDTO) {
        val featureEnabled = appSettingsStorage.isVoiceRecordingEnabled()
        updateAddRecordingButtonVisualState()

        // Feature disabled
        if (!featureEnabled) {
            binding.btnRecordingAdd.visibility = View.VISIBLE // always show disabled button
            binding.audioRecordingPlayback.visibility = View.GONE
            return
        }

        // Feature enabled
        if (thought.hasAudio) {
            binding.btnRecordingAdd.visibility = View.GONE
            binding.audioRecordingPlayback.visibility = View.VISIBLE

            lifecycleScope.launch {
                viewModel.loadAudioForPlayback(thought.id ?: return@launch) { audioFile ->
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
     * whole UI update for taking and displaying photo
     */
    private fun updatePhotoUI(thought: ThoughtDTO) {
        val featureEnabled = appSettingsStorage.isPhotoFeatureEnabled()
        updateAddPhotoButtonVisualState()

        // Feature disabled
        if (!featureEnabled) {
            binding.btnPhotoAdd.visibility = View.VISIBLE // always show disabled button
            binding.photoDisplayView.visibility = View.GONE
            return
        }

        // Feature enabled
        if (thought.hasPhoto) {
            binding.btnPhotoAdd.visibility = View.GONE
            binding.photoDisplayView.visibility = View.VISIBLE

            lifecycleScope.launch {
                viewModel.loadPhotoForDisplay(thought.id ?: return@launch) { photoData ->
                    binding.photoDisplayView.loadPhoto(photoData)
                }
            }
        }
        else {
            binding.btnPhotoAdd.visibility = View.VISIBLE
            binding.photoDisplayView.visibility = View.GONE
        }
    }

    private fun updateValueUI(thought: ThoughtDTO) {
        // Set text to display value
        binding.vbThoughtValue.currentLevel = thought.value

        // Enable/disable buttons based on bounds
        binding.btnValueIncrease.isEnabled = viewModel.canIncreaseValue()
        binding.btnValueDecrease.isEnabled = viewModel.canDecreaseValue()

        if(!binding.btnValueIncrease.isEnabled){
            binding.btnValueIncrease.imageTintList  = ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.button_secondary_disabled_background)
            )
        }
        else{
            binding.btnValueIncrease.imageTintList  = ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.button_secondary_enabled_background)
            )
        }
        if(!binding.btnValueDecrease.isEnabled){
            binding.btnValueDecrease.imageTintList  = ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.button_secondary_disabled_background)
            )
        }
        else{
            binding.btnValueDecrease.imageTintList  = ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.button_secondary_enabled_background)
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Resources management
        binding.audioRecordingPlayback.cleanupResources()
    }

    private fun showHexTagsBottomSheet() {
        val domains = viewModel.domainsWithIcons.value ?: emptyList()
        if (domains.isEmpty()) return

        val items = domains.mapNotNull { domain ->
            IconsGridItem(
                id = domain.labelEntityId ?: return@mapNotNull null,
                iconResId = domain.iconResourceId
            )
        }

        val tags = HexTags(
            domainId = viewModel.thoughtDetails.value?.domainId,
            person = viewModel.thoughtDetails.value?.soulMate,
            project = viewModel.thoughtDetails.value?.project
        )

        HexTagsBottomSheet.show(
            fragmentManager = supportFragmentManager,
            items = items,
            currentTags = tags
        ) { result ->
            viewModel.updateDomain(domainId = result.domainId)
            viewModel.updateSoulMate(result.person ?: "")
            viewModel.updateProject(result.project ?: "")
        }
    }
}