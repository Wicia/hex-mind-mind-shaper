package pl.hexmind.mindshaper.activities.stream

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.IntentCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import pl.hexmind.mindshaper.common.ui.dialogs.ActionsDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.activities.CoreActivity
import pl.hexmind.mindshaper.activities.capture.CaptureActivity
import pl.hexmind.mindshaper.activities.details.DetailsActivity
import pl.hexmind.mindshaper.activities.onboarding.OnboardingActivity
import pl.hexmind.mindshaper.common.onboarding.OnboardingTask
import pl.hexmind.mindshaper.common.onboarding.OnboardingTaskGuide
import pl.hexmind.mindshaper.common.ui.views.lists.SortConfig
import pl.hexmind.mindshaper.common.ui.views.lists.SortDirection
import pl.hexmind.mindshaper.common.regex.HexTags
import pl.hexmind.mindshaper.common.ui.dialogs.GuideDialog
import pl.hexmind.mindshaper.common.ui.dpToPx
import pl.hexmind.mindshaper.common.ui.views.HexSpotlight
import pl.hexmind.mindshaper.common.ui.views.HexTagsSearcher
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

    // Raised when the order or the criteria change, consumed once the new list is committed
    private var isListResetPending = false
    private lateinit var adapter: StreamAdapter

    private lateinit var btnSort: MaterialButton
    private lateinit var btnFilter: MaterialButton
    private lateinit var searcher: HexTagsSearcher

    // New thought FAB
    private lateinit var fabNewThought: FloatingActionButton

    // Scroll progress indicator
    private lateinit var flScrollIndicator: android.widget.FrameLayout
    private lateinit var vScrollProgress: android.view.View

    private lateinit var tvStreamEmpty: TextView

    // Capture returns RESULT_OK only after a thought was saved - leaving it without saving shows nothing
    private val captureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) showShortToast(R.string.stream_toast_thought_created)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.stream_activity)

        initializeViews()
        setupVerticalFeed()
        setupSearcher()
        setupSortButton()
        setupFilterButton()
        setupNewThoughtFab()
        setupScrollIndicator()
        setupReactiveDataObserver()

        viewModel.loadDomains()

        // Only on the first creation - after a recreation the ViewModel already holds this (or a newer) search
        if (savedInstanceState == null) {
            applyLaunchSearchCriteria()
            applyLaunchStateFilter()
        }
    }

    private fun initializeViews() {
        viewPager = findViewById(R.id.vp_thoughts)
        btnSort = findViewById(R.id.btn_sort)
        btnFilter = findViewById(R.id.btn_filter)
        searcher = findViewById(R.id.hts_search)
        fabNewThought = findViewById(R.id.fab_new_thought)
        flScrollIndicator = findViewById(R.id.fl_scroll_indicator)
        vScrollProgress   = findViewById(R.id.v_scroll_progress)
        tvStreamEmpty     = findViewById(R.id.tv_stream_empty)
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

        // Feather the top/bottom edges so a card sliding between pages fades out instead of being cut
        (viewPager.getChildAt(0) as? RecyclerView)?.apply {
            isVerticalFadingEdgeEnabled = true
            setFadingEdgeLength(dpToPx(FADING_EDGE_DP))
        }

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

    private fun setupSearcher() {
        searcher.onOpenRequested = {
            StreamSearchBottomSheet.show(
                fragmentManager = supportFragmentManager,
                currentTags     = searcher.getTags()
            ) { hexTags ->
                searcher.setTags(hexTags)
            }
        }

        // The help used to sit behind the "?" icon of the old text field
        // TODO: to be used when "traditional" searching will be brought back as alternative (like setting config)
//        searcher.setOnLongClickListener {
//            showSearchHelpDialog()
//            true
//        }

        searcher.onTagsChanged = { hexTags ->
            // A different set of criteria is a different stream - start reading it from the top
            isListResetPending = true

            if (hexTags.areCriteriaEmpty()) {
                viewModel.clearSearch()
            }
            else {
                viewModel.updateSearchQuery(hexTags)
            }
        }
    }

    // Criteria passed by the caller (e.g. a Metadata tag pill) replace the current search
    private fun applyLaunchSearchCriteria() {
        val criteria = IntentCompat.getParcelableExtra(intent, P_SEARCH_CRITERIA, HexTags::class.java) ?: return
        searcher.setTags(criteria)
    }

    // Opened for the near-dormant thoughts only - the state filter shows just them, still editable
    private fun applyLaunchStateFilter() {
        if (!intent.getBooleanExtra(P_NEAR_DORMANT_ONLY, false)) return
        viewModel.updateShowActive(false)
        viewModel.updateShowNearDormant(true)
        viewModel.updateShowDormant(false)
    }

    // TODO: to be used when "traditional" searching will be brought back as alternative (like setting config)
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
            val showActive      = result.getBoolean(StreamFilterBottomSheet.RESULT_SHOW_ACTIVE, true)
            val showNearDormant = result.getBoolean(StreamFilterBottomSheet.RESULT_SHOW_NEAR_DORMANT, true)
            val showDormant     = result.getBoolean(StreamFilterBottomSheet.RESULT_SHOW_DORMANT, false)
            val domainId        = result.getInt(StreamFilterBottomSheet.RESULT_DOMAIN_ID, -1).takeIf { it != -1 }
            val createdPeriod = result.getString(StreamFilterBottomSheet.RESULT_CREATED_PERIOD)
                ?.let { name -> CreatedPeriod.valueOf(name) }
            viewModel.updateShowActive(showActive)
            viewModel.updateShowNearDormant(showNearDormant)
            viewModel.updateShowDormant(showDormant)
            viewModel.updateSelectedDomain(domainId)
            viewModel.updateCreatedPeriod(createdPeriod)

            // Show count of filtered thoughts after applying filters
            val count = viewModel.filteredThoughts.value?.size ?: 0
            showShortToast(R.string.stream_filter_toast_count, count.toString())
        }

        // Update filter button text when any filter changes
        viewModel.selectedDomainId.observe(this) { updateFilterButtonText(it) }
        viewModel.showActive.observe(this)        { updateFilterButtonText(viewModel.selectedDomainId.value) }
        viewModel.showNearDormant.observe(this)   { updateFilterButtonText(viewModel.selectedDomainId.value) }
        viewModel.showDormant.observe(this)       { updateFilterButtonText(viewModel.selectedDomainId.value) }
        viewModel.createdPeriod.observe(this)     { updateFilterButtonText(viewModel.selectedDomainId.value) }
    }

    private fun setupNewThoughtFab() {
        fabNewThought.setOnClickListener {
            captureLauncher.launch(Intent(this, CaptureActivity::class.java)
                .putExtra(CaptureActivity.EXTRA_ONBOARDING_GUIDE, onboardingManager.isGuiding(OnboardingTask.CAPTURE_THOUGHT)))
        }
    }

    override fun onResume() {
        super.onResume()
        val completedTask = onboardingManager.takeCompletedActiveTask()
        if (completedTask != null) showTaskCompletedDialog(completedTask) else showCaptureTaskGuide()
    }

    // Back from the place the active task was done - next task or just close
    private fun showTaskCompletedDialog(task: OnboardingTask) {
        ActionsDialog.Builder(this)
            .setTitle(getString(R.string.common_onboarding_task_done_title))
            .setIcon(R.drawable.ic_mountain_flag)
            .setConfetti()
            .setDescription(getString(R.string.common_onboarding_task_done_info, getString(task.titleRes)))
            .setPrimaryAction(getString(R.string.common_onboarding_task_done_next)) {
                startActivity(Intent(this, OnboardingActivity::class.java)
                    .putExtra(OnboardingActivity.EXTRA_SCROLL_TO_TASKS, true))
            }
            .setCancelText(getString(R.string.common_btn_close))
            .show()
    }

    // Capture thought task starts here - once per run
    private fun showCaptureTaskGuide() {
        val task = OnboardingTask.CAPTURE_THOUGHT
        if (!onboardingManager.shouldShowTaskGuide(task, OnboardingTaskGuide.STREAM_CAPTURE)) return

        onboardingManager.markTaskGuideShown(task, OnboardingTaskGuide.STREAM_CAPTURE)
        HexSpotlight.show(
            activity   = this,
            target     = fabNewThought,
            text       = getString(R.string.stream_onboarding_task_capture),
            buttonText = getString(R.string.common_spotlight_btn_got_it)
        ) {
            fabNewThought.performClick()
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
            showNearDormant      = viewModel.showNearDormant.value ?: true,
            showDormant          = viewModel.showDormant.value ?: false,
            selectedDomainId     = viewModel.selectedDomainId.value,
            domainItems          = domainGridItems,
            isDormantModeEnabled = appSettingsStorage.isDormantModeEnabled(),
            activeCount          = viewModel.countActiveThoughts(),
            nearDormantCount     = viewModel.countNearDormantThoughts(),
            dormantCount         = viewModel.countDormantThoughts(),
            createdPeriod        = viewModel.createdPeriod.value
        )
    }

    private fun updateFilterButtonText(domainId: Int?) {
        // States count as one criterion, and only when they differ from the default (active + near dormant)
        val statesChanged = appSettingsStorage.isDormantModeEnabled() && (
            viewModel.showActive.value != true ||
            viewModel.showNearDormant.value != true ||
            viewModel.showDormant.value == true
        )
        val count = listOfNotNull(
            if (statesChanged) true else null,
            domainId,
            viewModel.createdPeriod.value
        ).size
        btnFilter.text = if (count == 0) getString(R.string.stream_filter_button_none) else "($count)"
    }

    private fun showSortDialog() {
        val currentConfig = viewModel.sortConfig.value ?: SortConfig()

        val dialog = SortDialogFragment(currentConfig) { newConfig ->
            // ! raise BEFORE the value changes - the VM's mediator rebuilds the list before this Activity's sortConfig observer runs
            isListResetPending = true
            viewModel.updateSortConfig(newConfig)
        }

        dialog.show(supportFragmentManager, SortDialogFragment.TAG)
    }

    private fun showDeleteConfirmationDialog(thought: ThoughtDTO) {
        ActionsDialog.Builder(this)
            .setTitle(getString(R.string.common_deletion_dialog_thought_title))
            .setDescription(getString(R.string.common_deletion_dialog_thought_message))
            .setPrimaryAction(getString(R.string.common_deletion_dialog_thought_yes), caution = true) {
                deleteThought(thought)
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

    // No thoughts at all = invite to capture the first one; thoughts hidden by the criteria = say so
    private fun bindEmptyState(isListEmpty: Boolean) {
        tvStreamEmpty.isVisible = isListEmpty
        if (!isListEmpty) return

        tvStreamEmpty.setText(
            if (viewModel.hasAnyThoughts()) R.string.stream_empty_no_matches else R.string.stream_empty_no_thoughts
        )
    }

    /**
     * Setup reactive data observer that automatically updates UI when database changes
     */
    private fun setupReactiveDataObserver() {
        viewModel.filteredThoughts.observe(this) { thoughtsDTO ->
            bindEmptyState(thoughtsDTO.isEmpty())

            // Submit to adapter - will automatically animate changes with DiffUtil
            adapter.submitList(thoughtsDTO) {
                // ! commit callback, not a timer - resetting before the diff lands moves the OLD position 0
                if (isListResetPending) {
                    isListResetPending = false

                    // ! doOnPreDraw, not a plain call - at commit time the pager still sits on the OLD item count,
                    // so "go to 0" does nothing until the inserted rows shift the current thought down
                    viewPager.doOnPreDraw { viewPager.setCurrentItem(0, false) }
                }
            }
        }

        viewModel.sortConfig.observe(this) { sortConfig ->
            adapter.updateSortConfig(sortConfig)
            performListRefresh()
            updateSortButtonLabel(sortConfig)
        }
    }

    // Button shows only the sorted property; the arrow on the left tells the direction
    private fun updateSortButtonLabel(sortConfig : SortConfig){
        val sortProperty = sortConfig.property
        val propertyName = getString(sortProperty.displayNameRes)

        btnSort.text = propertyName
        btnSort.setIconResource(
            if (sortConfig.direction == SortDirection.DESCENDING) R.drawable.ic_sort_desc else R.drawable.ic_sort_asc
        )
        // Screen readers still get the direction in words
        btnSort.contentDescription = propertyName
            .plus(": ")
            .plus(getString(sortConfig.direction.getLabelResByFieldType(sortProperty.type)))
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

    companion object {
        const val P_SEARCH_CRITERIA   = "search_criteria"
        const val P_NEAR_DORMANT_ONLY = "near_dormant_only"

        private const val FADING_EDGE_DP = 16
    }
}
