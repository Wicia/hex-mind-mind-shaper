package pl.hexmind.mindshaper.common.ui.views

import android.content.Context
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.widget.addTextChangedListener
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.databinding.CommonHexInputFieldBinding

/**
 * Reusable input field widget.
 * Displays value, shows/clears errors with animation.
 *
 * XML usage:bo
 *   <pl.hexmind.mindshaper.common.ui.views.HexInputField
 *       android:layout_width="match_parent"
 *       android:layout_height="wrap_content"
 *       app:hint="@string/hex_tags_hint_person" />
 */
class HexInputField @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding = CommonHexInputFieldBinding.inflate(
        LayoutInflater.from(context), this
    )

    init {
        orientation = VERTICAL

        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.HexInputField)
            try {
                binding.tilInput.hint = typedArray.getString(R.styleable.HexInputField_hint)
            } finally {
                typedArray.recycle()
            }
        }

        binding.etInput.addTextChangedListener { clearError() }
    }

    // ── Public API ────────────────────────────────────────────────

    fun getText(): String = binding.etInput.text?.toString()?.trim().orEmpty()

    fun setText(value: String?) {
        binding.etInput.setText(value)
    }

    fun showError(message: String) {
        TransitionManager.beginDelayedTransition(this as ViewGroup)
        binding.etInput.setBackgroundResource(R.drawable.shape_edit_text_error)
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
    }

    fun clearError() {
        val transition = AutoTransition().apply { duration = 400 }
        TransitionManager.beginDelayedTransition(this as ViewGroup, transition)
        binding.etInput.setBackgroundResource(R.drawable.shape_edit_text)
        binding.tvError.visibility = View.GONE
    }
}