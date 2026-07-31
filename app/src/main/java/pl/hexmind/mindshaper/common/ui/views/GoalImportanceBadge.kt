package pl.hexmind.mindshaper.common.ui.views

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import pl.hexmind.mindshaper.R

/**
 * Badge displaying goal importance level (1-3) with mapped background color.
 *
 * XML usage:
 *   <pl.hexmind.mindshaper.common.ui.views.GoalImportanceBadge
 *       android:layout_width="32dp"
 *       android:layout_height="32dp"
 *       app:importance="2" />
 *
 * Programmatic:
 *   badge.setImportance(goal.importance)
 */
class GoalImportanceBadge @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    init {
        gravity = Gravity.CENTER
        setBackgroundResource(R.drawable.shape_workshop_priority_badge)
        setTextColor(ContextCompat.getColor(context, R.color.text_primary))
        setTypeface(typeface, Typeface.BOLD)

        attrs?.let {
            val ta = context.obtainStyledAttributes(it, R.styleable.GoalImportanceBadge)
            try {
                setImportance(ta.getInt(R.styleable.GoalImportanceBadge_importance, 1))
            } finally {
                ta.recycle()
            }
        }
    }

    fun setImportance(level: Int) {
        // archived / importance cleared
        if (level <= 0) {
            setArchived()
            return
        }
        val safe = level.coerceIn(1, 3)
        text = safe.toString()
        val bgRes = when (safe) {
            3    -> R.color.importance_high
            2    -> R.color.importance_medium
            else -> R.color.importance_low
        }
        backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, bgRes))
    }

    // Archived goal: badge: empty + gray
    private fun setArchived() {
        text = ""
        backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color._gray_lvl_2))
    }
}