package pl.hexmind.mindshaper.activities.details

import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.activities.CoreActivity
import pl.hexmind.mindshaper.common.onboarding.OnboardingProgressStep
import pl.hexmind.mindshaper.common.ui.CommonIconsListDialog
import pl.hexmind.mindshaper.common.ui.CommonIconsListItem
import pl.hexmind.mindshaper.common.ui.CommonTextEditDialog
import pl.hexmind.mindshaper.common.ui.HexPhotoView
import pl.hexmind.mindshaper.common.ui.PhotoFullscreenDialog
import pl.hexmind.mindshaper.databinding.DetailsEditActivityBinding
import pl.hexmind.mindshaper.services.dto.ThoughtDTO
import pl.hexmind.mindshaper.services.validators.ThoughtValidator
import javax.inject.Inject

@AndroidEntryPoint
class DetailsActivity : CoreActivity() {

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

    private fun setupListeners() {
        binding.apply {
            btnSave.setOnClickListener {
                val recording = binding.audioRecordingPlayback.getCurrentRecording()
                viewModel.saveThought(recording)
                navigateToCarousel()
            }

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

            // DOMAIN
            btnDomainIcon.setOnClickListener {
                showDomainDialog()
            }
            btnDomainIconPlaceholder.setOnClickListener {
                showDomainDialog()
            }

            // SOUL MATE
            btnSoulMatePlaceholder.setOnClickListener {
                showEditSoulNameDialog()
            }
            tvSoulMate.setOnClickListener {
                showEditSoulNameDialog()
            }

            // PROJECT
            btnProjectPlaceholder.setOnClickListener {
                showEditProjectDialog()
            }
            tvProject.setOnClickListener {
                showEditProjectDialog()
            }

            // RICH TEXT
            tvRichText.apply{
                propagateClickEventsToParent = false
                setOnClickListener {
                    showEditRichTextDialog()
                }
            }
            btnRichTextPlaceholder.apply{
                setOnClickListener {
                    showEditRichTextDialog()
                }
            }

            // RECORDING
            btnRecordingPlaceholder.setOnClickListener {
                showRecordingWidget()
            }

            btnPhotoPlaceholder.setOnClickListener {
                showPhotoWidget()
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
                    Toast.makeText(this@DetailsActivity, error, Toast.LENGTH_SHORT).show() // TODO
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
            } catch (e: Exception) {
                Toast.makeText(this@DetailsActivity, "Failed to load photo", Toast.LENGTH_SHORT).show() // TODO
            }
        }
    }

    private fun setupUI(){
        binding.vbThoughtValue.apply {
            maxLevel = thoughtValidator.getThoughtValueMax()
        }

        updatePhotoFeatureVisibility()
    }

    private fun onDomainSelected(domain: CommonIconsListItem) {
        domain.labelEntityId?.let {
            viewModel.updateDomain(domainId = it)
        }
    }

    private fun navigateToCarousel() {
        finish() // ! Don't create new Carousel Instance as sort + search params will vanish
    }

    private fun showDomainDialog() {
        val domains = viewModel.domainsWithIcons.value ?: emptyList()
        if (domains.isEmpty()) return

        CommonIconsListDialog.Builder(this)
            .setTitle(this.getString(R.string.common_hex_tag_domain))
            .setIcons(domains)
            .setOnIconSelected { selectedDomain ->
                onDomainSelected(selectedDomain)
            }
            .show()
    }

    private fun showEditRichTextDialog() {
        val currentText = viewModel.thoughtDetails.value?.richText.orEmpty()
        CommonTextEditDialog(
            context = this,
            textInput = currentText,
            onSave = { newText ->
                viewModel.updateRichText(newText)
            }
        ).show()
    }

    private fun showEditThreadDialog() {
        val currentText = viewModel.thoughtDetails.value?.thread.orEmpty()
        CommonTextEditDialog(
            context = this,
            textInput = currentText,
            title = getString(R.string.common_hex_tag_thread),
            onSave = { newText ->
                viewModel.updateThread(newText)
            }
        ).show()
    }

    private fun showEditSoulNameDialog() {
        val currentText = viewModel.thoughtDetails.value?.soulMate.orEmpty()
        CommonTextEditDialog(
            context = this,
            textInput = currentText,
            title = getString(R.string.common_hex_tag_soul_mates),
            onSave = { newText ->
                viewModel.updateSoulMate(newText)
            }
        ).show()
    }

    private fun showEditProjectDialog() {
        val currentText = viewModel.thoughtDetails.value?.project.orEmpty()
        CommonTextEditDialog(
            context = this,
            textInput = currentText,
            title = getString(R.string.common_hex_tag_project),
            onSave = { newText ->
                viewModel.updateProject(newText)
            }
        ).show()
    }

    private fun showRecordingWidget() {
        binding.btnRecordingPlaceholder.visibility = View.GONE
        binding.audioRecordingPlayback.visibility = View.VISIBLE

        binding.audioRecordingPlayback.switchToRecordPlaybackMode()
        binding.audioRecordingPlayback.cleanupResources(cancelCoroutines = false)
        binding.audioRecordingPlayback.showStatus(
            getString(R.string.capture_voice_tooltip),
            R.color.validation_success
        )
    }

