package pl.hexmind.mindshaper.activities.workshop

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.activities.CoreActivity
import pl.hexmind.mindshaper.common.ui.dpToPx
import pl.hexmind.mindshaper.common.ui.dialogs.ActionsDialog
import pl.hexmind.mindshaper.common.ui.dialogs.TextEditDialog
import pl.hexmind.mindshaper.common.ui.views.HexPullTab
import pl.hexmind.mindshaper.common.ui.views.HexTabSwitch
import pl.hexmind.mindshaper.common.ui.views.lists.InsetDividerDecoration
import pl.hexmind.mindshaper.database.models.PathEntity

@AndroidEntryPoint
class WorkshopActivity : CoreActivity() {

    private val viewModel: WorkshopViewModel by viewModels()

    // TABS
    private lateinit var tabsWorkshop: HexTabSwitch
    private lateinit var cardGoals: View
    private lateinit var cardPaths: View

    // GOALS
    private lateinit var rvGoals: RecyclerView
    private lateinit var ptGoalsToggle: HexPullTab
    private lateinit var fabAddGoal: FloatingActionButton
    private lateinit var goalsAdapter: GoalsAdapter
    private lateinit var tvGoalsHeader: TextView
    private lateinit var btnGoalsView: MaterialButton
    private lateinit var tvGoalsEmpty: TextView

    private var allGoals: List<Goal> = emptyList()
    private var allArchivedGoals: List<Goal> = emptyList()
    // One list for both: active goals or the archive (chosen in WorkshopGoalsViewBottomSheet); active on every entry
    private var isArchiveShown: Boolean = false
    private var isGoalsExpanded: Boolean = false

