package pl.hexmind.mindshaper.activities.stream

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.activity.viewModels
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import pl.hexmind.mindshaper.common.ui.dialogs.ActionsDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.activities.CoreActivity
import pl.hexmind.mindshaper.activities.capture.CaptureActivity
import pl.hexmind.mindshaper.activities.details.DetailsActivity
import pl.hexmind.mindshaper.common.ui.views.lists.SortConfig
import pl.hexmind.mindshaper.common.onboarding.OnboardingProgressStep
import pl.hexmind.mindshaper.common.regex.HexTagsUtils
import pl.hexmind.mindshaper.common.ui.dialogs.GuideDialog
import pl.hexmind.mindshaper.common.ui.views.IconsGridItem
import pl.hexmind.mindshaper.services.ThoughtStatusService
import pl.hexmind.mindshaper.services.dto.ThoughtDTO
import pl.hexmind.mindshaper.services.validators.ThoughtValidator
import javax.inject.Inject

/**
 * Activity for browsing thoughts in a vertical feed
 */
@AndroidEntryPoint
class StreamActivity : CoreActivity() {

    @Inject
    lateinit var thoughtValidator: ThoughtValidator

    @Inject
    lateinit var thoughtStatusService: ThoughtStatusService

    private val viewModel: StreamViewModel by viewModels()

    private lateinit var viewPager: ViewPager2

    // Raised on sort change, consumed once the new order is committed
    private var isSortResetPending = false
    private lateinit var adapter: StreamAdapter

    private lateinit var btnSort: MaterialButton
    private lateinit var btnFilter: MaterialButton
    private lateinit var tilSearch: TextInputLayout
    private lateinit var etSearch: TextInputEditText

    // FAB menu
    private lateinit var fabNewThought: FloatingActionButton
    private var isMenuOpen = false