    private fun showPhotoWidget() {
        binding.btnPhotoPlaceholder.visibility = View.GONE
        binding.photoDisplayView.visibility = View.VISIBLE
        binding.photoDisplayView.showStatus(
            R.string.photos_no_file,
            R.color.validation_success
        )
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
                binding.photoDisplayView.showError("Olaboga! Kod się wyburaczył :/") // TODO
            }
        }
    }

    private fun updatePhotoFeatureVisibility() {
        val featureEnabled = appSettingsStorage.isPhotoFeatureEnabled()

        if (!featureEnabled) {
            binding.photoDisplayView.visibility = View.GONE
            binding.btnPhotoPlaceholder.visibility = View.GONE
        }
    }

    private fun updateUI(thought: ThoughtDTO) {
        updateRichTextUI(thought)
        updateThreadUI(thought)
        updateSoulNameUI(thought)
        updateProjectUI(thought)
        updateValueUI(thought)
        updateAudioUI(thought)
        updatePhotoUI(thought)
        lifecycleScope.launch {
            updateDomainUI(thought)
        }
    }

    private fun updateRichTextUI(thought: ThoughtDTO) {
        if (thought.richText.isNullOrBlank()) {
            binding.btnRichTextPlaceholder.visibility = View.VISIBLE
            binding.tvRichText.visibility = View.GONE
        }
        else {
            binding.btnRichTextPlaceholder.visibility = View.GONE
            binding.tvRichText.visibility = View.VISIBLE
            binding.tvRichText.originalText = thought.richText.orEmpty()
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

    private suspend fun updateDomainUI(thought: ThoughtDTO) {
        if (thought.domainId != null) {
            val iconId = viewModel.getIconIdForDomain(thought.domainId!!)
            if (iconId != null) {
                binding.btnDomainIcon.visibility = View.VISIBLE
                binding.btnDomainIcon.setIconResource(getIconResourceId(iconId))
                binding.btnDomainIconPlaceholder.visibility = View.GONE
            }
            else {
                binding.btnDomainIcon.visibility = View.GONE
                binding.btnDomainIconPlaceholder.visibility = View.VISIBLE
            }
        }
        else {
            binding.btnDomainIcon.visibility = View.GONE
            binding.btnDomainIconPlaceholder.visibility = View.VISIBLE
        }
    }

    private fun updateSoulNameUI(thought: ThoughtDTO) {
        if (thought.soulMate.isNullOrBlank()) {
            binding.btnSoulMatePlaceholder.visibility = View.VISIBLE
            binding.tvSoulMate.visibility = View.GONE
        }
        else {
            binding.btnSoulMatePlaceholder.visibility = View.GONE
            binding.tvSoulMate.visibility = View.VISIBLE
            binding.tvSoulMate.text = thought.soulMate
        }
    }

    private fun updateProjectUI(thought: ThoughtDTO) {
        if (thought.project.isNullOrBlank()) {
            binding.btnProjectPlaceholder.visibility = View.VISIBLE
            binding.tvProject.visibility = View.GONE
        }
        else {
            binding.btnProjectPlaceholder.visibility = View.GONE
            binding.tvProject.visibility = View.VISIBLE
            binding.tvProject.text = thought.project
        }
    }

    private fun updateAudioUI(thought: ThoughtDTO) {
        if (thought.hasAudio) {
            binding.btnRecordingPlaceholder.visibility = View.GONE
            binding.audioRecordingPlayback.visibility = View.VISIBLE

            lifecycleScope.launch {
                viewModel.loadAudioForPlayback(thought.id ?: return@launch) { audioFile ->
                    binding.audioRecordingPlayback.loadAudioForPlayback(audioFile)
                }
            }
        }
        else {
            binding.btnRecordingPlaceholder.visibility = View.VISIBLE
            binding.audioRecordingPlayback.visibility = View.GONE
        }
    }

    /**
     * Update value button UI with current value
     */
    private fun updatePhotoUI(thought: ThoughtDTO) {
        val featureEnabled = appSettingsStorage.isPhotoFeatureEnabled()

        if (!featureEnabled) {
            binding.photoDisplayView.visibility = View.GONE
            binding.btnPhotoPlaceholder.visibility = View.GONE
            return
        }

        if (thought.hasPhoto) {
            binding.btnPhotoPlaceholder.visibility = View.GONE
            binding.photoDisplayView.visibility = View.VISIBLE

            lifecycleScope.launch {
                viewModel.loadPhotoForDisplay(thought.id ?: return@launch) { photoData ->
                    binding.photoDisplayView.loadPhoto(photoData)
                }
            }
        }
        else {
            binding.btnPhotoPlaceholder.visibility = View.VISIBLE
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
        else{ // disabled
            binding.btnValueIncrease.imageTintList  = ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.button_secondary_enabled_background)
            )
        }
        if(!binding.btnValueDecrease.isEnabled){
            binding.btnValueDecrease.imageTintList  = ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.button_secondary_disabled_background)
            )
        }
        else{ // disabled
            binding.btnValueDecrease.imageTintList  = ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.button_secondary_enabled_background)
            )
        }
    }

    @DrawableRes
    private fun getIconResourceId(iconIdToFind: Int): Int {
        val domains = viewModel.domainsWithIcons.value ?: emptyList()
        return domains.find { it.iconEntityId == iconIdToFind }?.iconResourceId ?: R.drawable.ic_domain_none
    }

    override fun onDestroy() {
        super.onDestroy()
        // Resources management
        binding.audioRecordingPlayback.cleanupResources()
    }
}