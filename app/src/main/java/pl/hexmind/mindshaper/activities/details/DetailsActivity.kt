package pl.hexmind.mindshaper.activities.details

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.activities.CoreActivity
import pl.hexmind.mindshaper.activities.carousel.CarouselActivity
import pl.hexmind.mindshaper.common.ui.CommonIconsListDialog
import pl.hexmind.mindshaper.common.ui.CommonIconsListItem
import pl.hexmind.mindshaper.common.ui.CommonTextEditDialog
import pl.hexmind.mindshaper.databinding.DetailsEditActivityBinding
import pl.hexmind.mindshaper.services.dto.ThoughtDTO

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
                viewModel.saveThought()
                navigateToCarousel()
            }

            tvRichText.apply{
                propagateClickEventsToParent = false
                setOnClickListener {
                    showEditRichTextDialog()
                }
            }

            tvThread.setOnClickListener {
                showEditThreadDialog()
            }

            btnThreadPlaceholder.setOnClickListener {
                showEditThreadDialog()
            }

            btnDomainIcon.setOnClickListener {
                showDomainDialog()
            }

            btnDomainIconPlaceholder.setOnClickListener {
                showDomainDialog()
            }

            btnSoulNamePlaceholder.setOnClickListener {
                showEditSoulNameDialog()
            }
            tvSoulName.setOnClickListener {
                showEditSoulNameDialog()
            }

            btnProjectPlaceholder.setOnClickListener {
                showEditProjectDialog()
            }
            tvProject.setOnClickListener {
                showEditProjectDialog()
            }
        }
    }

    private fun onDomainSelected(domain: CommonIconsListItem) {
        viewModel.updateDomain(domain.labelSourceId)
    }

    private fun navigateToCarousel() {
        val intent = Intent(this, CarouselActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun showDomainDialog() {
        val domains = viewModel.domainsWithIcons.value ?: emptyList()
        if (domains.isEmpty()) return

        CommonIconsListDialog.Builder(this)
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
            onSave = { newText ->
                viewModel.updateThread(newText)
            }
        ).show()
    }

    private fun showEditSoulNameDialog() {
        val currentText = viewModel.thoughtDetails.value?.soulName.orEmpty()
        CommonTextEditDialog(
            context = this,
            textInput = currentText,
            onSave = { newText ->
                viewModel.updateSoulName(newText)
            }
        ).show()
    }

    private fun showEditProjectDialog() {
        val currentText = viewModel.thoughtDetails.value?.project.orEmpty()
        CommonTextEditDialog(
            context = this,
            textInput = currentText,
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
        if (thought.soulName.isNullOrBlank()) {
            binding.btnSoulNamePlaceholder.visibility = View.VISIBLE
            binding.tvSoulName.visibility = View.GONE
        }
        else {
            binding.btnSoulNamePlaceholder.visibility = View.GONE
            binding.tvSoulName.visibility = View.VISIBLE
            binding.tvSoulName.text = thought.soulName
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
    private fun getIcon(iconIdToFind: Int): Drawable {
        val defaultIcon = AppCompatResources.getDrawable(this, R.drawable.ic_domain_none)!!
        val domains = viewModel.domainsWithIcons.value ?: emptyList()
        return domains.find { it.iconSourceId == iconIdToFind }?.iconDrawable ?: defaultIcon
    }
}