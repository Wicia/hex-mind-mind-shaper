package pl.hexmind.mindshaper.activities.workshop

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.activities.CoreActivity
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
    private lateinit var itemTouchHelper: ItemTouchHelper

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
            onTapText       = { gl -> showEditGuidelineSheet(gl) },
            onLongPressText = { id -> showDeleteGuidelineConfirmation(id) },
            onTapRing       = { id -> viewModel.incrementGuideline(id) },
            onLongPressRing = { id -> viewModel.decrementGuideline(id) },
            onStartDrag     = { holder -> itemTouchHelper.startDrag(holder) }
        )

        val dragCallback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                guidelinesAdapter.moveItem(viewHolder.adapterPosition, target.adapterPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // Swipe disabled — no-op
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                viewModel.persistReorder(guidelinesAdapter.getOrderedIds())
            }
        }

        itemTouchHelper = ItemTouchHelper(dragCallback)
        itemTouchHelper.attachToRecyclerView(rvGuidelines)

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
