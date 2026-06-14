package pl.hexmind.mindshaper.activities.workshop

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import pl.hexmind.mindshaper.R
import kotlin.math.abs

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

    // 00:00 .. 23:30 every 30 minutes
    private val timeSlots: List<String> = buildList {
        for (hour in 0..23) {
            add(String.format("%02d:00", hour))
            add(String.format("%02d:30", hour))
        }
    }

    private val rvHours: RecyclerView
    private val hourAdapter = HourPickerAdapter(timeSlots)
    private val layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

    private val dayButtons: List<Pair<MaterialButton, Int>>

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.view_goal_reminder, this, true)

        rvHours = findViewById(R.id.rv_hours)
        rvHours.layoutManager = layoutManager
        rvHours.adapter = hourAdapter
        LinearSnapHelper().attachToRecyclerView(rvHours)

        rvHours.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                scaleVisibleHours()
            }
        })

        // Edge padding lets first/last slot reach center; clipToPadding=false keeps them drawn
        rvHours.doOnLayout {
            val sidePadding = rvHours.width / 2 - dpToPx(ITEM_HALF_WIDTH_DP)
            rvHours.setPadding(sidePadding, 0, sidePadding, 0)
            rvHours.clipToPadding = false
            layoutManager.scrollToPosition(DEFAULT_SLOT_INDEX)
            rvHours.post { scaleVisibleHours() }
        }

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

    // Scale dots by distance from center: center largest, edges smallest
    private fun scaleVisibleHours() {
        val center = rvHours.width / 2f
        for (index in 0 until rvHours.childCount) {
            val child = rvHours.getChildAt(index)
            val childCenter = (child.left + child.right) / 2f
            val distance = abs(center - childCenter)
            val ratio = (1f - distance / center).coerceIn(0f, 1f)
            val scale = MIN_SCALE + (MAX_SCALE - MIN_SCALE) * ratio
            child.scaleX = scale
            child.scaleY = scale
            child.alpha = ALPHA_MIN + (1f - ALPHA_MIN) * ratio

            // Tint the centermost dot orange, the rest gray
            val dot = child.findViewById<View>(R.id.v_hour_dot) ?: continue
            val isCenter = ratio > CENTER_RATIO_THRESHOLD
            dot.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    context,
                    if (isCenter) R.color._orange_lvl_3 else R.color._gray_lvl_3
                )
            )
        }
    }

    private fun centerSlotIndex(): Int {
        val center = rvHours.width / 2f
        var bestPosition = RecyclerView.NO_POSITION
        var bestDistance = Float.MAX_VALUE
        for (index in 0 until rvHours.childCount) {
            val child = rvHours.getChildAt(index)
            val childCenter = (child.left + child.right) / 2f
            val distance = abs(center - childCenter)
            if (distance < bestDistance) {
                bestDistance = distance
                bestPosition = layoutManager.getPosition(child)
            }
        }
        return bestPosition
    }

    // TODO: Move it to commons?
    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    // ── Public API ────────────────────────────────────────────────

    fun getSelectedTime(): String {
        val slot = centerSlotIndex().coerceIn(timeSlots.indices)
        return timeSlots[slot]
    }

    fun getSelectedDays(): List<Int> =
        dayButtons.filter { (button, _) -> button.isSelected }
            .map { (_, dayValue) -> dayValue }

    companion object {
        private const val MIN_SCALE = 0.55f
        private const val MAX_SCALE = 1.0f
        private const val ALPHA_MIN = 0.4f
        private const val ITEM_HALF_WIDTH_DP = 30
        private const val DEFAULT_SLOT_INDEX = 36  // 18:00
        private const val CENTER_RATIO_THRESHOLD = 0.92f
    }
}