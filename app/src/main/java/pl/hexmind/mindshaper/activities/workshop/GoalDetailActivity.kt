package pl.hexmind.mindshaper.activities.workshop

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.activities.CoreActivity
import pl.hexmind.mindshaper.activities.capture.CaptureActivity
import pl.hexmind.mindshaper.activities.details.DetailsActivity
import pl.hexmind.mindshaper.common.ui.dialogs.ActionsDialog
import pl.hexmind.mindshaper.common.ui.dialogs.TextEditDialog
import pl.hexmind.mindshaper.common.ui.views.GoalImportanceBadge
import pl.hexmind.mindshaper.common.ui.views.lists.InsetDividerDecoration

@AndroidEntryPoint
class GoalDetailActivity : CoreActivity() {

    companion object {
        // Intent extra key — must match SavedStateHandle key in GoalDetailViewModel
        private const val EXTRA_GOAL_ID = "goalId"

        // Row padding
        private const val STEP_DIVIDER_INSET_DP = 24

        fun newIntent(context: Context, goalId: Int): Intent =
            Intent(context, GoalDetailActivity::class.java)
                .putExtra(EXTRA_GOAL_ID, goalId)
    }

    private val viewModel: GoalDetailViewModel by viewModels()

    private lateinit var tvBadge: GoalImportanceBadge
    private lateinit var cbGoalDesc: MaterialButton
    private lateinit var rvSteps: RecyclerView
    private lateinit var rvStepsCompleted: RecyclerView
    private lateinit var llStepsCompleted: View
    private lateinit var vStepsSeparator: View
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var btnQuickComplete: MaterialButton
    private lateinit var stepsAdapter: GoalDetailStepsAdapter
    private lateinit var completedStepsAdapter: GoalDetailStepsAdapter

    // Step which launched CaptureActivity
    private var pendingLinkStepId: Int? = null