    // Scroll progress indicator
    private lateinit var flScrollIndicator: android.widget.FrameLayout
    private lateinit var vScrollProgress: android.view.View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.stream_activity)

        initializeViews()
        setupVerticalFeed()
        setupRealTimeSearchBar()
        setupSortButton()
        setupFilterButton()
        setupFabMenu()
        setupScrollIndicator()
        setupReactiveDataObserver()

        viewModel.loadDomains()

        onboardingManager.showTooltipForStep(
            OnboardingProgressStep.STREAM_TOOLTIP, this
        )
    }

    private fun initializeViews() {
        viewPager = findViewById(R.id.vp_thoughts)
        btnSort = findViewById(R.id.btn_sort)
        btnFilter = findViewById(R.id.btn_filter)
        tilSearch = findViewById(R.id.til_search)
        etSearch = findViewById(R.id.et_search)
        fabNewThought = findViewById(R.id.fab_new_thought)
        flScrollIndicator = findViewById(R.id.fl_scroll_indicator)
        vScrollProgress   = findViewById(R.id.v_scroll_progress)
        setupHeader(R.drawable.ic_activity_stream, R.string.stream_title)
    }

    private fun setupVerticalFeed() {
        adapter = StreamAdapter(
            appSettingsStorage,
            thoughtStatusService,
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
            },
            onLoadPhoto = { thoughtId, onReady ->
                viewModel.loadPhotoForDisplay(thoughtId, onReady)
            }
        )

        viewPager.adapter = adapter
        viewPager.orientation = ViewPager2.ORIENTATION_VERTICAL // VERTICAL feed!
        viewPager.offscreenPageLimit = 3

        // Smooth page change callback
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateScrollIndicator(position)
            }
        })
    }

    private fun setupScrollIndicator() {
        // Trigger initial state after layout is measured
        viewPager.post {
            updateScrollIndicator(viewPager.currentItem)
        }
    }

    private fun updateScrollIndicator(position: Int) {
        val total = adapter.itemCount
        if (total <= 1) {
            vScrollProgress.layoutParams.height = 0
            vScrollProgress.requestLayout()
            return
        }
        val progress    = position.toFloat() / (total - 1).toFloat()
        val totalHeight = flScrollIndicator.height
        vScrollProgress.layoutParams.height = (totalHeight * progress).toInt()
        vScrollProgress.requestLayout()
    }

    private fun setupRealTimeSearchBar() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val hexTags = HexTagsUtils.parseInput(s?.toString() ?: "")
                viewModel.updateSearchQuery(hexTags)
                updateSearchEndIcon(s?.isNotEmpty() == true)
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        tilSearch.setEndIconOnClickListener {
            if (etSearch.text?.isNotEmpty() == true) {
                etSearch.text?.clear()
                viewModel.clearSearch()
            }
            else {
                showSearchHelpDialog()
            }
        }
    }

    private fun updateSearchEndIcon(hasText: Boolean) {
        val iconRes = if (hasText) {
            android.R.drawable.ic_menu_close_clear_cancel
        }
        else {
            R.drawable.ic_search_help
        }
        tilSearch.endIconDrawable = ContextCompat.getDrawable(this, iconRes)
    }

    private fun showSearchHelpDialog() {
        GuideDialog.Builder(this)
            .addGuideScreen(getString(R.string.stream_searching_tooltip_1), getString(R.string.stream_searching_title_1))
            .addGuideScreen(getString(R.string.stream_searching_tooltip_2), getString(R.string.stream_searching_title_2))
            .show()
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

        // Listen for filter result from StreamFilterBottomSheet
        supportFragmentManager.setFragmentResultListener(
            StreamFilterBottomSheet.REQUEST_KEY, this
        ) { _, result ->
            val showActive  = result.getBoolean(StreamFilterBottomSheet.RESULT_SHOW_ACTIVE, true)
            val showDormant = result.getBoolean(StreamFilterBottomSheet.RESULT_SHOW_DORMANT, false)
            val domainId    = result.getInt(StreamFilterBottomSheet.RESULT_DOMAIN_ID, -1).takeIf { it != -1 }
            viewModel.updateShowActive(showActive)
            viewModel.updateShowDormant(showDormant)
            viewModel.updateSelectedDomain(domainId)

            // Show count of filtered thoughts after applying filters
            val count = viewModel.filteredThoughts.value?.size ?: 0
            showShortToast(R.string.stream_filter_toast_count, count.toString())
        }

        // Update filter button text when any filter changes
        viewModel.selectedDomainId.observe(this) { updateFilterButtonText(it) }
        viewModel.showActive.observe(this)        { updateFilterButtonText(viewModel.selectedDomainId.value) }
        viewModel.showDormant.observe(this)       { updateFilterButtonText(viewModel.selectedDomainId.value) }
    }

    private fun setupFabMenu() {
        fabNewThought.setOnClickListener {
            val intent = Intent(this, CaptureActivity::class.java)
            startActivity(intent)
        }
    }

    private fun showFilterDialog() {
        val domains = viewModel.domainsWithIcons.value ?: emptyList()
        val domainGridItems = domains.mapNotNull { domain ->
            val id = domain.labelEntityId ?: return@mapNotNull null
            IconsGridItem(id = id, iconResId = domain.iconResourceId)
        }

        StreamFilterBottomSheet.show(
            fragmentManager      = supportFragmentManager,
            showActive           = viewModel.showActive.value ?: true,
            showDormant          = viewModel.showDormant.value ?: false,
            selectedDomainId     = viewModel.selectedDomainId.value,
            domainItems          = domainGridItems,
            isDormantModeEnabled = appSettingsStorage.isDormantModeEnabled(),
            activeCount          = viewModel.countActiveThoughts(),
            dormantCount         = viewModel.countDormantThoughts()
        )
    }

    private fun updateFilterButtonText(domainId: Int?) {
        val dormantModeOn = appSettingsStorage.isDormantModeEnabled()
        val count = listOfNotNull(
            if (dormantModeOn && viewModel.showActive.value == true) true else null,
            if (dormantModeOn && viewModel.showDormant.value == true) true else null,
            domainId
        ).size
        btnFilter.text = if (count == 0) getString(R.string.stream_filter_button_none) else "($count)"
    }

    private fun showSortDialog() {
        val currentConfig = viewModel.sortConfig.value ?: SortConfig()

        val dialog = SortDialogFragment(currentConfig) { newConfig ->
            // ! raise BEFORE the value changes - the VM's mediator rebuilds the list before this Activity's sortConfig observer runs
            isSortResetPending = true
            viewModel.updateSortConfig(newConfig)
        }

        dialog.show(supportFragmentManager, SortDialogFragment.TAG)
    }

    private fun showDeleteConfirmationDialog(thought: ThoughtDTO) {
        ActionsDialog.Builder(this)
            .setTitle(getString(R.string.common_deletion_dialog_title))
            .setDescription(getString(R.string.common_deletion_dialog_message, getString(R.string.common_object_type_thought)))
            .setCautionAction(getString(R.string.common_deletion_dialog_yes)) {
                deleteThought(thought)
            }
            .setDismissText(getString(R.string.common_deletion_dialog_no))
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
            adapter.submitList(thoughtsDTO) {
                // ! commit callback, not a timer - resetting before the diff lands moves the OLD position 0
                if (isSortResetPending) {
                    isSortResetPending = false
                    viewPager.setCurrentItem(0, false)
                }
            }
        }

        viewModel.sortConfig.observe(this) { sortConfig ->
            adapter.updateSortConfig(sortConfig)
            performListRefresh()
            updateSortButtonLabel(sortConfig)
        }
    }

    private fun updateSortButtonLabel(sortConfig : SortConfig){
        val sortProperty = sortConfig.property
        val labelText = getString(sortProperty.displayNameRes)
            .plus(": ")
            .plus(getString(sortConfig.direction.getLabelResByFieldType(sortProperty.type)))

        btnSort.text = labelText
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