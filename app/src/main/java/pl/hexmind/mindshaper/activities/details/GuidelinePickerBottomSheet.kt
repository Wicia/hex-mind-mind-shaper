package pl.hexmind.mindshaper.activities.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.activities.details.GuidelinePickerBottomSheet.Companion.show
import pl.hexmind.mindshaper.services.GoalsService
import pl.hexmind.mindshaper.services.dto.GoalDTO
import javax.inject.Inject

/**
 * Bottom sheet that lets the user pick a guideline to link the current thought to.
 *
 * Result is delivered via [onGuidelinePicked] callback, set through the [show] factory.
 */
@AndroidEntryPoint
class GuidelinePickerBottomSheet : BottomSheetDialogFragment() {

    @Inject lateinit var goalsService: GoalsService

    private var onGuidelinePicked: ((guidelineId: Int) -> Unit)? = null

    private lateinit var llGoalsContainer: LinearLayout
    private lateinit var tvEmptyState: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.bottom_sheet_guideline_picker, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (dialog as? BottomSheetDialog)?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED

        llGoalsContainer = view.findViewById(R.id.ll_goals_container)
        tvEmptyState     = view.findViewById(R.id.tv_empty_state)

        loadGoals()
    }

    private fun loadGoals() {
        lifecycleScope.launch {
            val goals = goalsService.getAvailableGuidelinesForLink()
            renderGoals(goals)
        }
    }

    private fun renderGoals(goals: List<GoalDTO>) {
        llGoalsContainer.removeAllViews()

        if (goals.isEmpty()) {
            tvEmptyState.visibility = View.VISIBLE
            return
        }
        tvEmptyState.visibility = View.GONE

        val inflater = LayoutInflater.from(requireContext())
        goals.forEach { goal ->
            val block = inflater.inflate(
                R.layout.guideline_picker_goal_item, llGoalsContainer, false
            )
            bindGoalBlock(block, goal, inflater)
            llGoalsContainer.addView(block)
        }
    }

    private fun bindGoalBlock(block: View, goal: GoalDTO, inflater: LayoutInflater) {
        val tvGoalBadge: TextView      = block.findViewById(R.id.tv_goal_badge)
        val tvGoalName: TextView       = block.findViewById(R.id.tv_goal_name)
        val ivArrow                    = block.findViewById<View>(R.id.iv_goal_arrow)
        val llGuidelinesInner: LinearLayout = block.findViewById(R.id.ll_guidelines_inner)
        val goalRow                    = block.findViewById<View>(R.id.ll_goal_row)

        tvGoalBadge.text = goal.importance.toString()
        // 1 = low (green) / 2 = medium (yellow) / 3 = high (red) — matches workshop badge palette
        val badgeBgRes = when (goal.importance) {
            3    -> R.color.importance_high
            2    -> R.color.importance_medium
            else -> R.color.importance_low
        }
        tvGoalBadge.setBackgroundResource(R.drawable.shape_workshop_priority_badge)
        tvGoalBadge.backgroundTintList = androidx.core.content.ContextCompat.getColorStateList(
            requireContext(), badgeBgRes
        )

        tvGoalName.text = goal.description

        // Build guideline rows once; toggle visibility on goal tap
        goal.guidelines.forEach { guideline ->
            val glRow = inflater.inflate(
                R.layout.guideline_picker_guideline_item, llGuidelinesInner, false
            )
            glRow.findViewById<TextView>(R.id.tv_guideline_description).text = guideline.description
            glRow.setOnClickListener {
                onGuidelinePicked?.invoke(guideline.id)
                dismiss()
            }
            llGuidelinesInner.addView(glRow)
        }

        // Single-expand pattern: tapping a goal collapses any other expanded one
        goalRow.setOnClickListener {
            val willExpand = llGuidelinesInner.visibility != View.VISIBLE
            collapseAllGoals()
            if (willExpand) {
                llGuidelinesInner.visibility = View.VISIBLE
                ivArrow.rotation = 90f
            }
        }
    }

    private fun collapseAllGoals() {
        for (i in 0 until llGoalsContainer.childCount) {
            val block = llGoalsContainer.getChildAt(i)
            block.findViewById<LinearLayout>(R.id.ll_guidelines_inner).visibility = View.GONE
            block.findViewById<View>(R.id.iv_goal_arrow).rotation = 0f
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        onGuidelinePicked = null
    }

    companion object {
        private const val TAG = "GuidelinePickerBottomSheet"

        fun show(
            fragmentManager: FragmentManager,
            onGuidelinePicked: (guidelineId: Int) -> Unit
        ) {
            GuidelinePickerBottomSheet().apply {
                this.onGuidelinePicked = onGuidelinePicked
            }.show(fragmentManager, TAG)
        }
    }
}
