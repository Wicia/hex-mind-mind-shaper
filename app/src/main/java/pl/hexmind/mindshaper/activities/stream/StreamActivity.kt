package pl.hexmind.mindshaper.activities.stream

import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.animation.doOnEnd
import androidx.core.content.IntentCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.Lifecycle
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

    // Header (icon + title) folds away while paging forward and returns on any page back
    private lateinit var header: View
    private var headerFullHeight = 0
    private var headerMarginTop = 0
    private var headerMarginBottom = 0
    private var headerFraction = 1f
    private var isHeaderShown = true
    private var isHeaderShownPending = true
    private var headerAnimator: ValueAnimator? = null
    private var lastPagePosition = 0

    // Focus mode: left alone for a while, the stream dims to the thought card and the FAB
    private lateinit var cvSearchBar: View
    private lateinit var vFocusScrim: View
    private lateinit var btnFocusExit: MaterialButton
    private var isFocusMode = false
    private var focusFraction = 0f
    private var focusCardShift = 0f
    private var focusAnimator: ValueAnimator? = null
    private val focusModeHandler = Handler(Looper.getMainLooper())
    private val focusModeRunnable = Runnable { enterFocusMode() }

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
        setupFocusMode()
        setupReactiveDataObserver()

        viewModel.loadDomains()

        // Only on the first creation - after a recreation the ViewModel already holds this (or a newer) search
        if (savedInstanceState == null) {
            applyLaunchSearchCriteria()
            applyLaunchDomainFilter()
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
        cvSearchBar       = findViewById(R.id.cv_search_bar)
        vFocusScrim       = findViewById(R.id.v_focus_scrim)
        btnFocusExit      = findViewById(R.id.btn_focus_exit)
        header            = findViewById(R.id.header)
        (header.layoutParams as ViewGroup.MarginLayoutParams).let {
            headerMarginTop    = it.topMargin
            headerMarginBottom = it.bottomMargin
        }
        setupHeader(R.drawable.ic_activity_stream, R.string.stream_title)
    }

    private fun setupVerticalFeed() {
        adapter = StreamAdapter(
            appSettingsStorage,
            thoughtStatusService,
            onDeleteThought = { thoughtToDelete ->
                showDeleteConfirmationDialog(thoughtToDelete)
            },
            onThoughtTap = { thoughtTap -> openThoughtDetails(thoughtTap) },
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
                trackHeaderForPage(position)
            }

            override fun onPageScrollStateChanged(state: Int) {
                if (state == ViewPager2.SCROLL_STATE_IDLE) setHeaderShown(isHeaderShownPending)
            }
        })
    }

    // Paging forward hides the header, paging back or the first thought shows it.
    // ! Applied once the page settles - resizing the pager mid-swipe would disturb its snapping
    private fun trackHeaderForPage(position: Int) {
        when {
            position == 0 || position < lastPagePosition -> isHeaderShownPending = true
            position > lastPagePosition                  -> isHeaderShownPending = false
        }
        lastPagePosition = position
        if (viewPager.scrollState == ViewPager2.SCROLL_STATE_IDLE) setHeaderShown(isHeaderShownPending)
    }

    private fun setHeaderShown(isShown: Boolean) {
        if (isShown == isHeaderShown) return

        // ! Behind the scrim the fold waits - resizing the pager would move the one card that is lit
        if (isFocusMode) return

        isHeaderShown = isShown
        headerAnimator?.cancel()

        // Full height is read while the header still wraps its content
        if (!isShown && header.layoutParams.height == ViewGroup.LayoutParams.WRAP_CONTENT) {
            headerFullHeight = header.height
        }

        headerAnimator = ValueAnimator.ofFloat(headerFraction, if (isShown) 1f else 0f).apply {
            duration = HEADER_ANIMATION_MS
            addUpdateListener { applyHeaderFraction(it.animatedValue as Float) }
            doOnEnd {
                // Fully shown header wraps its content again
                if (headerFraction == 1f) header.updateLayoutParams { height = ViewGroup.LayoutParams.WRAP_CONTENT }
            }
            start()
        }
    }

    // Height and margins shrink together, so the search bar slides up instead of jumping
    private fun applyHeaderFraction(fraction: Float) {
        headerFraction = fraction
        applyChromeAlpha()
        header.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            height       = (headerFullHeight * fraction).toInt()
            topMargin    = (headerMarginTop * fraction).toInt()
            bottomMargin = (headerMarginBottom * fraction).toInt()
        }
    }

    private fun setupFocusMode() {
        btnFocusExit.setOnClickListener { exitFocusMode() }
        scheduleFocusMode()
    }

    // ! Every touch postpones the dimming; with the lights out the countdown is off - the card itself works as always
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (!isFocusMode) scheduleFocusMode()
        return super.dispatchTouchEvent(ev)
    }

    private fun scheduleFocusMode() {
        focusModeHandler.removeCallbacks(focusModeRunnable)
        focusModeHandler.postDelayed(focusModeRunnable, FOCUS_IDLE_MS)
    }

    private fun enterFocusMode() {
        if (isFocusMode) return

        // A dialog or a sheet in front holds the window focus - the lights wait for the stream itself to go idle
        if (!hasWindowFocus()) {
            scheduleFocusMode()
            return
        }

        // Nothing to focus on without a card - the countdown starts again once a thought lands
        if (tvStreamEmpty.isVisible) return

        isFocusMode = true

        // The gone chrome still holds its place, so the card has to be moved to the middle of the dark screen
        focusCardShift = cardShiftToCenter()
        setFocusModeShown(true)
    }

    private fun exitFocusMode() {
        if (!isFocusMode) return
        isFocusMode = false
        setFocusModeShown(false)

        // The fold waited in the dark - the header comes back folded the way the current page wants it
        setHeaderShown(isHeaderShownPending)
        scheduleFocusMode()
    }

    // Scrim and the exit button come in as the rest of the screen goes out; the card and the FAB stay untouched
    private fun setFocusModeShown(isShown: Boolean) {
        setNavigationBarShown(!isShown)
        focusAnimator?.cancel()
        focusAnimator = ValueAnimator.ofFloat(focusFraction, if (isShown) 1f else 0f).apply {
            duration = FOCUS_ANIMATION_MS
            addUpdateListener { applyFocusFraction(it.animatedValue as Float) }
            start()
        }
    }

    // Measured on the way in, with the fold already frozen - the same shift takes the card back on the way out
    private fun cardShiftToCenter(): Float {
        val content = viewPager.parent as? View ?: return 0f
        if (viewPager.height == 0) return 0f
        return content.height / 2f - (viewPager.top + viewPager.height / 2f)
    }

    private fun applyFocusFraction(fraction: Float) {
        focusFraction          = fraction
        viewPager.translationY = focusCardShift * fraction
        vFocusScrim.alpha      = fraction
        btnFocusExit.alpha     = fraction

        vFocusScrim.isInvisible  = fraction == 0f
        btnFocusExit.isInvisible = fraction == 0f
        applyChromeAlpha()
    }

    // ! The folding header and the scrim both set these alphas, so one formula keeps the two fractions together
    private fun applyChromeAlpha() {
        val chromeAlpha = 1f - focusFraction
        header.alpha = headerFraction * chromeAlpha

        // Transparent is not enough - a faded out view would still catch the taps meant for the card
        header.isInvisible = header.alpha == 0f
        listOf(cvSearchBar, btnFilter, btnSort, flScrollIndicator).forEach {
            it.alpha = chromeAlpha
            it.isInvisible = chromeAlpha == 0f
        }
    }

    private fun openThoughtDetails(thought: ThoughtDTO) {
        val intent = Intent(this, DetailsActivity::class.java)
        intent.putExtra(DetailsActivity.P_SELECTED_THOUGHT_ID, thought.id ?: -1)
        startActivity(intent)
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
    // Every thought with the tag, whatever its state - dormant ones included
    private fun applyLaunchSearchCriteria() {
        val criteria = IntentCompat.getParcelableExtra(intent, P_SEARCH_CRITERIA, HexTags::class.java) ?: return
        showAllStates()
        searcher.setTags(criteria)
    }

    // Domain passed by the caller (a Metadata domain tile) - every thought in it, whatever its state
    private fun applyLaunchDomainFilter() {
        if (!intent.hasExtra(P_DOMAIN_ID)) return
        showAllStates()
        viewModel.updateSelectedDomain(intent.getIntExtra(P_DOMAIN_ID, -1))
    }

    private fun showAllStates() {
        viewModel.updateShowActive(true)
        viewModel.updateShowNearDormant(true)
        viewModel.updateShowDormant(true)
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

        // Back on the screen with the light on, and the countdown from the start
        exitFocusMode()
        scheduleFocusMode()

        val completedTask = onboardingManager.takeCompletedActiveTask()
        if (completedTask != null) {
            showTaskCompletedDialog(completedTask)
        }
        else {
            showCaptureTaskGuide()
            showTagTaskGuide()
        }
    }

    override fun onPause() {
        super.onPause()
        focusModeHandler.removeCallbacks(focusModeRunnable)
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

    // Tag thought task starts here - once per run, on the thought in view; called again once the list lands
    private fun showTagTaskGuide() {
        val task = OnboardingTask.TAG_THOUGHT
        if (!onboardingManager.shouldShowTaskGuide(task, OnboardingTaskGuide.STREAM_THOUGHT)) return

        val position = viewPager.currentItem
        val thought = adapter.currentList.getOrNull(position) ?: return
        val card = (viewPager.getChildAt(0) as? RecyclerView)?.findViewHolderForAdapterPosition(position)?.itemView ?: return

        onboardingManager.markTaskGuideShown(task, OnboardingTaskGuide.STREAM_THOUGHT)
        HexSpotlight.show(
            activity   = this,
            target     = card,
            text       = getString(R.string.stream_onboarding_task_open_thought),
            buttonText = getString(R.string.common_spotlight_btn_got_it),
            holeShape  = HexSpotlight.HoleShape.ROUNDED_RECT
        ) {
            openThoughtDetails(thought)
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
        if (!isListEmpty) {
            scheduleFocusMode()
            return
        }

        // No page to swipe back to - the header has to be visible
        lastPagePosition = 0
        isHeaderShownPending = true
        setHeaderShown(true)

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

                // ! The card is there only once the pager laid out the committed list
                viewPager.post { if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) showTagTaskGuide() }
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
        const val P_DOMAIN_ID         = "domain_id"

        private const val FADING_EDGE_DP = 16
        private const val HEADER_ANIMATION_MS = 200L
        private const val FOCUS_ANIMATION_MS = 250L
        private const val FOCUS_IDLE_MS = 5_000L
    }
}
