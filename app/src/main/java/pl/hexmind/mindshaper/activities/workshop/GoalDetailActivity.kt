package pl.hexmind.mindshaper.activities.workshop

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
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

@AndroidEntryPoint
class GoalDetailActivity : CoreActivity() {

    companion object {
        // Intent extra key — must match SavedStateHandle key in GoalDetailViewModel
        private const val EXTRA_GOAL_ID = "goalId"

        fun newIntent(context: Context, goalId: Int): Intent =
            Intent(context, GoalDetailActivity::class.java)
                .putExtra(EXTRA_GOAL_ID, goalId)
    }

    private val viewModel: GoalDetailViewModel by viewModels()

    private lateinit var tvBadge: TextView
    private lateinit var cbGoalDesc: MaterialButton
    private lateinit var rvGuidelines: RecyclerView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var guidelinesAdapter: GoalDetailGuidelinesAdapter

    // Guideline which launched CaptureActivity
    private var pendingLinkGuidelineId: Int? = null

    // Handling receiving newly-created thoughtId in the result from CaptureActivity
    private val captureActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val thoughtId = result.data?.getLongExtra(CaptureActivity.EXTRA_THOUGHT_ID, -1L) ?: -1L
            val guidelineId = pendingLinkGuidelineId
            if (thoughtId > 0 && guidelineId != null) {
                viewModel.linkThought(guidelineId, thoughtId.toInt())
            }
        }
        pendingLinkGuidelineId = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.goal_detail_activity)
        // No header widget

        initViews()
        setupGuidelinesList()
        setupFab()
        observeGoal()
    }

    // ── Init ───────────────────────────────────────────────────────────────────

    private fun initViews() {
        tvBadge      = findViewById(R.id.tv_goal_priority_badge)
        cbGoalDesc   = findViewById(R.id.cb_goal_description)
        rvGuidelines = findViewById(R.id.rv_guidelines)
        fabAdd       = findViewById(R.id.fab_add_guideline)
    }

    private fun setupGuidelinesList() {
        guidelinesAdapter = GoalDetailGuidelinesAdapter(
            onTapText              = { gl -> showEditGuidelineSheet(gl) },
            onLongPressText        = { id -> showDeleteGuidelineConfirmation(id) },
            onTapRing              = { id -> viewModel.incrementGuideline(id) },
            onLongPressRing        = { id -> viewModel.decrementGuideline(id) },
            onMenuClick            = { anchor, gl, isFirst, isLast ->
                showGuidelineMenu(anchor, gl, isFirst, isLast)
            },
            onThoughtChipClick     = { thoughtId -> openThoughtDetails(thoughtId) },
            onThoughtChipLongPress = { id -> showUnlinkThoughtDialog(id) }
        )

        rvGuidelines.apply {
            layoutManager = LinearLayoutManager(this@GoalDetailActivity)
            adapter = guidelinesAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupFab() {
        fabAdd.setOnClickListener { showAddGuidelineSheet() }
    }

    // ── Observe ────────────────────────────────────────────────────────────────

    private fun observeGoal() {
        viewModel.goal.observe(this) { goal ->
            goal ?: return@observe
            bindHeader(goal)
            guidelinesAdapter.setItems(goal.subItems)
        }
    }

    private fun bindHeader(goal: Goal) {
        tvBadge.text = goal.importance.toString()
        // 1 = low importance (green), 2 = medium (yellow), 3 = critical (red)
        val bgRes = when (goal.importance) {
            3    -> R.color.importance_high
            2    -> R.color.importance_medium
            else -> R.color.importance_low
        }
        tvBadge.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(this, bgRes)
        )
        tvBadge.setOnClickListener { viewModel.cycleGoalImportance() }

        cbGoalDesc.text = goal.description
        cbGoalDesc.setOnClickListener { showEditGoalDescriptionDialog(goal) }
    }

    // ── Guideline menu ───────────────────────────────────────────────────────────────────────────

    private fun showGuidelineMenu(anchor: View, guideline: GoalGuideline, isFirst: Boolean, isLast: Boolean) {
        val popup = PopupMenu(this, anchor)
        popup.menuInflater.inflate(R.menu.menu_goal_detail_guideline, popup.menu) // TODO: replace with more cool/fancy UI menu

        // Disable move up/down at list edges
        popup.menu.findItem(R.id.action_move_up).isEnabled = !isFirst
        popup.menu.findItem(R.id.action_move_down).isEnabled = !isLast

        // Label of "link thought" depends on whether one is already linked
        val linkItem = popup.menu.findItem(R.id.action_link_thought)
        linkItem.title = getString(
            if (guideline.hasLinkedThought) R.string.workshop_guideline_menu_change_thought
            else R.string.workshop_guideline_menu_link_thought
        )

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_move_up   -> { viewModel.moveGuidelineUp(guideline.id); true }
                R.id.action_move_down -> { viewModel.moveGuidelineDown(guideline.id); true }
                R.id.action_link_thought -> {
                    launchCaptureForGuideline(guideline)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun launchCaptureForGuideline(guideline: GoalGuideline) {
        pendingLinkGuidelineId = guideline.id
        // If guideline already has a linked thought, the old link is replaced after CaptureActivity returns
        captureActivityLauncher.launch(Intent(this, CaptureActivity::class.java))
    }

    // ── Linked thought ───────────────────────────────────────────────────────────────────────────

    private fun openThoughtDetails(thoughtId: Int) {
        val intent = Intent(this, DetailsActivity::class.java)
            .putExtra(DetailsActivity.P_SELECTED_THOUGHT_ID, thoughtId)
        startActivity(intent)
    }

    private fun showUnlinkThoughtDialog(guidelineId: Int) {
        ActionsDialog.Builder(this)
            .setTitle(getString(R.string.workshop_guideline_unlink_title))
            .setDescription(getString(R.string.workshop_guideline_unlink_description))
            .setStandardAction(getString(R.string.workshop_guideline_unlink_keep_thought)) {
                viewModel.unlinkThought(guidelineId, alsoDeleteThought = false)
            }
            .setCautionAction(getString(R.string.workshop_guideline_unlink_delete_thought)) {
                viewModel.unlinkThought(guidelineId, alsoDeleteThought = true)
            }
            .setDismissText(getString(R.string.common_btn_cancel))
            .show()
    }

    // ── Bottom sheets: guidelines ──────────────────────────────────────────────

    private fun showAddGuidelineSheet() {
        GuidelineEditBottomSheet.show(
            fragmentManager = supportFragmentManager,
            title           = getString(R.string.workshop_dialog_add_guideline),
            description     = "",
            maxRepetitions  = 1
        ) { desc, maxReps -> viewModel.addGuideline(desc, maxReps) }
    }

    private fun showEditGuidelineSheet(guideline: GoalGuideline) {
        GuidelineEditBottomSheet.show(
            fragmentManager = supportFragmentManager,
            title           = getString(R.string.workshop_dialog_edit_guideline),
            description     = guideline.description,
            maxRepetitions  = guideline.maxRepetitions
        ) { desc, maxReps -> viewModel.updateGuideline(guideline.id, desc, maxReps) }
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

    private fun showDeleteGuidelineConfirmation(guidelineId: Int) {
        ActionsDialog.Builder(this)
            .setTitle(getString(R.string.workshop_dialog_delete_guideline_title))
            .setDescription(getString(R.string.common_deletion_dialog_warning))
            .setCautionAction(getString(R.string.common_deletion_dialog_yes)) {
                viewModel.deleteGuideline(guidelineId)
            }
            .setDismissText(getString(R.string.common_btn_cancel))
            .show()
    }
}