    // PATHS
    private lateinit var llPathsList: LinearLayout
    private lateinit var tvPoolEmpty: TextView
    private lateinit var btnPathsReset: MaterialButton
    private lateinit var cardAnimator: PathCardAnimator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.workshop_activity)

        initializeViews()
        setupGoalsList()
        setupFab()
        observeGoals()
        observePaths()
    }

    override fun onResume() {
        super.onResume()
        // Reload goals after returning from GoalDetailActivity (goal's steps may change :)
        viewModel.reload()
    }

    // ── Init ───────────────────────────────────────────────────────────────────

    private fun initializeViews() {
        setupHeader(R.drawable.ic_activity_workshop, R.string.workshop_title)

        rvGoals        = findViewById(R.id.rv_goals)
        ptGoalsToggle  = findViewById(R.id.pt_goals_toggle)
        tvGoalsHeader  = findViewById(R.id.tv_goals_section_header)
        btnGoalsView   = findViewById(R.id.btn_goals_view)
        fabAddGoal     = findViewById(R.id.fab_add_goal)
        tvGoalsEmpty   = findViewById(R.id.tv_goals_empty)

        ptGoalsToggle.setOnClickListener { toggleGoalsSection() }
        btnGoalsView.setOnClickListener { showGoalsViewSheet() }

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

        tabsWorkshop.setTabs(
            listOf(
                HexTabSwitch.Tab(getString(R.string.workshop_tab_goals), iconRes = R.drawable.ic_tab_goals),
                HexTabSwitch.Tab(getString(R.string.workshop_tab_paths), iconRes = R.drawable.ic_tab_paths)
            ),
            initialIndex = TAB_GOALS
        )
        tabsWorkshop.onTabSelected = { position -> showTab(position) }

        showTab(TAB_GOALS)
    }

    private fun showTab(position: Int) {
        val goalsVisible = position == TAB_GOALS
        cardGoals.visibility = if (goalsVisible) View.VISIBLE else View.GONE
        cardPaths.visibility = if (goalsVisible) View.GONE else View.VISIBLE
        updateFab()
    }

    // FAB adds an active goal - Goals tab with the active goals only (a goal added over the archive would not show up)
    private fun updateFab() {
        val isGoalsTab = cardGoals.visibility == View.VISIBLE
        if (isGoalsTab && !isArchiveShown) fabAddGoal.show() else fabAddGoal.hide()
    }

    private fun setupGoalsList() {
        goalsAdapter = GoalsAdapter(
            onGoalTap      = { goalId -> openGoalDetail(goalId) },
            onCycleImportance = { goalId -> viewModel.cycleGoalImportance(goalId) },
            // Active: delete / archive; archived: delete / restore
            onGoalLongPress = { goalId ->
                if (isArchiveShown) showArchivedGoalActions(goalId) else showDeleteGoalConfirmation(goalId)
            }
        )
        rvGoals.apply {
            layoutManager = LinearLayoutManager(this@WorkshopActivity)
            adapter = goalsAdapter
            isNestedScrollingEnabled = false
            // Inset to separate list elements
            addItemDecoration(InsetDividerDecoration(this@WorkshopActivity, GOAL_DIVIDER_INSET_DP))
        }
    }

    private fun setupFab() {
        fabAddGoal.setOnClickListener { showAddGoalDialog() }
    }

    private fun observeGoals() {
        viewModel.goals.observe(this) { goals ->
            allGoals = goals
            renderGoalsList()
        }
        viewModel.archivedGoals.observe(this) { goals ->
            allArchivedGoals = goals
            // Last one restored / deleted - nothing left to show, back to the active goals
            if (isArchiveShown && goals.isEmpty()) showArchive(false)
            else renderGoalsList()
        }
    }

    // Shows up to MAX_COLLAPSED_GOALS by default; toggle button reveals/hides the rest - the same for the archive
    private fun renderGoalsList() {
        val shownGoals = if (isArchiveShown) allArchivedGoals else allGoals

        tvGoalsHeader.setText(
            if (isArchiveShown) R.string.workshop_section_goals_archived else R.string.workshop_section_goals_active
        )

        // Collapse again when the list shrinks below the threshold, so the state stays predictable
        if (shownGoals.size <= MAX_COLLAPSED_GOALS) isGoalsExpanded = false

        val visibleGoals = if (isGoalsExpanded || shownGoals.size <= MAX_COLLAPSED_GOALS)
            shownGoals
        else
            shownGoals.take(MAX_COLLAPSED_GOALS)

        goalsAdapter.submitList(visibleGoals)
        // Archive never stays empty on screen - it switches back to the active goals
        tvGoalsEmpty.visibility = if (shownGoals.isEmpty()) View.VISIBLE else View.GONE

        ptGoalsToggle.visibility = if (shownGoals.size > MAX_COLLAPSED_GOALS) View.VISIBLE else View.GONE
        ptGoalsToggle.setExpanded(isGoalsExpanded)
        if (isGoalsExpanded) {
            ptGoalsToggle.setText(getString(R.string.workshop_goals_show_less))
        }
        else {
            ptGoalsToggle.setText(getString(R.string.workshop_goals_show_all, shownGoals.size - MAX_COLLAPSED_GOALS))
        }
    }

    private fun toggleGoalsSection() {
        isGoalsExpanded = !isGoalsExpanded
        renderGoalsList()
    }

    private fun showGoalsViewSheet() {
        WorkshopGoalsViewBottomSheet(
            isArchiveShown = isArchiveShown,
            isArchiveEmpty = allArchivedGoals.isEmpty(),
            onViewSelected = { archive -> showArchive(archive) }
        ).show(supportFragmentManager, WorkshopGoalsViewBottomSheet.TAG)
    }

    // Switching lists starts collapsed - the expanded state belonged to the other list
    private fun showArchive(show: Boolean) {
        if (isArchiveShown != show) isGoalsExpanded = false
        isArchiveShown = show
        renderGoalsList()
        updateFab()
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
            ContextCompat.getColor(this, R.color._palette_lvl_1)
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
            .setTitle(getString(R.string.workshop_dialog_reset_paths_title))
            .setDescription(getString(R.string.workshop_dialog_reset_paths_desc))
            .setPrimaryAction(getString(R.string.workshop_dialog_reset_paths_yes), caution = true) {
                viewModel.resetAllPaths()
            }
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
            .setSecondaryAction(getString(R.string.workshop_dialog_goal_archive)) {
                viewModel.archiveGoal(goalId)
            }
            .setPrimaryAction(getString(R.string.workshop_dialog_delete_goal_yes), caution = true) {
                viewModel.deleteGoal(goalId)
            }
            .show()
    }

    private fun showArchivedGoalActions(goalId: Int) {
        ActionsDialog.Builder(this)
            .setTitle(getString(R.string.workshop_dialog_delete_archived_goal_title))
            .setDescription(getString(R.string.workshop_dialog_delete_archived_goal_desc))
            .setSecondaryAction(getString(R.string.workshop_dialog_goal_restore)) {
                viewModel.restoreGoal(goalId)
            }
            .setPrimaryAction(getString(R.string.workshop_dialog_delete_goal_yes), caution = true) {
                viewModel.deleteGoal(goalId)
            }
            .show()
    }

    companion object {
        private const val MAX_COLLAPSED_GOALS = 6
        private const val GOAL_DIVIDER_INSET_DP = 28

        private const val TAB_GOALS = 0
    }
}