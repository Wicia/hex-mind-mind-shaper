package pl.hexmind.mindshaper.common.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.CompoundButton
import android.widget.LinearLayout
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.databinding.CommonHexSwitchRowBinding

/**
 * Switch on the left, its label on the right (left-aligned).
 * Tapping the label toggles the switch; a disabled row dims the label as well.
 *
 * XML usage:
 *   <pl.hexmind.mindshaper.common.ui.views.HexSwitchRow
 *       android:layout_width="match_parent"
 *       android:layout_height="wrap_content"
 *       android:text="@string/settings_permissions_photo" />
 */
class HexSwitchRow @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding = CommonHexSwitchRowBinding.inflate(
        LayoutInflater.from(context), this
    )

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = (MIN_HEIGHT_DP * resources.displayMetrics.density).toInt()

        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.HexSwitchRow)
            try {
                binding.tvLabel.text = typedArray.getText(R.styleable.HexSwitchRow_android_text)
            } finally {
                typedArray.recycle()
            }
        }

        // ! Every row holds a switch with the same id - restored state would land in the wrong rows
        // (and fire their listeners); the screen reads the checked state from its storage anyway
        binding.switchToggle.isSaveEnabled = false

        extendTrack()

        setOnClickListener { binding.switchToggle.toggle() }
    }

    /** Track longer than the default one by [TRACK_EXTRA_WIDTH_DP]; the thumb travels the extra distance. */
    private fun extendTrack() {
        val switchToggle = binding.switchToggle

        // ! The 48dp touch-target minWidth hides the default track width - measured without it
        val touchTargetWidth = switchToggle.minimumWidth
        switchToggle.minimumWidth = 0
        switchToggle.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED)
        val defaultTrackWidth = switchToggle.measuredWidth
        switchToggle.minimumWidth = touchTargetWidth

        switchToggle.switchMinWidth = defaultTrackWidth + (TRACK_EXTRA_WIDTH_DP * resources.displayMetrics.density).toInt()
    }

    // ── Public API ────────────────────────────────────────────────

    var isChecked: Boolean
        get() = binding.switchToggle.isChecked
        set(value) {
            binding.switchToggle.isChecked = value
        }

    fun setOnCheckedChangeListener(listener: CompoundButton.OnCheckedChangeListener?) {
        binding.switchToggle.setOnCheckedChangeListener(listener)
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        binding.switchToggle.isEnabled = enabled
        binding.tvLabel.alpha = if (enabled) 1f else DISABLED_ALPHA
    }

    companion object {
        private const val MIN_HEIGHT_DP = 48
        private const val DISABLED_ALPHA = 0.4f
        private const val TRACK_EXTRA_WIDTH_DP = 4
    }
}
