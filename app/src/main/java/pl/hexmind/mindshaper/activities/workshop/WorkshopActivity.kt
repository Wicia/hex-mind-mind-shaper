package pl.hexmind.mindshaper.activities.workshop

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.activities.CoreActivity
import pl.hexmind.mindshaper.activities.capture.CaptureActivity
import pl.hexmind.mindshaper.common.formatting.colorStateList
import pl.hexmind.mindshaper.common.ui.dialogs.MultipleActionsDialog
import pl.hexmind.mindshaper.common.ui.dialogs.TextEditDialog

/**
 * TODO: Room integration — replace mock data in WorkshopViewModel.
 */
@AndroidEntryPoint
class WorkshopActivity : CoreActivity() {

    private val viewModel: WorkshopViewModel by viewModels()

    private lateinit var rvGoals: RecyclerView
    private lateinit var btnToggleEdit: ImageView
    private lateinit var btnAddGoal: ImageView
    private lateinit var fabCapture: FloatingActionButton
    private lateinit var goalsAdapter: GoalsAdapter

    private var isEditMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workshop)

        initializeViews()
        setupGoalsList()
        setupEditModeToggle()
        setupFab()
        observeGoals()
    }

    // ── Init ───────────────────────────────────────────────────────────────────

    private fun initializeViews() {
        setupHeader(R.drawable.ic_activity_workshop, R.string.workshop_title)
        rvGoals = findViewById(R.id.rv_goals)
        btnToggleEdit = findViewById(R.id.btn_toggle_edit_mode)
        btnAddGoal = findViewById(R.id.btn_add_goal)
        fabCapture = findViewById(R.id.fab_capture)
    }

    private fun setupGoalsList() {
        goalsAdapter = GoalsAdapter(
            onToggleExpand = { goalId ->
                viewModel.toggleGoalExpanded(goalId)
            },
            onCyclePriority = { goalId ->
                viewModel.cycleGoalPriority(goalId)
            },
            onGoalEditTap = { goal ->
                showEditGoalDialog(goal)
            },
            onGoalDeleteTap = { goalId ->
                showDeleteGoalConfirmation(goalId)
            },
            onToggleSubItemDone = { goalId, subItemId ->
                viewModel.toggleSubItemDone(goalId, subItemId)
            },
            onSubItemEditTap = { goalId, subItem ->
                showEditSubItemDialog(goalId, subItem)
            },
            onSubItemDeleteTap = { goalId, subItemId ->
                showDeleteSubItemConfirmation(goalId, subItemId)
            },
            onSubItemReorder = { goalId, from, to ->
                viewModel.reorderSubItems(goalId, from, to)
            },
            onAddSubItemTap = { goalId ->
                showAddSubItemDialog(goalId)
            }
        )

        rvGoals.apply {
            layoutManager = LinearLayoutManager(this@WorkshopActivity)
            adapter = goalsAdapter
            isNestedScrollingEnabled = false
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
            goalsAdapter.submitList(goals)
        }
    }

    // ── Edit mode ──────────────────────────────────────────────────────────────

    private fun setupEditModeToggle() {
        btnToggleEdit.setOnClickListener {
            if (isEditMode) exitEditMode() else enterEditMode()
        }
    }

    private fun enterEditMode() {
        isEditMode = true
        goalsAdapter.isEditMode = true

        // Show "+" add goal button
        btnAddGoal.visibility = View.VISIBLE
        btnAddGoal.setOnClickListener { showAddGoalDialog() }

        // Switch icon: pencil → tick (white checkmark on orange background)
        btnToggleEdit.setImageResource(R.drawable.ic_action_approve)
        btnToggleEdit.backgroundTintList = resources.colorStateList(R.color._orange_lvl_3, theme)
    }

    private fun exitEditMode() {
        isEditMode = false
        goalsAdapter.isEditMode = false

        // Hide "+" button
        btnAddGoal.visibility = View.GONE
        btnAddGoal.setOnClickListener(null)

        // Restore pencil icon
        btnToggleEdit.setImageResource(R.drawable.ic_action_edit)
        btnToggleEdit.backgroundTintList = resources.colorStateList(R.color._orange_lvl_1, theme)

        // Sort goals: priority ASC, then lastModifiedAt DESC
        viewModel.sortGoals()
    }

    // ── Dialogs: goals ─────────────────────────────────────────────────────────

    private fun showAddGoalDialog() {
        TextEditDialog(
            context = this,
            title = getString(R.string.workshop_dialog_add_goal),
            textInput = "",
            onSave = { description -> viewModel.addGoal(description) }
        ).show()
    }

    private fun showEditGoalDialog(goal: Goal) {
        TextEditDialog(
            context = this,
            title = getString(R.string.workshop_dialog_edit_goal),
            textInput = goal.description,
            onSave = { description -> viewModel.updateGoalDescription(goal.id, description) }
        ).show()
    }

    private fun showDeleteGoalConfirmation(goalId: Int) {
        MultipleActionsDialog.Builder(this)
            .setTitle(getString(R.string.workshop_dialog_delete_goal_title))
            .setDescription(getString(R.string.workshop_dialog_delete_goal_desc))
            .setCautionAction(getString(R.string.common_btn_delete)) {
                viewModel.deleteGoal(goalId)
            }
            .setCancelText(getString(R.string.common_btn_cancel))
            .show()
    }

    // ── Dialogs: sub-items ─────────────────────────────────────────────────────

    private fun showAddSubItemDialog(goalId: Int) {
        TextEditDialog(
            context = this,
            title = getString(R.string.workshop_dialog_add_step),
            textInput = "",
            onSave = { description -> viewModel.addSubItem(goalId, description) }
        ).show()
    }

    private fun showEditSubItemDialog(goalId: Int, subItem: GoalGuideline) {
        TextEditDialog(
            context = this,
            title = getString(R.string.workshop_dialog_edit_step),
            textInput = subItem.description,
            onSave = { description ->
                viewModel.updateSubItemDescription(goalId, subItem.id, description)
            }
        ).show()
    }

    private fun showDeleteSubItemConfirmation(goalId: Int, subItemId: Int) {
        MultipleActionsDialog.Builder(this)
            .setTitle(getString(R.string.workshop_dialog_delete_step_title))
            .setDescription(getString(R.string.workshop_dialog_delete_step_desc))
            .setCautionAction(getString(R.string.common_btn_delete)) {
                viewModel.deleteSubItem(goalId, subItemId)
            }
            .setCancelText(getString(R.string.common_btn_cancel))
            .show()
    }
}