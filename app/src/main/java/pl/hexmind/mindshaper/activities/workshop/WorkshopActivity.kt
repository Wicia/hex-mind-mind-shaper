package pl.hexmind.mindshaper.activities.workshop

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.isNotEmpty
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.activities.CoreActivity
import pl.hexmind.mindshaper.activities.capture.CaptureActivity
import pl.hexmind.mindshaper.common.ui.dialogs.MultipleActionsDialog
import pl.hexmind.mindshaper.common.ui.dialogs.TextEditDialog
import pl.hexmind.mindshaper.database.models.PathEntity
import androidx.core.view.isEmpty

@AndroidEntryPoint
class WorkshopActivity : CoreActivity() {

    private val viewModel: WorkshopViewModel by viewModels()

    // GOALS
    private lateinit var rvGoals: RecyclerView
    private lateinit var btnToggleEdit: MaterialButton
    private lateinit var btnAddGoal: MaterialButton
    private lateinit var fabCapture: FloatingActionButton
    private lateinit var goalsAdapter: GoalsAdapter

    // PATHS
    private lateinit var llPathsList: LinearLayout
    private lateinit var tvPoolEmpty: TextView

    private lateinit var btnPathsToggle: MaterialButton
    private var isPathsExpanded: Boolean = true

    private var isEditMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workshop)

        initializeViews()
        setupGoalsList()
        setupEditModeToggle()
        setupFab()
        observeGoals()
        observePaths()
    }

    // ── Init ───────────────────────────────────────────────────────────────────

    private fun initializeViews() {
        setupHeader(R.drawable.ic_activity_workshop, R.string.workshop_title)

        rvGoals = findViewById(R.id.rv_goals)
        btnToggleEdit = findViewById(R.id.btn_toggle_edit_mode)
        btnAddGoal = findViewById(R.id.btn_add_goal)
        fabCapture = findViewById(R.id.fab_capture)

        llPathsList = findViewById(R.id.ll_paths_list)
        tvPoolEmpty = findViewById(R.id.tv_paths_pool_empty)

        btnPathsToggle = findViewById(R.id.btn_paths_toggle)
        btnPathsToggle.setOnClickListener { togglePathsSection()}
    }

    private fun setupGoalsList() {
        goalsAdapter = GoalsAdapter(
            onToggleExpand = { goalId -> viewModel.toggleGoalExpanded(goalId) },
            onCyclePriority = { goalId -> viewModel.cycleGoalPriority(goalId) },
            onGoalEditTap = { goal -> showEditGoalDialog(goal) },
            onGoalDeleteTap = { goalId -> showDeleteGoalConfirmation(goalId) },
            onToggleSubItemDone = { goalId, subItemId ->
                viewModel.toggleSubItemDone(
                    goalId,
                    subItemId
                )
            },
            onSubItemEditTap = { goalId, subItem -> showEditSubItemDialog(goalId, subItem) },
            onSubItemDeleteTap = { goalId, subItemId ->
                showDeleteSubItemConfirmation(
                    goalId,
                    subItemId
                )
            },
            onSubItemReorder = { goalId, from, to -> viewModel.reorderSubItems(goalId, from, to) },
            onAddSubItemTap = { goalId -> showAddSubItemDialog(goalId) }
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
        viewModel.goals.observe(this) { goals -> goalsAdapter.submitList(goals) }
    }

    // ── Paths ──────────────────────────────────────────────────────────────────

    private fun observePaths() {
        viewModel.pickedPaths.observe(this) { paths -> rebuildPathCards(paths) }
    }

    /**
     * Clears paths list and re-inflates one item_path card per active path
     */
    private fun rebuildPathCards(paths: List<PathItem>) {
        llPathsList.removeAllViews()

        if (paths.isEmpty()) {
            tvPoolEmpty.visibility = View.VISIBLE
            return
        }
        tvPoolEmpty.visibility = View.GONE

        for (slot in 0 until 2) {
            val path = paths.getOrNull(slot)

            val view: View = if (path != null) {
                LayoutInflater.from(this)
                    .inflate(R.layout.item_path, llPathsList, false)
                    .also { bindPathCard(it, path) }
            } else {
                // pusty placeholder zachowuje symetrię
                View(this)
            }

            val params = LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
            )
            if (slot > 0) params.marginStart = 8
            view.layoutParams = params
            llPathsList.addView(view)
        }
    }

    /**
     * Binds a single path item view to a PathItem.
     */
    private fun bindPathCard(cardView: View, path: PathItem) {
        val card = cardView.findViewById<View>(R.id.ll_path_card)
        val btnPathProceed = cardView.findViewById<MaterialButton>(R.id.btn_path_proceed_action)
        val tvContent = cardView.findViewById<TextView>(R.id.tv_path_content)
        val tvStepsCount = cardView.findViewById<TextView>(R.id.tv_path_steps_count)
        val btnRepick = cardView.findViewById<MaterialButton>(R.id.btn_path_redraw)

        val isUnselected = path.status == PathEntity.STATUS_UNSELECTED

        // Card background
        val bgColor = if (isUnselected)
            ContextCompat.getColor(this, R.color._gray_lvl_1)
        else
            ContextCompat.getColor(this, R.color._orange_lvl_1)
        (card.background?.mutate() as? GradientDrawable)?.setColor(bgColor)
            ?: card.setBackgroundColor(bgColor)

        // Footer icons (centered)
        when {
            isUnselected -> {
                btnPathProceed.setIconResource(R.drawable.ic_path_reveal)
                btnPathProceed.setOnClickListener { viewModel.revealPath(path.pathKey) }
            }
            path.isLastStep -> {
                btnPathProceed.setIconResource(R.drawable.ic_path_complete)
                btnPathProceed.setOnClickListener { viewModel.advanceToNextStep(path.pathKey) }
            }
            else -> {
                btnPathProceed.setIconResource(R.drawable.ic_path_step_complete)
                btnPathProceed.setOnClickListener { viewModel.advanceToNextStep(path.pathKey) }
            }
        }
        btnRepick.setOnClickListener { viewModel.repickPath(path.pathKey) }

        if (isUnselected) {
            // MODE 1: category name + step count
            tvContent.text = path.category
            tvStepsCount.visibility = View.VISIBLE
            tvStepsCount.text = resources.getQuantityString(
                R.plurals.path_steps_count, path.totalSteps, path.totalSteps
            )
        }
        else {
            // MODE 2/3: step content only — category hidden
            tvContent.text = path.currentStepContent
            tvStepsCount.visibility = View.GONE
        }
    }

    private fun togglePathsSection() {
        isPathsExpanded = !isPathsExpanded
        val contentVisibility = if (isPathsExpanded) View.VISIBLE else View.GONE
        llPathsList.visibility = contentVisibility
        tvPoolEmpty.visibility = if (isPathsExpanded && llPathsList.isEmpty()) View.VISIBLE else View.GONE
        btnPathsToggle.setIconResource(
            if (isPathsExpanded) R.drawable.ic_section_collapse else R.drawable.ic_section_expand
        )
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
        btnAddGoal.visibility = View.VISIBLE
        btnAddGoal.setOnClickListener { showAddGoalDialog() }
        btnToggleEdit.setIconResource(R.drawable.ic_action_approve)
    }

    private fun exitEditMode() {
        isEditMode = false
        goalsAdapter.isEditMode = false
        btnAddGoal.visibility = View.GONE
        btnAddGoal.setOnClickListener(null)
        btnToggleEdit.setIconResource(R.drawable.ic_action_edit)
        viewModel.sortGoals()
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

    private fun showEditGoalDialog(goal: Goal) {
        TextEditDialog(
            context = this,
            title = getString(R.string.workshop_dialog_edit_goal),
            notesStyle = false,
            textInput = goal.description,
            onSave = { description -> viewModel.updateGoalDescription(goal.id, description) }
        ).show()
    }

    private fun showDeleteGoalConfirmation(goalId: Int) {
        MultipleActionsDialog.Builder(this)
            .setTitle(getString(R.string.workshop_dialog_delete_goal_title))
            .setDescription(getString(R.string.workshop_dialog_delete_goal_desc))
            .setCautionAction(getString(R.string.common_deletion_dialog_yes)) {
                viewModel.deleteGoal(goalId)
            }
            .setCancelText(getString(R.string.common_btn_cancel))
            .show()
    }

    // ── Dialogs: sub-items ─────────────────────────────────────────────────────

    private fun showAddSubItemDialog(goalId: Int) {
        TextEditDialog(
            context = this,
            title = getString(R.string.workshop_dialog_add_guideline),
            notesStyle = false,
            textInput = "",
            onSave = { description -> viewModel.addSubItem(goalId, description) }
        ).show()
    }

    private fun showEditSubItemDialog(goalId: Int, subItem: GoalGuideline) {
        TextEditDialog(
            context = this,
            title = getString(R.string.workshop_dialog_edit_guideline),
            notesStyle = false,
            textInput = subItem.description,
            onSave = { description ->
                viewModel.updateSubItemDescription(goalId, subItem.id, description)
            }
        ).show()
    }

    private fun showDeleteSubItemConfirmation(goalId: Int, subItemId: Int) {
        MultipleActionsDialog.Builder(this)
            .setTitle(getString(R.string.workshop_dialog_delete_guideline_title))
            .setDescription(getString(R.string.common_deletion_dialog_warning))
            .setCautionAction(getString(R.string.common_deletion_dialog_yes)) {
                viewModel.deleteSubItem(goalId, subItemId)
            }
            .setCancelText(getString(R.string.common_btn_cancel))
            .show()
    }
}