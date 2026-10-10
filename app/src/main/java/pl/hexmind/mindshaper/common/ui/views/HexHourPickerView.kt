package pl.hexmind.mindshaper.common.ui.views

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import pl.hexmind.mindshaper.R
import kotlin.math.abs

/**
 * Horizontal time-of-day picker - center slot is selected, side slots scale down.
 * Slots every [stepMinutes] (attr `stepMinutes`, default 30): 60 = full hours only.
 * Disabled = dimmed, no scrolling.
 *
 * Usage:
 *   picker.setSelectedTime("18:00")      picker.getSelectedTime()
 *   picker.setSelectedHour(9)            picker.getSelectedHour()
 */
class HexHourPickerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val timeSlots: List<String>

    private val rvHours = RecyclerView(context)
    private val layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

    // Kept apart from the scroll position - valid also before the first layout
    private var selectedIndex: Int = 0

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.HexHourPickerView)
        val stepMinutes = typedArray.getInt(R.styleable.HexHourPickerView_stepMinutes, DEFAULT_STEP_MINUTES)
        typedArray.recycle()

        timeSlots = (0 until MINUTES_IN_DAY step stepMinutes).map { minute ->
            String.format("%02d:%02d", minute / 60, minute % 60)
        }

        rvHours.layoutManager = layoutManager
        rvHours.adapter = HourPickerAdapter(timeSlots)
        rvHours.overScrollMode = OVER_SCROLL_NEVER
        // Edge padding lets first/last slot reach center; clipToPadding=false keeps them drawn
        rvHours.clipToPadding = false
        addView(rvHours, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        LinearSnapHelper().attachToRecyclerView(rvHours)

        rvHours.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                scaleVisibleHours()
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    centerSlotIndex().takeIf { it != RecyclerView.NO_POSITION }?.let { selectedIndex = it }
                }
            }
        })

        rvHours.doOnLayout { centerOnSelectedSlot() }
    }

    // Re-center when the picker becomes visible - doOnLayout in init fires while still GONE (width=0)
    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == VISIBLE) {
            rvHours.doOnLayout { centerOnSelectedSlot() }
        }
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        alpha = if (enabled) 1f else DISABLED_ALPHA
    }

    // Disabled = touches stop here, the list does not scroll
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean =
        !isEnabled || super.onInterceptTouchEvent(ev)

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean =
        !isEnabled || super.onTouchEvent(event)

    // Center [selectedIndex] using padding + offset; needs a non-zero width to work
    private fun centerOnSelectedSlot() {
        if (rvHours.width == 0) return
        val sidePadding = rvHours.width / 2 - dpToPx(ITEM_HALF_WIDTH_DP)
        if (rvHours.paddingLeft != sidePadding) {
            rvHours.setPadding(sidePadding, 0, sidePadding, 0)
        }
        // Scroll after padding is applied so the offset is measured against the padded layout
        rvHours.post {
            layoutManager.scrollToPositionWithOffset(selectedIndex, 0)
            rvHours.post { scaleVisibleHours() }
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

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    // ── Public API ────────────────────────────────────────────────

    fun getSelectedTime(): String = timeSlots[selectedIndex]

    // Time outside the slots (e.g. "09:15" with full hours) is ignored
    fun setSelectedTime(time: String) {
        val slotIndex = timeSlots.indexOf(time)
        if (slotIndex < 0) return
        selectedIndex = slotIndex
        if (rvHours.width > 0) centerOnSelectedSlot()
    }

    fun getSelectedHour(): Int = getSelectedTime().substringBefore(':').toInt()

    fun setSelectedHour(hour: Int) = setSelectedTime(String.format("%02d:00", hour))

    companion object {
        private const val MINUTES_IN_DAY = 24 * 60
        private const val DEFAULT_STEP_MINUTES = 30
        private const val MIN_SCALE = 0.55f
        private const val MAX_SCALE = 1.0f
        private const val ALPHA_MIN = 0.4f
        private const val DISABLED_ALPHA = 0.4f
        private const val ITEM_HALF_WIDTH_DP = 30
        private const val CENTER_RATIO_THRESHOLD = 0.92f
    }
}
