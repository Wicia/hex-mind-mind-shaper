package pl.hexmind.mindshaper.activities.details

import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.activities.CoreActivity
import pl.hexmind.mindshaper.common.ui.CommonIconsListDialog
import pl.hexmind.mindshaper.common.ui.CommonIconsListItem
import pl.hexmind.mindshaper.common.ui.CommonTextEditDialog
import pl.hexmind.mindshaper.databinding.DetailsEditActivityBinding
import pl.hexmind.mindshaper.services.dto.ThoughtDTO
import pl.hexmind.mindshaper.services.validators.ThoughtValidator
import java.io.File

@AndroidEntryPoint
class DetailsActivity : CoreActivity() {

    private val viewModel: DetailsViewModel by viewModels()
    private lateinit var binding: DetailsEditActivityBinding

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
                val recording = binding.recordingPlayback.getCurrentRecording()
                viewModel.saveThought(recording)
                navigateToCarousel()
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
        }
    }

    private fun setupUI(){
        binding.vbThoughtValue.apply {
            maxLevel = ThoughtValidator.THOUGHT_VALUE_MAX
        }
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

    private fun updateUI(thought: ThoughtDTO) {
        updateRichTextUI(thought)
        updateThreadUI(thought)
        updateSoulNameUI(thought)
        updateProjectUI(thought)
        updateValueUI(thought)
        updateAudioUI(thought)
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
                binding.btnDomainIcon.icon = getIcon(iconId)
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
            binding.recordingPlayback.visibility = View.VISIBLE

            lifecycleScope.launch {
                viewModel.loadAudioForPlayback(thought.id ?: return@launch) { audioFile ->
                    binding.recordingPlayback.loadAudioForPlayback(audioFile)
                }
            }
        } else {
            binding.btnRecordingPlaceholder.visibility = View.VISIBLE
            binding.recordingPlayback.visibility = View.GONE
        }
    }

    /**
     * Update value button UI with current value
     */
    private fun updateValueUI(thought: ThoughtDTO) {
        // Set text to display value
        binding.vbThoughtValue.currentLevel = thought.value

        // Enable/disable buttons based on bounds
        binding.btnValueIncrease.isEnabled = viewModel.canIncreaseValue()
        binding.btnValueDecrease.isEnabled = viewModel.canDecreaseValue()

        if(!binding.btnValueIncrease.isEnabled){
            binding.btnValueIncrease.imageTintList  = ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.button_disabled_background)
            )
        }
        else{
            binding.btnValueIncrease.imageTintList  = ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.button_primary)
            )
        }
        if(!binding.btnValueDecrease.isEnabled){
            binding.btnValueDecrease.imageTintList  = ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.button_disabled_background)
            )
        }
        else{
            binding.btnValueDecrease.imageTintList  = ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.button_primary)
            )
        }
    }

    private fun getIcon(iconIdToFind: Int): Drawable {
        val defaultIcon = AppCompatResources.getDrawable(this, R.drawable.ic_domain_none)!!
        val domains = viewModel.domainsWithIcons.value ?: emptyList()
        return domains.find { it.iconEntityId == iconIdToFind }?.iconDrawable ?: defaultIcon
    }

    override fun onDestroy() {
        super.onDestroy()
        // Resources management
        binding.recordingPlayback.cleanupResources()
    }
}