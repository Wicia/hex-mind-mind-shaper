package pl.hexmind.mindshaper.activities.details

import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.view.View
import pl.hexmind.mindshaper.common.ui.dialogs.CountdownDialog
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
import pl.hexmind.mindshaper.common.dormant.ThoughtState
import pl.hexmind.mindshaper.services.ThoughtStatusService
import pl.hexmind.mindshaper.common.onboarding.OnboardingProgressStep
import pl.hexmind.mindshaper.common.ui.dialogs.HexTags
import pl.hexmind.mindshaper.common.ui.dialogs.HexTagsBottomSheet
import pl.hexmind.mindshaper.common.ui.dialogs.ActionsDialog
import pl.hexmind.mindshaper.common.ui.dialogs.PhotoFullscreenDialog
import pl.hexmind.mindshaper.common.ui.dialogs.TextEditDialog
import pl.hexmind.mindshaper.common.ui.dialogs.GuideDialog
import pl.hexmind.mindshaper.common.ui.views.IconsGridItem
import pl.hexmind.mindshaper.common.ui.views.content.HexAudioView
import pl.hexmind.mindshaper.common.ui.views.content.HexPhotoView
import pl.hexmind.mindshaper.common.ui.views.content.HexTextView
import pl.hexmind.mindshaper.databinding.DetailsEditActivityBinding
import pl.hexmind.mindshaper.services.dto.GuidelineWithGoalDTO
import pl.hexmind.mindshaper.services.dto.ThoughtDTO
import java.io.File
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

@AndroidEntryPoint
class DetailsActivity : ThoughtManagerActivity() {

    private val viewModel: DetailsViewModel by viewModels()
    private lateinit var binding: DetailsEditActivityBinding

    @Inject
    lateinit var thoughtStatusService: ThoughtStatusService

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

