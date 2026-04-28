package pl.hexmind.mindshaper.common.ui.views.values

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import pl.hexmind.mindshaper.R

/**
 * Displays thought value as a cloud shape with a number label.
 * More: ic_cloud_shape.xml
 */
class ValueCloude @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    companion object {
        const val DEFAULT_LEVEL = 0
    }

    private val ivCloud: ImageView
    private val tvValue: TextView

    var currentLevel: Int = DEFAULT_LEVEL
        set(value) {
            if (value != field) startFlashAnimation()
            field = value
            tvValue.text = if (isLocked) "?" else "$value"
            onLevelChangeListener?.invoke(field)
        }

    var cloudColor: Int = R.color._orange_lvl_2
        set(value) {
            field = value
            applyCloudColor()
        }

    var isLocked: Boolean = false
        set(value) {
            field = value
            applyCloudColor()
            tvValue.text = if (value) "?" else "$currentLevel"
        }

    var showLevelText: Boolean = true
        set(value) {
            field = value
            tvValue.visibility = if (value) VISIBLE else GONE
        }

    // Callback
    var onLevelChangeListener: ((Int) -> Unit)? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.common_value_cloud_view, this, true)
        ivCloud = findViewById(R.id.iv_cloud_shape)
        tvValue = findViewById(R.id.tv_cloud_value)

        attrs?.let {
            val styledAttr = context.obtainStyledAttributes(it, R.styleable.ValueCloude, 0, 0)
            try {
                cloudColor    = styledAttr.getResourceId(R.styleable.ValueCloude_cloudColor, R.color._orange_lvl_2)
                val defaultTextSizePx = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_SP, 14f, resources.displayMetrics
                )
                val textSizePx = styledAttr.getDimension(R.styleable.ValueCloude_valueTextSize, defaultTextSizePx)
                tvValue.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizePx)
                showLevelText = styledAttr.getBoolean(R.styleable.ValueCloude_showLevelText, true)

                val textColor = styledAttr.getResourceId(R.styleable.ValueCloude_valueTextColor, R.color._black)
                tvValue.setTextColor(ContextCompat.getColor(context, textColor))
            }
            finally {
                styledAttr.recycle()
            }
        }

        applyCloudColor()
        tvValue.text = "$currentLevel"
    }

    private fun applyCloudColor() {
        val colorRes = if (isLocked) R.color._gray_lvl_2 else cloudColor
        ImageViewCompat.setImageTintList(
            ivCloud,
            android.content.res.ColorStateList.valueOf(ContextCompat.getColor(context, colorRes))
        )
    }

    // Flash animation on value change — simple scale pulse
    private fun startFlashAnimation() {
        val scaleX = PropertyValuesHolder.ofFloat("scaleX", 1f, 1.15f, 1f)
        val scaleY = PropertyValuesHolder.ofFloat("scaleY", 1f, 1.15f, 1f)
        ObjectAnimator.ofPropertyValuesHolder(this, scaleX, scaleY).apply {
            duration = 350
            start()
        }
    }
}