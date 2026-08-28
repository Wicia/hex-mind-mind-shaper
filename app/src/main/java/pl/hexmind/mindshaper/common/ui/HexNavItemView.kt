package pl.hexmind.mindshaper.common.ui

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import pl.hexmind.mindshaper.R

/**
 * Alternative for Material Button - used for displaying bottom nav menu items
 */
class HexNavItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val labelView: TextView
    private val iconView: ImageView

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        isClickable = true
        isFocusable = true

        LayoutInflater.from(context).inflate(R.layout.view_hex_nav_item, this, true)
        labelView = findViewById(R.id.hexNavLabel)
        iconView = findViewById(R.id.hexNavIcon)

        // Icons are orange by default; the controller re-tints on selection
        iconView.imageTintList = ColorStateList.valueOf(context.getColor(R.color._orange_lvl_3))

        context.obtainStyledAttributes(attrs, R.styleable.HexNavItemView).apply {
            getString(R.styleable.HexNavItemView_hexNavLabel)?.let { labelView.text = it }
            val iconRes = getResourceId(R.styleable.HexNavItemView_hexNavIcon, 0)
            if (iconRes != 0) {
                iconView.setImageResource(iconRes)
            }
            recycle()
        }
    }

    fun setLabel(text: CharSequence) {
        labelView.text = text
    }

    // Mirror the MaterialButton API the controller already uses, so it stays a drop-in
    fun setIconResource(@DrawableRes res: Int) {
        iconView.setImageResource(res)
    }

    fun setTextColor(color: Int) {
        labelView.setTextColor(color)
    }

    var iconTint: ColorStateList?
        get() = iconView.imageTintList
        set(value) {
            iconView.imageTintList = value
        }
}
