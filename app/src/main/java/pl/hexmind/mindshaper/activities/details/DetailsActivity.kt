package pl.hexmind.mindshaper.activities.details

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.common.ui.CommonIconsListDialog
import pl.hexmind.mindshaper.common.ui.CommonIconsListItem
import pl.hexmind.mindshaper.common.ui.CommonTextEditDialog
import pl.hexmind.mindshaper.activities.CoreActivity
import pl.hexmind.mindshaper.activities.home.HomeActivity
import pl.hexmind.mindshaper.databinding.DetailsEditActivityBinding
import pl.hexmind.mindshaper.services.DomainsService
import pl.hexmind.mindshaper.services.ThoughtsService
import pl.hexmind.mindshaper.services.dto.ThoughtDTO
import javax.inject.Inject

@AndroidEntryPoint
class DetailsActivity : CoreActivity() {

    @Inject
    lateinit var thoughtsService : ThoughtsService

    @Inject
    lateinit var domainsService: DomainsService

    private lateinit var binding: DetailsEditActivityBinding

    // State-related data
    private var thoughtDetails : ThoughtDTO? = null
    private var domainsWithIcons: List<CommonIconsListItem> = emptyList()

    companion object PARAMS {
        const val P_SELECTED_THOUGHT_ID = "P_SELECTED_THOUGHT_ID"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DetailsEditActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadThoughtFromIntent()
        setupListeners()

        lifecycleScope.launch {
            loadAllDomainsWithIcons()
            updateUI()
        }
    }

    private fun loadThoughtFromIntent() {
        thoughtDetails = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(P_SELECTED_THOUGHT_ID, ThoughtDTO::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(P_SELECTED_THOUGHT_ID) as? ThoughtDTO
        }
    }

    private suspend fun loadAllDomainsWithIcons(){
        domainsWithIcons = domainsService.getAllDomainWithIcons()
    }

    private fun setupListeners(){
        binding.apply {
            btnSave.setOnClickListener {
                saveThought()
            }
            tvRichText.setOnClickListener {
                showEditRichTextDialog()
            }
            tvThread.setOnClickListener {
                showEditThreadDialog()
            }
            btnDomainIcon.setOnClickListener {
                showDomainDialog()
            }
            btnDomainIconPlaceholder.setOnClickListener {
                showDomainDialog()
            }
        }
    }

    private fun onDomainSelected(domain: CommonIconsListItem) {
        thoughtDetails?.domainId = domain.labelSourceId
        lifecycleScope.launch {
            updateDomainUI()
        }
    }

    fun saveThought(){
        val dto = thoughtDetails ?: return

        dto.thread = binding.tvThread.text.toString()
        dto.richText = binding.tvRichText.originalText // Save raw text

        lifecycleScope.launch {
            thoughtsService.updateThought(dto)
        }

        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
    }

    private fun showDomainDialog() {
        if (domainsWithIcons.isEmpty()) return

        CommonIconsListDialog.Builder(this)
            .setIcons(domainsWithIcons)
            .setOnIconSelected { selectedDomain ->
                onDomainSelected(selectedDomain)
            }
            .show()
    }

    private fun showEditRichTextDialog() {
        CommonTextEditDialog(
            context = this,
            textInput = binding.tvRichText.originalText, // Show raw text
            onSave = { newText ->
                binding.tvRichText.originalText = newText // Show HTML-formatted text
            }
        ).show()
    }

    private fun showEditThreadDialog() {
        CommonTextEditDialog(
            context = this,
            textInput = binding.tvThread.text.toString(),
            onSave = { newText ->
                binding.tvThread.text = newText
            }
        ).show()
    }

    private suspend fun updateUI() {
        updateRichTextUI()
        updateThreadUI()
        updateDomainUI()
    }

    private fun updateRichTextUI(){
        val details = thoughtDetails ?: return

        if(details.richText.isNullOrBlank()){
            binding.btnRichTextPlaceholder.visibility = View.VISIBLE
            binding.tvRichText.visibility = View.GONE
        }
        else{
            binding.btnRichTextPlaceholder.visibility = View.GONE
            binding.tvRichText.visibility = View.VISIBLE
            binding.tvRichText.originalText = details.richText.orEmpty()
        }
    }

    private fun updateThreadUI(){
        val details = thoughtDetails ?: return

        if(details.thread.isNullOrBlank()){
            binding.btnThreadPlaceholder.visibility = View.VISIBLE
            binding.tvThread.visibility = View.GONE
        }
        else{
            binding.btnThreadPlaceholder.visibility = View.GONE
            binding.tvThread.visibility = View.VISIBLE
            binding.tvThread.text = details.thread
        }
    }

    private suspend fun updateDomainUI(){
        val details = thoughtDetails ?: return

        if(details.domainId != null){
            val iconId : Int? = domainsService.getIconIdForDomain(details.domainId!!)
            binding.btnDomainIcon.visibility = View.VISIBLE
            binding.btnDomainIcon.icon = getIcon(iconId!!)
            binding.btnDomainIconPlaceholder.visibility = View.GONE
        }
        else{
            binding.btnDomainIcon.visibility = View.GONE
            binding.btnDomainIconPlaceholder.visibility = View.VISIBLE
        }
    }

    private fun getIcon(iconIdToFind : Int): Drawable {
        val defaultIcon = AppCompatResources.getDrawable(this, R.drawable.ic_domain_default)!!
        val icon = this.domainsWithIcons.find { it.iconSourceId == iconIdToFind }?.iconDrawable ?: defaultIcon
        return icon
    }
}