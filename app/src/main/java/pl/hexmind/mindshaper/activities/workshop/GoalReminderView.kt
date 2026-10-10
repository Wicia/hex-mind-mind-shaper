package pl.hexmind.mindshaper.activities.workshop

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.google.android.material.button.MaterialButton
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.common.ui.views.HexHourPickerView

/**
 * Reminder config block:
 * 1. time-of-day picker - center slot is selected, horizontal scrollable
 * 2. set of weekday toggles
 *
 * Self-contained — exposes selected time and days via public getters.
 */
class GoalReminderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val hourPicker: HexHourPickerView

    private val dayButtons: List<Pair<MaterialButton, Int>>

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.view_goal_reminder, this, true)

        hourPicker = findViewById(R.id.hour_picker)
        hourPicker.setSelectedTime(DEFAULT_TIME)

        dayButtons = listOf(
            findViewById<MaterialButton>(R.id.btn_day_mon) to 1,
            findViewById<MaterialButton>(R.id.btn_day_tue) to 2,
            findViewById<MaterialButton>(R.id.btn_day_wed) to 3,
            findViewById<MaterialButton>(R.id.btn_day_thu) to 4,
            findViewById<MaterialButton>(R.id.btn_day_fri) to 5,
            findViewById<MaterialButton>(R.id.btn_day_sat) to 6,
            findViewById<MaterialButton>(R.id.btn_day_sun) to 7
        )
        dayButtons.forEach { (button, _) ->
            button.setOnClickListener { button.isSelected = !button.isSelected }
        }
    }

    // ── Public API ────────────────────────────────────────────────

    fun getSelectedTime(): String = hourPicker.getSelectedTime()

    fun getSelectedDays(): List<Int> =
        dayButtons.filter { (button, _) -> button.isSelected }
            .map { (_, dayValue) -> dayValue }

    fun hasSelectedDays(): Boolean = dayButtons.any { (button, _) -> button.isSelected }

    // CSV of selected weekday numbers (e.g. "1,3,5"); null when nothing selected
    fun getSelectedDaysCsv(): String? =
        getSelectedDays().takeIf { it.isNotEmpty() }?.joinToString(",")

    // Restore previously saved reminder: center picker on [time], re-select [daysCsv]
    fun setReminder(time: String?, daysCsv: String?) {
        time?.let { hourPicker.setSelectedTime(it) }
        val days = daysCsv?.split(",")?.mapNotNull { it.trim().toIntOrNull() }?.toSet().orEmpty()
        dayButtons.forEach { (button, dayValue) ->
            button.isSelected = dayValue in days
        }
    }

    companion object {
        private const val DEFAULT_TIME = "18:00"
    }
}
