package pl.hexmind.mindshaper.activities.workshop

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.common.ui.views.HexInputField
import pl.hexmind.mindshaper.common.ui.views.HexSwitchRow

/**
 * Bottom sheet for adding or editing a step.
 *
 * Usage:
 *   StepEditBottomSheet.show(
 *       fragmentManager  = supportFragmentManager,
 *       title            = getString(R.string.workshop_dialog_add_step),
 *       description      = "",
 *       maxRepetitions   = 1,
 *       reminderTime     = null,
 *       reminderDays     = null
 *   ) { desc, maxReps, time, days -> viewModel.addStep(desc, maxReps, time, days) }
 */
class StepEditBottomSheet : BottomSheetDialogFragment() {

    private var onConfirm: ((
        description: String,
        maxRepetitions: Int,
        reminderTime: String?,
        reminderDays: String?
    ) -> Unit)? = null

    private lateinit var tvTitle: TextView
    private lateinit var hifDescription: HexInputField
    private lateinit var fabConfirm: FloatingActionButton
    private lateinit var rootView: ViewGroup

    // Repetitions
    private lateinit var switchRepetitions: HexSwitchRow
    private lateinit var repetitionsViews: List<View>
    private lateinit var chipButtons: List<Pair<MaterialButton, Int>>

    private lateinit var hifRepetitions: HexInputField

    private lateinit var switchReminderEnabled: HexSwitchRow
    private lateinit var reminderView: GoalReminderView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.step_edit_bottom_sheet, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (dialog as? BottomSheetDialog)?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED

        bindViews(view)
        setupTouchOutsideToClearFocus()
        setupInitialValues()
        setupChips()
        setupRepetitionsField()
        setupRepetitionsSwitch()
        setupReminderToggle()
        setupConfirm()
        focusDescriptionForNewStep()
    }

    // A new step opens straight into typing the description; editing opens without the keyboard.
    // The sheet resizes above the keyboard, so the switches below the field stay visible
    private fun focusDescriptionForNewStep() {
        // Edited steps always have a description (it is required on confirm)
        if (!arguments?.getString(ARG_DESCRIPTION).isNullOrEmpty()) return
        dialog?.window?.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE or
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )
        hifDescription.requestFocusOnField()
    }

    private fun bindViews(view: View) {
        rootView        = view as ViewGroup
        tvTitle         = view.findViewById(R.id.tv_step_sheet_title)
        hifDescription  = view.findViewById(R.id.hif_step_description)
        hifRepetitions  = view.findViewById(R.id.hif_repetitions)
        fabConfirm      = view.findViewById(R.id.fab_step_confirm)

        switchReminderEnabled = view.findViewById(R.id.switch_reminder_enabled)
        reminderView      = view.findViewById(R.id.reminder_view)

        switchRepetitions = view.findViewById(R.id.switch_repetitions)
        repetitionsViews = listOf(
            view.findViewById(R.id.tv_repetitions_header),
            view.findViewById(R.id.ll_repetitions)
        )

        chipButtons = listOf(
            view.findViewById<MaterialButton>(R.id.chip_3x)  to 3,
            view.findViewById<MaterialButton>(R.id.chip_5x)  to 5,
            view.findViewById<MaterialButton>(R.id.chip_7x)  to 7,
            view.findViewById<MaterialButton>(R.id.chip_14x) to 14,
            view.findViewById<MaterialButton>(R.id.chip_21x) to 21,
            view.findViewById<MaterialButton>(R.id.chip_31x) to 31
        )
    }

    // Tap anywhere outside a focused field -> clear focus + hide keyboard
    private fun setupTouchOutsideToClearFocus() {
        rootView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                clearFocusAndHideKeyboard()
            }
            false
        }
    }

    private fun setupInitialValues() {
        arguments?.getString(ARG_TITLE)?.let { tvTitle.text = it }

        hifDescription.setText(arguments?.getString(ARG_DESCRIPTION) ?: "")

        val maxRepetitions = arguments?.getInt(ARG_MAX_REPETITIONS, 1) ?: 1
        hifRepetitions.setText(maxRepetitions.toString())
        updateReminderLabel(maxRepetitions)

        // Hide config details when feature is off
        val calendarEnabled = arguments?.getBoolean(ARG_CALENDAR_ENABLED, false) ?: false
        if (!calendarEnabled) {
            switchReminderEnabled.visibility = View.GONE
            reminderView.visibility = View.GONE
            return
        }

        // Restore reminder state when editing an existing step
        val reminderTime = arguments?.getString(ARG_REMINDER_TIME)
        val reminderDays = arguments?.getString(ARG_REMINDER_DAYS)
        if (reminderTime != null || reminderDays != null) {
            switchReminderEnabled.isChecked = true
            reminderView.visibility = View.VISIBLE
            reminderView.setReminder(reminderTime, reminderDays)
        }
    }

    private fun setupChips() {
        chipButtons.forEach { (chip, value) ->
            chip.setOnClickListener {
                hifRepetitions.setText(value.toString())
                clearFocusAndHideKeyboard()
                syncChipSelection(value)
            }
        }
        syncChipSelection(hifRepetitions.getText().toIntOrNull() ?: 1)
    }

    private fun setupRepetitionsField() {
        hifRepetitions.addTextChangedListener { text ->
            val repetitions = text.trim().toIntOrNull()
            syncChipSelection(repetitions)
            updateReminderLabel(repetitions)
        }
    }

    // A step runs once by default; repeating is opt-in, so its config stays hidden until switched on
    private fun setupRepetitionsSwitch() {
        switchRepetitions.isChecked = (hifRepetitions.getText().toIntOrNull() ?: 1) > 1
        updateRepetitionsVisibility(switchRepetitions.isChecked)

        switchRepetitions.setOnCheckedChangeListener { _, isChecked ->
            updateRepetitionsVisibility(isChecked)
            if (isChecked && (hifRepetitions.getText().toIntOrNull() ?: 1) <= 1) {
                hifRepetitions.setText(DEFAULT_REPEATED_COUNT.toString())
            }
            updateReminderLabel(selectedRepetitions())
        }
    }

    private fun updateRepetitionsVisibility(isVisible: Boolean) {
        repetitionsViews.forEach { view ->
            view.visibility = if (isVisible) View.VISIBLE else View.GONE
        }
    }

    // Null when the switch is on and the field holds no number
    private fun selectedRepetitions(): Int? =
        if (switchRepetitions.isChecked) hifRepetitions.getText().toIntOrNull() else 1

    // Reflect the entered repetitions count in the reminder switch label
    private fun updateReminderLabel(repetitions: Int?) {
        val count = repetitions ?: 0
        switchReminderEnabled.text =
            resources.getQuantityString(R.plurals.workshop_reminder_switch_label, count, count)
    }

    // Highlight chip matching [value]; deselect all if null or no match
    private fun syncChipSelection(value: Int?) {
        chipButtons.forEach { (chip, chipValue) ->
            chip.isSelected = value != null && chipValue == value
        }
    }

    private fun setupReminderToggle() {
        switchReminderEnabled.setOnCheckedChangeListener { _, isChecked ->
            reminderView.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
    }

    private fun setupConfirm() {
        fabConfirm.setOnClickListener {
            val description = hifDescription.getText()
            if (description.isEmpty()) {
                hifDescription.showError(getString(R.string.workshop_step_description_error_no_input))
                return@setOnClickListener
            }
            val maxRepetitions = selectedRepetitions()

            // TODO Move it to validation in view model + callback here (Nice to have)
            if (maxRepetitions == null) {
                hifRepetitions.showError(getString(R.string.workshop_step_repetitions_error_no_input))
                return@setOnClickListener
            }
            else if (maxRepetitions < 1){
                hifRepetitions.showError(getString(R.string.workshop_step_repetitions_error_below_min))
                return@setOnClickListener
            }
            else if (maxRepetitions > 31){
                hifRepetitions.showError(getString(R.string.workshop_step_repetitions_error_above_max))
                return@setOnClickListener
            }

            // Reminder is opt-in; when enabled at least one day must be selected
            var reminderTime: String? = null
            var reminderDays: String? = null
            if (switchReminderEnabled.isChecked) {
                if (!reminderView.hasSelectedDays()) {
                    clearFocusAndHideKeyboard()
                    android.widget.Toast.makeText(
                        requireContext(),
                        getString(R.string.workshop_reminder_error_no_days),
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }
                reminderTime = reminderView.getSelectedTime()
                reminderDays = reminderView.getSelectedDaysCsv()
            }

            dismiss()
            onConfirm?.invoke(description, maxRepetitions.coerceAtMost(365), reminderTime, reminderDays)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        onConfirm = null
    }

    private fun clearFocusAndHideKeyboard() {
        rootView.clearFocus()
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(rootView.windowToken, 0)
    }

    companion object {
        private const val TAG                 = "StepEditBottomSheet"
        private const val ARG_TITLE           = "arg_title"
        private const val ARG_DESCRIPTION     = "arg_description"
        private const val ARG_MAX_REPETITIONS = "arg_max_repetitions"
        private const val ARG_REMINDER_TIME   = "arg_reminder_time"
        private const val ARG_REMINDER_DAYS   = "arg_reminder_days"
        private const val ARG_CALENDAR_ENABLED = "arg_calendar_enabled"

        private const val DEFAULT_REPEATED_COUNT = 3

        fun show(
            fragmentManager: FragmentManager,
            title: String,
            description: String = "",
            maxRepetitions: Int = 1,
            reminderTime: String? = null,
            reminderDays: String? = null,
            calendarRemindersEnabled: Boolean = false,
            onConfirm: (
                description: String,
                maxRepetitions: Int,
                reminderTime: String?,
                reminderDays: String?
            ) -> Unit
        ) {
            StepEditBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, title)
                    putString(ARG_DESCRIPTION, description)
                    putInt(ARG_MAX_REPETITIONS, maxRepetitions)
                    putString(ARG_REMINDER_TIME, reminderTime)
                    putString(ARG_REMINDER_DAYS, reminderDays)
                    putBoolean(ARG_CALENDAR_ENABLED, calendarRemindersEnabled)
                }
                this.onConfirm = onConfirm
            }.show(fragmentManager, TAG)
        }
    }
}