    // Handling receiving newly-created thoughtId in the result from CaptureActivity
    private val captureActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val thoughtId = result.data?.getLongExtra(CaptureActivity.EXTRA_THOUGHT_ID, -1L) ?: -1L
            val stepId = pendingLinkStepId
            if (thoughtId > 0 && stepId != null) {
                viewModel.linkThought(stepId, thoughtId.toInt())
            }
        }
        pendingLinkStepId = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.goal_detail_activity)
        // No header widget

        initViews()
        setupStepsList()
        setupFab()
        observeGoal()
    }

    // ── Init ───────────────────────────────────────────────────────────────────

    private fun initViews() {
        tvBadge          = findViewById(R.id.tv_goal_importance_badge)
        cbGoalDesc       = findViewById(R.id.cb_goal_description)
        rvSteps          = findViewById(R.id.rv_steps)
        rvStepsCompleted = findViewById(R.id.rv_steps_completed)
        llStepsCompleted = findViewById(R.id.ll_steps_completed)
        vStepsSeparator  = findViewById(R.id.v_steps_separator)
        fabAdd           = findViewById(R.id.fab_add_step)
        btnQuickComplete = findViewById(R.id.btn_quick_complete)
    }

    private fun setupStepsList() {
        stepsAdapter          = buildStepsAdapter()
        completedStepsAdapter = buildStepsAdapter()

        rvSteps.apply {
            layoutManager = LinearLayoutManager(this@GoalDetailActivity)
            adapter = stepsAdapter
            isNestedScrollingEnabled = false
            //TODO: info - separators consistent with the goals list in WorkshopActivity
            addItemDecoration(InsetDividerDecoration(this@GoalDetailActivity, STEP_DIVIDER_INSET_DP))
        }

        rvStepsCompleted.apply {
            layoutManager = LinearLayoutManager(this@GoalDetailActivity)
            adapter = completedStepsAdapter
            isNestedScrollingEnabled = false
            addItemDecoration(InsetDividerDecoration(this@GoalDetailActivity, STEP_DIVIDER_INSET_DP))
        }
    }

    // Two lists share one adapter class, so isFirst/isLast stay correct within each block
    private fun buildStepsAdapter() = GoalDetailStepsAdapter(
        onTapText              = { step -> showEditStepSheet(step) },
        onLongPressText        = { id -> showDeleteStepConfirmation(id) },
        onTapRing              = { id -> viewModel.incrementStep(id) },
        onLongPressRing        = { id -> viewModel.decrementStep(id) },
        onMenuClick            = { anchor, step, isFirst, isLast ->
            showStepMenu(anchor, step, isFirst, isLast)
        },
        onThoughtChipClick     = { thoughtId -> openThoughtDetails(thoughtId) },
        onThoughtChipLongPress = { id -> showUnlinkThoughtDialog(id) }
    )

    private fun setupFab() {
        fabAdd.setOnClickListener { showAddStepSheet() }
        btnQuickComplete.setOnClickListener {
            if (viewModel.quickCompleteAll()) {
                showShortToast(R.string.workshop_goal_quick_complete_done)
            }
        }
    }

    // ── Observe ────────────────────────────────────────────────────────────────

    private fun observeGoal() {
        viewModel.goal.observe(this) { goal ->
            goal ?: return@observe
            bindHeader(goal)
            bindSteps(goal.subItems)
            updateQuickCompleteButtonVisibility(goal.subItems)
        }
    }

    private fun bindSteps(steps: List<GoalStep>) {
        val (completedSteps, activeSteps) = steps.partition { step -> step.isCompleted }

        stepsAdapter.setItems(activeSteps)
        completedStepsAdapter.setItems(completedSteps)

        llStepsCompleted.visibility = if (completedSteps.isEmpty()) View.GONE else View.VISIBLE
        // Nothing above it to separate when every step is done
        vStepsSeparator.visibility = if (activeSteps.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun updateQuickCompleteButtonVisibility(steps: List<GoalStep>) {
        val shouldShow = steps.isNotEmpty()
            && steps.all { it.maxRepetitions == 1 }
            && steps.any { !it.isCompleted }
        btnQuickComplete.visibility = if (shouldShow) View.VISIBLE else View.GONE
    }

    private fun bindHeader(goal: Goal) {
        tvBadge.setImportance(goal.importance)
        // importance == 0 -> archived goal: badge is inert here too, mirroring the list
        if (goal.importance <= 0) {
            tvBadge.setOnClickListener(null)
            tvBadge.isClickable = false
        }
        else {
            tvBadge.setOnClickListener { viewModel.cycleGoalImportance() }
        }

        cbGoalDesc.text = goal.description
        cbGoalDesc.setOnClickListener { showEditGoalDescriptionDialog(goal) }
    }

    // ── Step menu ────────────────────────────────────────────────────────────────────────────────

    private fun showStepMenu(anchor: View, step: GoalStep, isFirst: Boolean, isLast: Boolean) {
        val popup = PopupMenu(this, anchor)
        popup.menuInflater.inflate(R.menu.menu_goal_detail_step, popup.menu) // TODO: replace with more cool/fancy UI menu

        // Order of the completed block is driven by completion, not by the user
        val allowReorder = !step.isCompleted
        popup.menu.findItem(R.id.action_move_up).isVisible = allowReorder
        popup.menu.findItem(R.id.action_move_down).isVisible = allowReorder

        // Disable move up/down at list edges
        popup.menu.findItem(R.id.action_move_up).isEnabled = !isFirst
        popup.menu.findItem(R.id.action_move_down).isEnabled = !isLast

        // Label of "link thought" depends on whether one is already linked
        val linkItem = popup.menu.findItem(R.id.action_link_thought)
        linkItem.title = getString(
            if (step.hasLinkedThought) R.string.workshop_step_menu_change_thought
            else R.string.workshop_step_menu_link_thought
        )

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_move_up   -> { viewModel.moveStepUp(step.id); true }
                R.id.action_move_down -> { viewModel.moveStepDown(step.id); true }
                R.id.action_link_thought -> {
                    launchCaptureForStep(step)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun launchCaptureForStep(step: GoalStep) {
        pendingLinkStepId = step.id
        // If step already has a linked thought, the old link is replaced after CaptureActivity returns
        captureActivityLauncher.launch(Intent(this, CaptureActivity::class.java))
    }

    // ── Linked thought ───────────────────────────────────────────────────────────────────────────

    private fun openThoughtDetails(thoughtId: Int) {
        val intent = Intent(this, DetailsActivity::class.java)
            .putExtra(DetailsActivity.P_SELECTED_THOUGHT_ID, thoughtId)
        startActivity(intent)
    }

    private fun showUnlinkThoughtDialog(stepId: Int) {
        ActionsDialog.Builder(this)
            .setTitle(getString(R.string.workshop_step_unlink_title))
            .setDescription(getString(R.string.workshop_step_unlink_description))
            .setStandardAction(getString(R.string.workshop_step_unlink_keep_thought)) {
                viewModel.unlinkThought(stepId, alsoDeleteThought = false)
            }
            .setCautionAction(getString(R.string.workshop_step_unlink_delete_thought)) {
                viewModel.unlinkThought(stepId, alsoDeleteThought = true)
            }
            .setDismissText(getString(R.string.common_btn_cancel))
            .show()
    }

    // ── Bottom sheets: steps ───────────────────────────────────────────────────

    private fun showAddStepSheet() {
        StepEditBottomSheet.show(
            fragmentManager = supportFragmentManager,
            title           = getString(R.string.workshop_dialog_add_step),
            description     = "",
            maxRepetitions  = 1
        ) { description, maxRepetitions, reminderTime, reminderDays ->
            viewModel.addStep(description, maxRepetitions, reminderTime, reminderDays)
        }
    }

    private fun showEditStepSheet(step: GoalStep) {
        StepEditBottomSheet.show(
            fragmentManager = supportFragmentManager,
            title           = getString(R.string.workshop_dialog_edit_step),
            description     = step.description,
            maxRepetitions  = step.maxRepetitions,
            reminderTime    = step.reminderTime,
            reminderDays    = step.reminderDays
        ) { description, maxRepetitions, reminderTime, reminderDays ->
            viewModel.updateStep(step.id, description, maxRepetitions, reminderTime, reminderDays)
        }
    }

    // ── Dialogs ────────────────────────────────────────────────────────────────

    private fun showEditGoalDescriptionDialog(goal: Goal) {
        TextEditDialog(
            context   = this,
            title     = getString(R.string.workshop_dialog_edit_goal),
            textInput = goal.description,
            onSave    = { desc -> viewModel.updateGoalDescription(desc) }
        ).show()
    }

    private fun showDeleteStepConfirmation(stepId: Int) {
        ActionsDialog.Builder(this)
            .setTitle(getString(R.string.workshop_dialog_delete_step_title))
            .setDescription(getString(R.string.common_deletion_dialog_warning))
            .setCautionAction(getString(R.string.common_deletion_dialog_yes)) {
                viewModel.deleteStep(stepId)
            }
            .setDismissText(getString(R.string.common_btn_cancel))
            .show()
    }
}