        viewModel.linkedGuideline.observe(this) { linked ->
            updateUsageSection(linked)
        }
    }

    private fun updateUsageSection(linked: GuidelineWithGoalDTO?) {
        binding.apply {
            if (linked != null) {
                btnUsageLink.visibility = View.GONE
                llUsageLinked.visibility = View.VISIBLE
                tvUsageHeader.text = getText(R.string.details_usage_linked)
                tvUsageGoalContext.text = linked.goalDescription
            }
            else { // Not linked
                btnUsageLink.visibility = View.VISIBLE
                llUsageLinked.visibility = View.GONE
                tvUsageHeader.text = getText(R.string.details_usage_not_linked)
            }
        }
    }

    /**
     * Setup adding new thought content buttons with 25% of screen width
     */
    private fun setupToolbarButtonWidths() {
        val screenWidth = resources.displayMetrics.widthPixels
        val buttonWidth = (screenWidth * 0.25f).toInt()

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
                viewModel.increaseValue() // for not locked thought
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

            audioRecordingPlayback.setCallback(object : HexAudioView.RecordingCallback {
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

            // Guideline link/unlink handlers
            btnUsageLink.setOnClickListener {
                openGuidelinePicker()
            }
            btnUsageUnlink.setOnClickListener {
                showUnlinkDialog()
            }
        }
    }

    private fun openGuidelinePicker() {
        GuidelinePickerBottomSheet.show(supportFragmentManager) { guidelineId ->
            viewModel.linkToGuideline(guidelineId)
        }
    }

    private fun showUnlinkDialog() {
        ActionsDialog.Builder(this)
            .setTitle(getString(R.string.details_usage_dialog_title))
            .setDescription(getString(R.string.details_usage_dialog_description))
            .setStandardAction(getString(R.string.details_usage_dialog_change)) {
                // Swap flow
                viewModel.unlinkFromGuideline()
                openGuidelinePicker()
            }
            .setCautionAction(getString(R.string.details_usage_dialog_unlink)) {
                viewModel.unlinkFromGuideline()
            }
            .setDismissText(getString(R.string.common_btn_cancel))
            .show()
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

        val state = thoughtStatusService.computeState(thought)
        updateBackgroundForState(state)

        val statusColor = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.thought_status_icon))

        when (state) {

            ThoughtState.LOCKED -> {
                binding.vbThoughtValue.isLocked = true
                binding.btnValueIncrease.isEnabled = false
                binding.btnValueDecrease.isEnabled = false
                val gray = ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.button_secondary_disabled_background)
                )
                binding.btnValueIncrease.imageTintList = gray
                binding.btnValueDecrease.imageTintList = gray
                binding.vbThoughtValue.setOnClickListener { animateStatusIconGlow() }

                binding.ivStatus.setImageResource(R.drawable.ic_status_locked)
                binding.ivStatus.imageTintList = statusColor
                binding.ivStatus.setOnClickListener { showLockedCountdownDialog(thought) }
            }

            ThoughtState.WARNING -> {
                binding.vbThoughtValue.isLocked = false
                binding.vbThoughtValue.currentLevel = thought.value
                binding.vbThoughtValue.setOnClickListener { viewModel.increaseValue() }
                binding.btnValueIncrease.isEnabled = viewModel.canIncreaseValue()
                binding.btnValueDecrease.isEnabled = viewModel.canDecreaseValue()
                binding.btnValueIncrease.imageTintList = null
                binding.btnValueDecrease.imageTintList = null

                binding.ivStatus.setImageResource(R.drawable.ic_status_attention)
                binding.ivStatus.imageTintList = statusColor
                binding.ivStatus.setOnClickListener { showWarningDialog() }
            }

            ThoughtState.DORMANT -> {
                binding.vbThoughtValue.isLocked = false
                binding.vbThoughtValue.currentLevel = thought.value
                binding.vbThoughtValue.setOnClickListener { viewModel.increaseValue() }
                binding.btnValueIncrease.isEnabled = viewModel.canIncreaseValue()
                binding.btnValueDecrease.isEnabled = viewModel.canDecreaseValue()
                binding.btnValueIncrease.imageTintList = null
                binding.btnValueDecrease.imageTintList = null

                binding.ivStatus.setImageResource(R.drawable.ic_status_dormant)
                binding.ivStatus.imageTintList = statusColor
                binding.ivStatus.setOnClickListener { showDormantDialog() }
            }

            ThoughtState.ACTIVE -> {
                binding.vbThoughtValue.isLocked = false
                binding.vbThoughtValue.currentLevel = thought.value
                binding.vbThoughtValue.setOnClickListener { viewModel.increaseValue() }
                binding.btnValueIncrease.isEnabled = viewModel.canIncreaseValue()
                binding.btnValueDecrease.isEnabled = viewModel.canDecreaseValue()
                binding.btnValueIncrease.imageTintList = null
                binding.btnValueDecrease.imageTintList = null

                binding.ivStatus.setImageResource(R.drawable.ic_status_wip)
                binding.ivStatus.imageTintList = statusColor
                binding.ivStatus.setOnClickListener(null)
            }
        }
    }

    private fun updateBackgroundForState(state: ThoughtState) {
        val bgColor = if (state == ThoughtState.DORMANT) R.color._gray_lvl_1 else R.color.app_background
        binding.llDetailsContent.setBackgroundColor(ContextCompat.getColor(this, bgColor))
    }

    private fun showWarningDialog() {
        GuideDialog.Builder(this)
            .withSingleScreen(getString(R.string.details_warning_dialog_title), getString(R.string.details_warning_dialog_message))
            .show()
    }

    private fun showDormantDialog() {
        ActionsDialog.Builder(this)
            .setTitle(getString(R.string.details_dormant_dialog_title))
            .setDescription(getString(R.string.details_dormant_dialog_message))
            .setCautionAction(getString(R.string.details_dormant_dialog_wake)) {
                viewModel.restoreFromDormant()
            }
            .setDismissText(getString(R.string.details_dormant_dialog_leave))
            .show()
    }

    private fun animateStatusIconGlow() {
        binding.ivStatus.animate()
            .scaleX(1.35f).scaleY(1.35f).setDuration(130)
            .withEndAction {
                binding.ivStatus.animate()
                    .scaleX(1f).scaleY(1f).setDuration(130).start()
            }.start()
    }

    private fun showLockedCountdownDialog(thought: ThoughtDTO) {
        val hours = appSettingsStorage.getSlowModeHours()
        val unlockAt = thought.createdAt.plusSeconds(hours * 3600L)
        val remainingMs = Duration.between(Instant.now(), unlockAt).toMillis()

        if (remainingMs <= 0) return

        CountdownDialog(
            context    = this,
            title      = getString(R.string.details_slow_mode_title),
            durationMs = remainingMs,
            message    = getString(R.string.details_slow_mode_description),
            onFinish   = { viewModel.thoughtDetails.value?.let { updateValueUI(it) } }
        ).show()
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
            onValidate = { tags -> viewModel.validateTags(tags) },
            currentTags = tags
        ) { result ->
            viewModel.updateHexTags(result)
        }
    }
}