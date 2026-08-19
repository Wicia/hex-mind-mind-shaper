package pl.hexmind.mindshaper.activities.workshop

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.activities.CoreActivity
import pl.hexmind.mindshaper.activities.capture.CaptureActivity
import pl.hexmind.mindshaper.activities.details.DetailsActivity
import pl.hexmind.mindshaper.activities.home.HomeActivity
import pl.hexmind.mindshaper.activities.settings.SettingsActivity
import pl.hexmind.mindshaper.activities.stream.StreamActivity
import kotlinx.coroutines.launch
import pl.hexmind.mindshaper.common.onboarding.OnboardingSection
import pl.hexmind.mindshaper.common.ui.dpToPx
import pl.hexmind.mindshaper.common.ui.dialogs.ActionsDialog
import pl.hexmind.mindshaper.common.ui.dialogs.TextEditDialog
import pl.hexmind.mindshaper.common.ui.views.lists.InsetDividerDecoration
import pl.hexmind.mindshaper.database.models.PathEntity

@AndroidEntryPoint
class WorkshopActivity : CoreActivity() {

    private val viewModel: WorkshopViewModel by viewModels()

    // TABS
    private lateinit var tabsWorkshop: TabLayout
    private lateinit var cardGoals: View
    private lateinit var cardPaths: View

    // GOALS
    private lateinit var rvGoals: RecyclerView
    private lateinit var btnAddGoal: MaterialButton
    private lateinit var btnGoalsToggle: MaterialButton
    private lateinit var fabCapture: FloatingActionButton
    private lateinit var goalsAdapter: GoalsAdapter
    private lateinit var llGoalsArchive: View
    private lateinit var llGoalsArchiveHeader: View
    private lateinit var llGoalsArchiveContent: View
    private lateinit var tvGoalsArchiveHeader: TextView
    private lateinit var ivGoalsArchiveChevron: ImageView
    private lateinit var rvGoalsArchive: RecyclerView
    private lateinit var btnGoalsArchiveMore: MaterialButton
    private lateinit var archiveAdapter: GoalsAdapter

    private var allGoals: List<Goal> = emptyList()
    private var allArchivedGoals: List<Goal> = emptyList()
    private var archiveVisibleCount: Int = ARCHIVE_PAGE_SIZE
    private var isArchiveExpanded: Boolean = false
    private var isGoalsExpanded: Boolean = false

    // PATHS
    private lateinit var llPathsList: LinearLayout
    private lateinit var tvPoolEmpty: TextView
    private lateinit var btnPathsReset: MaterialButton
    private lateinit var cardAnimator: PathCardAnimator

