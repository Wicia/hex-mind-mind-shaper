package pl.hexmind.mindshaper.activities.workshop

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.isEmpty
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.activities.CoreActivity
import pl.hexmind.mindshaper.activities.capture.CaptureActivity
import pl.hexmind.mindshaper.common.ui.dialogs.ActionsDialog
import pl.hexmind.mindshaper.common.ui.dialogs.TextEditDialog
import pl.hexmind.mindshaper.database.models.PathEntity

@AndroidEntryPoint
class WorkshopActivity : CoreActivity() {

    private val viewModel: WorkshopViewModel by viewModels()

    // GOALS
    private lateinit var rvGoals: RecyclerView
    private lateinit var btnAddGoal: MaterialButton
    private lateinit var fabCapture: FloatingActionButton
    private lateinit var goalsAdapter: GoalsAdapter

    // PATHS
    private lateinit var llPathsList: LinearLayout
    private lateinit var tvPoolEmpty: TextView
    private lateinit var btnPathsToggle: MaterialButton
    private lateinit var cardAnimator: PathCardAnimator

    private var isPathsExpanded: Boolean = true

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
        // Reload goals after returning from GoalDetailActivity (changeable guidelines may change :)
        viewModel.reload()
    }

    // ── Init ───────────────────────────────────────────────────────────────────

    private fun initializeViews() {
        setupHeader(R.drawable.ic_activity_workshop, R.string.workshop_title)

        rvGoals    = findViewById(R.id.rv_goals)
        btnAddGoal = findViewById(R.id.btn_add_goal)
        fabCapture = findViewById(R.id.fab_capture)

        btnAddGoal.visibility = View.VISIBLE
        btnAddGoal.setOnClickListener { showAddGoalDialog() }

        llPathsList   = findViewById(R.id.ll_paths_list)
        tvPoolEmpty   = findViewById(R.id.tv_paths_pool_empty)
        btnPathsToggle = findViewById(R.id.btn_paths_toggle)
        btnPathsToggle.setOnClickListener { togglePathsSection() }

        cardAnimator = PathCardAnimator(viewModel)
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
            val path = paths.getOrNull(slot)

            val view: View = if (path != null) {
                LayoutInflater.from(this)
                    .inflate(R.layout.path_item, llPathsList, false)
                    .also { bindPathCard(it, path) }
            }
            else {
                View(this) // empty placeholder to preserve symmetry
            }

            val params = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            if (slot > 0) params.marginStart = 8
            view.layoutParams = params
            llPathsList.addView(view)

            if (path != null) cardAnimator.onCardBuilt(view, path.pathKey)
        }
    }

    private fun bindPathCard(cardView: View, path: PathItem) {
        val card = cardView.findViewById<View>(R.id.ll_path_card)
        val btnPathProceed = cardView.findViewById<MaterialButton>(R.id.btn_path_proceed_action)
        val tvContent = cardView.findViewById<TextView>(R.id.tv_path_content)
        val tvStepsCount = cardView.findViewById<TextView>(R.id.tv_path_steps_count)
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
                R.plurals.path_steps_count, path.totalSteps, path.totalSteps
            )
        }
        else {
            tvContent.text = path.currentStepContent
            tvStepsCount.visibility = View.GONE
        }
    }

    private fun togglePathsSection() {
        isPathsExpanded = !isPathsExpanded
        llPathsList.visibility = if (isPathsExpanded) View.VISIBLE else View.GONE
        tvPoolEmpty.visibility = if (isPathsExpanded && llPathsList.isEmpty()) View.VISIBLE else View.GONE
        btnPathsToggle.setIconResource(
            if (isPathsExpanded) R.drawable.ic_section_collapse else R.drawable.ic_section_expand
        )
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
            .setDismissText(getString(R.string.common_btn_cancel))
            .show()
    }
}