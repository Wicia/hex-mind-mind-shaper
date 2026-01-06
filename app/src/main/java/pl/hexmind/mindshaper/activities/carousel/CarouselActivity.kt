package pl.hexmind.mindshaper.activities.carousel

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.GestureDetector
import androidx.activity.viewModels
import androidx.appcompat.content.res.AppCompatResources
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.activities.CoreActivity
import pl.hexmind.mindshaper.activities.details.DetailsActivity
import pl.hexmind.mindshaper.common.SortConfig
import pl.hexmind.mindshaper.common.onboarding.OnboardingProgressStep
import pl.hexmind.mindshaper.common.regex.HexTagsUtils
import pl.hexmind.mindshaper.common.ui.CommonIconsListDialog
import pl.hexmind.mindshaper.common.ui.CommonIconsListItem
import pl.hexmind.mindshaper.services.dto.ThoughtDTO
import pl.hexmind.mindshaper.services.validators.ThoughtValidator
import timber.log.Timber
import javax.inject.Inject

/**
 * Activity for browsing thoughts in an elegant carousel format with 3D animations and search
 */
@AndroidEntryPoint
class CarouselActivity : CoreActivity() {

    @Inject
    lateinit var thoughtValidator: ThoughtValidator

    private val viewModel: CarouselViewModel by viewModels()

    private lateinit var viewPager: ViewPager2
    private lateinit var adapter: CarouselAdapter

    // Search UI components
    private lateinit var tilSearch: TextInputLayout
    private lateinit var etSearch: TextInputEditText
    private lateinit var btnSort: MaterialButton
    private lateinit var btnFilter: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.carousel_activity)

        initializeViews()
        setupCarousel()
        setupRealTimeSearchBar()
        setupSortButton()
        setupFilterButton()
        setupReactiveDataObserver()

        viewModel.loadDomains()

        showTooltip(
            R.string.onb_dialog_header,
            contentResId = R.string.carousel_possibilities_tooltip_content,
            stepToComplete = OnboardingProgressStep.CAROUSEL_TOOLTIP_SHOWN
        )
    }

    private fun initializeViews() {
        viewPager = findViewById(R.id.vp_thoughts)
        tilSearch = findViewById(R.id.til_search)
        etSearch = findViewById(R.id.et_search)
        btnSort = findViewById(R.id.btn_sort)
        btnFilter = findViewById(R.id.btn_filter)
        setupHeader(R.drawable.ic_header_carousel, R.string.thoughts_carousel_title)
    }

    private fun setupCarousel() {
        adapter = CarouselAdapter(
            thoughtValidator,
            onDeleteThought = { thoughtToDelete ->
                showDeleteConfirmationDialog(thoughtToDelete)
            },
            onThoughtTap = { thoughtTap ->
                val intent = Intent(this, DetailsActivity::class.java)
                intent.putExtra(DetailsActivity.P_SELECTED_THOUGHT_ID, thoughtTap.id ?: -1)
                startActivity(intent)
            },
            onLoadAudio = { thoughtId, onReady ->
                viewModel.loadAudioForPlayback(thoughtId, onReady)
            }
        )

        viewPager.adapter = adapter
        viewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        viewPager.offscreenPageLimit = 3

        // Custom page transformer for 3D carousel effect
        viewPager.setPageTransformer { _, _ -> ThoughtCardPageTransformer() }

        // Smooth page change callback
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
        })
    }

    private fun setupRealTimeSearchBar() {
        // ! TextWatcher for real-time search
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val hexTags = HexTagsUtils.parseInput(s?.toString() ?: "")
                // Update search query in ViewModel on every text change
                viewModel.updateSearchQuery(hexTags)
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Listener for X icon
        tilSearch.setEndIconOnClickListener {
            etSearch.text?.clear()
            viewModel.clearSearch()
        }
    }

    private fun setupSortButton() {
        btnSort.setOnClickListener {
            showSortDialog()
        }
    }

    private fun setupFilterButton() {
        btnFilter.setOnClickListener {
            showFilterDialog()
        }

        // Update filter button text when domain changes
        viewModel.selectedDomainId.observe(this) { domainId ->
            updateFilterButtonText(domainId)
        }
    }

    private fun showFilterDialog() {
        val domains = viewModel.domainsWithIcons.value ?: emptyList()
        if (domains.isEmpty()) return

        // Add "All domains" option at the beginning
        val allDomainsOption = CommonIconsListItem(
            iconEntityId = null,
            iconResourceId = R.drawable.ic_domain_none,
            labelText = getString(R.string.carousel_filter_all_domains),
            labelEntityId = null,
            highlightItem = true
        )

        val domainsWithAll = listOf(allDomainsOption) + domains

        CommonIconsListDialog.Builder(this)
            .setTitle(getString(R.string.carousel_filter_dialog_title))
            .setIcons(domainsWithAll)
            .setOnIconSelected { selectedDomain ->
                onDomainFilterSelected(selectedDomain)
            }
            .show()
    }

    private fun onDomainFilterSelected(domain: CommonIconsListItem) {
        if (domain.labelEntityId == null) {
            // "All domains" selected - clear filter
            viewModel.clearDomainFilter()
        } else {
            // Specific domain selected
            viewModel.updateSelectedDomain(domain.labelEntityId)
        }
    }

    private fun updateFilterButtonText(domainId: Int?) {
        btnFilter.text = if (domainId == null) {
            getString(R.string.carousel_filter_button_none)
        }
        else {
            "(1)" // TODO: to be replaced with filter counter
        }
    }

    private fun showSortDialog() {
        val currentConfig = viewModel.sortConfig.value ?: SortConfig()

        val dialog = SortDialogFragment(currentConfig) { newConfig ->
            viewModel.updateSortConfig(newConfig)
        }

        dialog.show(supportFragmentManager, SortDialogFragment.TAG)
    }

    private fun showDeleteConfirmationDialog(thought: ThoughtDTO) {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.common_deletion_dialog_title))
            .setMessage(getString(R.string.common_deletion_dialog_message, "myśl"))
            .setPositiveButton(getString(R.string.common_deletion_dialog_yes)) { dialog, _ ->
                deleteThought(thought)
                Timber.d("Thought deleted: ${thought.id}")
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.common_deletion_dialog_no)) { dialog, _ ->
                Timber.d("Deletion cancelled")
                dialog.dismiss()
            }
            .show()
    }

    private fun deleteThought(thought: ThoughtDTO) {
        viewModel.deleteThought(thought)
        showShortToast(
            R.string.common_deletion_dialog_confirmation,
            this.getString(R.string.common_object_type_thought)
        )
    }

    /**
     * Setup reactive data observer that automatically updates UI when database changes
     */
    private fun setupReactiveDataObserver() {
        viewModel.filteredThoughts.observe(this) { thoughtsDTO ->
            // Submit to adapter - will automatically animate changes with DiffUtil
            adapter.submitList(thoughtsDTO)
        }

        viewModel.sortConfig.observe(this) { sortConfig ->
            performListRefresh()
            btnSort.text = getString(sortConfig.property.displayNameRes)
                .plus(": ")
                .plus(getString(sortConfig.direction.getLabelResByFieldType(sortConfig.property.type)))
        }
    }

    /**
     * Animate ViewPager refresh with fade and scale effect
     */
    private fun performListRefresh() {
        // Fade out and scale down
        viewPager.animate()
            .alpha(0.3f)
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(150)
            .withEndAction {
                // Reset to first item
                viewPager.setCurrentItem(0, false)

                // Fade in and scale up
                viewPager.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .start()
            }
            .start()
    }
}