    // ONBOARDING
    private lateinit var rvOnboardingSections: RecyclerView
    private lateinit var onboardingAdapter: OnboardingSectionsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.workshop_activity)

        initializeViews()
        setupGoalsList()
        setupOnboardingList()
        setupFab()
        observeGoals()
        observePaths()
    }

    override fun onResume() {
        super.onResume()
        // Reload goals after returning from GoalDetailActivity (goal's steps may change :)
        viewModel.reload()
        // Tooltips may have been seen on another screen in the meantime
        if (::onboardingAdapter.isInitialized) {
            onboardingAdapter.notifyDataSetChanged()
        }
    }

    // ── Init ───────────────────────────────────────────────────────────────────

    private fun initializeViews() {
        setupHeader(R.drawable.ic_activity_workshop, R.string.workshop_title)

        rvGoals        = findViewById(R.id.rv_goals)
        btnAddGoal     = findViewById(R.id.btn_add_goal)
        btnGoalsToggle = findViewById(R.id.btn_goals_toggle)
        llGoalsArchive        = findViewById(R.id.ll_goals_archive)
        llGoalsArchiveHeader  = findViewById(R.id.ll_goals_archive_header)
        llGoalsArchiveContent = findViewById(R.id.ll_goals_archive_content)
        tvGoalsArchiveHeader  = findViewById(R.id.tv_goals_archive_header)
        ivGoalsArchiveChevron = findViewById(R.id.iv_goals_archive_chevron)
        rvGoalsArchive        = findViewById(R.id.rv_goals_archive)
        btnGoalsArchiveMore   = findViewById(R.id.btn_goals_archive_more)
        fabCapture     = findViewById(R.id.fab_capture)

        btnAddGoal.visibility = View.VISIBLE
        btnAddGoal.setOnClickListener { showAddGoalDialog() }
        btnGoalsToggle.setOnClickListener { toggleGoalsSection() }
        btnGoalsArchiveMore.setOnClickListener { showMoreArchivedGoals() }
        llGoalsArchiveHeader.setOnClickListener { toggleArchive() }

        llPathsList   = findViewById(R.id.ll_paths_list)
        tvPoolEmpty   = findViewById(R.id.tv_paths_pool_empty)
        btnPathsReset = findViewById(R.id.btn_paths_reset)
        btnPathsReset.setOnClickListener { showResetPathsDialog() }

        cardAnimator = PathCardAnimator(viewModel)

        setupTabs()
    }

    // ── Tabs ────────────────────────────────────────────────────────────────

    // both sections stay inflated; tabs only swap visibility
    private fun setupTabs() {
        tabsWorkshop = findViewById(R.id.tabs_workshop)
        cardGoals    = findViewById(R.id.card_goals)
        cardPaths    = findViewById(R.id.card_paths)

        tabsWorkshop.addTab(newSectionTab(R.string.workshop_tab_goals))
        tabsWorkshop.addTab(newSectionTab(R.string.workshop_tab_paths))

        tabsWorkshop.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) = showTab(tab.position)
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        showTab(TAB_GOALS)
    }

    private fun newSectionTab(titleRes: Int): TabLayout.Tab {
        val title = layoutInflater.inflate(R.layout.tab_section_header, tabsWorkshop, false) as TextView
        title.setText(titleRes)
        return tabsWorkshop.newTab().apply { customView = title }
    }

    private fun showTab(position: Int) {
        val goalsVisible = position == TAB_GOALS
        cardGoals.visibility = if (goalsVisible) View.VISIBLE else View.GONE
        cardPaths.visibility = if (goalsVisible) View.GONE else View.VISIBLE
    }

    // ── Onboarding review ───────────────────────────────────────────────────

    private fun setupOnboardingList() {
        rvOnboardingSections = findViewById(R.id.rv_onboarding_sections)

        onboardingAdapter = OnboardingSectionsAdapter(
            onOpenScreen   = { section -> openSectionScreen(section) },
            onResetSection = { section -> showResetOnboardingDialog(section) },
            tipsProvider   = { section -> onboardingManager.getSectionTips(section) }
        )

        rvOnboardingSections.apply {
            layoutManager = LinearLayoutManager(this@WorkshopActivity)
            adapter = onboardingAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun showResetOnboardingDialog(section: OnboardingSection) {
        ActionsDialog.Builder(this)
            .setTitle(getString(R.string.common_caution_dialog_title))
            .setDescription(getString(R.string.workshop_dialog_reset_onboarding))
            .setCautionAction(getString(R.string.common_btn_yes)) {
                onboardingManager.resetSection(section)
                onboardingAdapter.notifyDataSetChanged()
            }
            .setDismissText(getString(R.string.common_btn_no))
            .show()
    }

    private fun openSectionScreen(section: OnboardingSection) {
        val intent = when (section) {
            OnboardingSection.HOME     -> Intent(this, HomeActivity::class.java)
            OnboardingSection.SETTINGS -> Intent(this, SettingsActivity::class.java)
            OnboardingSection.CAPTURE  -> Intent(this, CaptureActivity::class.java)
            OnboardingSection.STREAM   -> Intent(this, StreamActivity::class.java)
            OnboardingSection.DETAILS  -> return openNewestThought() // "Go to Details" needs a thought to open
        }

        startActivity(intent)
    }

    private fun openNewestThought() {
        lifecycleScope.launch {
            val thoughtId = viewModel.getNewestThoughtId()

            if (thoughtId == null)
                showShortToast(R.string.workshop_onboarding_no_thoughts)
            else {
                val intent = Intent(this@WorkshopActivity, DetailsActivity::class.java)
                    .putExtra(DetailsActivity.P_SELECTED_THOUGHT_ID, thoughtId)

                startActivity(intent)
            }
        }
    }

    private fun setupGoalsList() {
        goalsAdapter = GoalsAdapter(
            onGoalTap      = { goalId -> openGoalDetail(goalId) },
            onCycleImportance = { goalId -> viewModel.cycleGoalImportance(goalId) },
            onGoalLongPress = { goalId -> showDeleteGoalConfirmation(goalId) }
        )
        rvGoals.apply {
            layoutManager = LinearLayoutManager(this@WorkshopActivity)
            adapter = goalsAdapter
            isNestedScrollingEnabled = false
            // Inset to separate list elements
            addItemDecoration(InsetDividerDecoration(this@WorkshopActivity, GOAL_DIVIDER_INSET_DP))
        }

        //  same adapter class as the main list
        archiveAdapter = GoalsAdapter(
            onGoalTap         = { goalId -> openGoalDetail(goalId) },
            onCycleImportance = { goalId -> viewModel.cycleGoalImportance(goalId) },
            onGoalLongPress   = { goalId -> showArchivedGoalActions(goalId) }
        )
        rvGoalsArchive.apply {
            layoutManager = LinearLayoutManager(this@WorkshopActivity)
            adapter = archiveAdapter
            isNestedScrollingEnabled = false
            // Darker divider fur ARCHIVED state
            addItemDecoration(
                InsetDividerDecoration(
                    this@WorkshopActivity,
                    GOAL_DIVIDER_INSET_DP,
                    dividerColorRes = R.color._gray_lvl_2
                )
            )
        }
    }

    private fun setupFab() {
        fabCapture.setOnClickListener {
            val intent = Intent(this, CaptureActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }
    }

    private fun observeGoals() {
        viewModel.goals.observe(this) { goals ->
            allGoals = goals
            renderGoalsList()
        }
        viewModel.archivedGoals.observe(this) { goals ->
            allArchivedGoals = goals
            renderArchiveList()
        }
    }

    // Shows up to MAX_COLLAPSED_GOALS by default; toggle button reveals/hides the rest
    private fun renderGoalsList() {
        // Collapse again when the list shrinks below the threshold, so the state stays predictable
        if (allGoals.size <= MAX_COLLAPSED_GOALS) isGoalsExpanded = false

        val visibleGoals = if (isGoalsExpanded || allGoals.size <= MAX_COLLAPSED_GOALS)
            allGoals
        else
            allGoals.take(MAX_COLLAPSED_GOALS)

        goalsAdapter.submitList(visibleGoals)

        btnGoalsToggle.visibility = if (allGoals.size > MAX_COLLAPSED_GOALS) View.VISIBLE else View.GONE
        if (isGoalsExpanded) {
            btnGoalsToggle.text = getString(R.string.workshop_goals_show_less)
        }
        else {
            btnGoalsToggle.text = getString(R.string.workshop_goals_show_all, allGoals.size - MAX_COLLAPSED_GOALS)
        }
    }

    private fun toggleGoalsSection() {
        isGoalsExpanded = !isGoalsExpanded
        renderGoalsList()
    }

    // Feed: starts at ARCHIVE_PAGE_SIZE and grows by the same step
    private fun renderArchiveList() {
        llGoalsArchive.visibility = if (allArchivedGoals.isEmpty()) View.GONE else View.VISIBLE

        // Count shows the whole archive, not just the loaded page
        tvGoalsArchiveHeader.text = getString(
            R.string.workshop_section_goals_archive_count, allArchivedGoals.size
        )

        // ! Shrink back when the archive gets smaller, so the feed can't stay stuck open
        archiveVisibleCount = archiveVisibleCount.coerceAtMost(
            maxOf(ARCHIVE_PAGE_SIZE, allArchivedGoals.size)
        )

        archiveAdapter.submitList(allArchivedGoals.take(archiveVisibleCount))

        val remaining = allArchivedGoals.size - archiveVisibleCount
        btnGoalsArchiveMore.visibility = if (remaining > 0) View.VISIBLE else View.GONE
        btnGoalsArchiveMore.text = getString(R.string.workshop_goals_archive_more)

        applyArchiveExpansion()
    }

    private fun toggleArchive() {
        isArchiveExpanded = !isArchiveExpanded
        applyArchiveExpansion()
    }

    // Chevron: collapsed / expanded
    private fun applyArchiveExpansion() {
        llGoalsArchiveContent.visibility = if (isArchiveExpanded) View.VISIBLE else View.GONE
        ivGoalsArchiveChevron.rotation = if (isArchiveExpanded) 270f else 90f
    }

    private fun showMoreArchivedGoals() {
        archiveVisibleCount += ARCHIVE_PAGE_SIZE
        renderArchiveList()
    }

    // ── Navigation ─────────────────────────────────────────────────────────────

    private fun openGoalDetail(goalId: Int) {
        startActivity(GoalDetailActivity.newIntent(this, goalId))
    }

    // ── Paths ──────────────────────────────────────────────────────────────────

    private fun observePaths() {
        viewModel.pickedPaths.observe(this) { paths -> rebuildPathCards(paths) }
    }

    private fun rebuildPathCards(paths: List<PathItem>) {
        llPathsList.removeAllViews()

        if (paths.isEmpty()) {
            tvPoolEmpty.visibility = View.VISIBLE
            return
        }
        tvPoolEmpty.visibility = View.GONE

        for (slot in 0 until 2) {
            val path = paths.getOrNull(slot) ?: continue

            val view: View = LayoutInflater.from(this)
                .inflate(R.layout.path_item, llPathsList, false)
                .also { bindPathCard(it, path) }

            // ! keeping the params from inflate: replacing them dropped the card's fixed height,
            // and the weighted spacer in the button column then expanded to the whole viewport
            if (slot > 0) {
                (view.layoutParams as LinearLayout.LayoutParams).topMargin = dpToPx(8)
            }
            llPathsList.addView(view)

            cardAnimator.onCardBuilt(view, path.pathKey)
        }
    }

    private fun bindPathCard(cardView: View, path: PathItem) {
        val card = cardView.findViewById<View>(R.id.ll_path_card)
        val btnPathProceed = cardView.findViewById<MaterialButton>(R.id.btn_path_proceed_action)
        val tvContent = cardView.findViewById<TextView>(R.id.tv_path_content)
        val tvStepsCount = cardView.findViewById<TextView>(R.id.tv_workshop_path_steps_count)
        val btnRepick = cardView.findViewById<MaterialButton>(R.id.btn_path_redraw)

        val isUnselected = path.status == PathEntity.STATUS_UNSELECTED

        val bgColor = if (isUnselected)
            ContextCompat.getColor(this, R.color._gray_lvl_1)
        else
            ContextCompat.getColor(this, R.color._orange_lvl_1)
        (card.background?.mutate() as? GradientDrawable)?.setColor(bgColor)
            ?: card.setBackgroundColor(bgColor)

        when {
            isUnselected -> {
                btnPathProceed.setIconResource(R.drawable.ic_path_reveal)
                btnPathProceed.setOnClickListener { cardAnimator.revealWithFlip(cardView, path.pathKey) }
            }
            path.isLastStep -> {
                btnPathProceed.setIconResource(R.drawable.ic_path_step_complete)
                btnPathProceed.setOnClickListener { cardAnimator.advanceLastStepWithSlideOut(cardView, path.pathKey) }
            }
            else -> {
                btnPathProceed.setIconResource(R.drawable.ic_path_step_complete)
                btnPathProceed.setOnClickListener { cardAnimator.advanceWithFade(cardView, path.pathKey) }
            }
        }
        btnRepick.setOnClickListener { viewModel.repickPath(path.pathKey) }

        if (isUnselected) {
            tvContent.text = path.category
            tvStepsCount.visibility = View.VISIBLE
            tvStepsCount.text = resources.getQuantityString(
                R.plurals.workshop_path_steps_count, path.totalSteps, path.totalSteps
            )
        }
        else {
            tvContent.text = path.currentStepContent
            tvStepsCount.visibility = View.GONE
        }
    }

    private fun showResetPathsDialog() {
        ActionsDialog.Builder(this)
            .setTitle(getString(R.string.common_caution_dialog_title))
            .setDescription(getString(R.string.workshop_dialog_reset_paths_title))
            .setCautionAction(getString(R.string.common_btn_yes)) {
                viewModel.resetAllPaths()
            }
            .setDismissText(getString(R.string.common_btn_no))
            .show()
    }

    // ── Dialogs: goals ─────────────────────────────────────────────────────────

    private fun showAddGoalDialog() {
        TextEditDialog(
            context = this,
            title = getString(R.string.workshop_dialog_add_goal),
            notesStyle = false,
            textInput = "",
            onSave = { description -> viewModel.addGoal(description) }
        ).show()
    }

    private fun showDeleteGoalConfirmation(goalId: Int) {
        ActionsDialog.Builder(this)
            .setTitle(getString(R.string.workshop_dialog_delete_goal_title))
            .setDescription(getString(R.string.workshop_dialog_delete_goal_desc))
            .setCautionAction(getString(R.string.common_deletion_dialog_yes)) {
                viewModel.deleteGoal(goalId)
            }
            .setStandardAction(getString(R.string.workshop_dialog_goal_archive)) {
                viewModel.archiveGoal(goalId)
            }
            .setDismissText(getString(R.string.common_btn_cancel))
            .show()
    }

    private fun showArchivedGoalActions(goalId: Int) {
        ActionsDialog.Builder(this)
            .setTitle(getString(R.string.workshop_dialog_delete_goal_title))
            .setDescription(getString(R.string.workshop_dialog_delete_goal_desc))
            .setCautionAction(getString(R.string.common_deletion_dialog_yes)) {
                viewModel.deleteGoal(goalId)
            }
            .setStandardAction(getString(R.string.workshop_dialog_goal_restore)) {
                viewModel.restoreGoal(goalId)
            }
            .setDismissText(getString(R.string.common_btn_cancel))
            .show()
    }

    companion object {
        private const val MAX_COLLAPSED_GOALS = 6
        private const val GOAL_DIVIDER_INSET_DP = 28
        private const val ARCHIVE_PAGE_SIZE = 5

        private const val TAB_GOALS = 0
    }
}