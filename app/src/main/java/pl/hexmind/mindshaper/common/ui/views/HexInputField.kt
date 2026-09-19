package pl.hexmind.mindshaper.common.ui.views

import android.content.Context
import android.text.InputType
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.databinding.CommonHexInputFieldBinding

/**
 * Reusable input field widget.
 * Displays value, shows/clears errors with animation.
 *
 * XML usage:
 *   <pl.hexmind.mindshaper.common.ui.views.HexInputField
 *       android:layout_width="match_parent"
 *       android:layout_height="wrap_content"
 *       app:hint="@string/common_hex_tags_hint_person"
 *       app:inputType="number"
 *       app:maxLines="1" />
 */
class HexInputField @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding = CommonHexInputFieldBinding.inflate(
        LayoutInflater.from(context), this
    )

    // two-state hint - resting placeholder swaps to the floating-label text once
    // the field is focused or non-empty; both fall back to app:hint when the paired attr is absent
    private var restingHint: String? = null
    private var floatingHint: String? = null
    private var twoStateHint = false
    private var externalFocusListener: ((Boolean) -> Unit)? = null

    init {
        orientation = VERTICAL

        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.HexInputField)
            try {
                val hint = typedArray.getString(R.styleable.HexInputField_hint)
                restingHint = typedArray.getString(R.styleable.HexInputField_hintPlaceholder) ?: hint
                floatingHint = typedArray.getString(R.styleable.HexInputField_hintFloating) ?: hint
                // Two-state only when the two texts actually differ; otherwise a plain single hint
                twoStateHint = restingHint != floatingHint
                binding.tilInput.hint = restingHint

                // Apply hexInputType enum: all_chars=0 (default), text=1, number=2
                val hexInputType = typedArray.getInt(R.styleable.HexInputField_hexInputType, 0)
                binding.etInput.inputType = when (hexInputType) {
                    1    -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                    2    -> InputType.TYPE_CLASS_NUMBER
                    else -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
                }

                // Apply maxLines if provided, otherwise keep default (1)
                val maxLines = typedArray.getInt(R.styleable.HexInputField_maxLines, 1)
                binding.etInput.maxLines = maxLines
                if (maxLines > 1) binding.etInput.isSingleLine = false

                // Tick shown only where the field commits tags; hidden in plain inputs
                binding.ivConfirm.isVisible =
                    typedArray.getBoolean(R.styleable.HexInputField_showConfirmIcon, false)

            } finally {
                typedArray.recycle()
            }
        }

        binding.etInput.addTextChangedListener { clearError() }

        // swap resting <-> floating hint; "floated" = focused OR non-empty, so a filled field keeps the floating text after focus leaves
        if (twoStateHint) {
            binding.etInput.addTextChangedListener { applyStatefulHint() }
        }
        // Tone focus listener owns the view slot (a View has only one) - it does the hint swap AND forwards to the listener stored via setOnFocusChangeListener
        binding.etInput.setOnFocusChangeListener { _, hasFocus ->
            if (twoStateHint) applyStatefulHint()
            externalFocusListener?.invoke(hasFocus)
        }
    }

    fun addTextChangedListener(listener: (String) -> Unit) {
        binding.etInput.addTextChangedListener { listener(it?.toString().orEmpty()) }
    }

    // ── Public API ────────────────────────────────────────────────

    /** The "confirm tick" commits the typed text as a tag - same effect as ending with a space. */
    fun setOnConfirmClickListener(listener: () -> Unit) {
        binding.ivConfirm.setOnClickListener { listener() }
    }

    fun setOnFocusChangeListener(listener: (Boolean) -> Unit) {
        // stored, not set directly - the init focus listener owns the slot and forwards here, so the two-state hint swap is not clobbered
        externalFocusListener = listener
    }

    fun setHint(hint: String) {
        // programmatic single hint overrides any two-state config
        restingHint = hint
        floatingHint = hint
        twoStateHint = false
        binding.tilInput.hint = hint
    }

    private fun applyStatefulHint() {
        val floated = binding.etInput.hasFocus() || !binding.etInput.text.isNullOrEmpty()
        binding.tilInput.hint = if (floated) floatingHint else restingHint
    }

    fun requestFocusOnField() {
        binding.etInput.requestFocus()
    }

    fun getText(): String = binding.etInput.text?.toString()?.trim().orEmpty()

    /** Untrimmed, so a trailing separator stays visible - suggestions need it to spot a new tag. */
    fun getRawText(): String = binding.etInput.text?.toString().orEmpty()

    fun setText(value: String?) {
        binding.etInput.setText(value)

        // ! setText parks the cursor at position 0 - without this, picking a suggestion throws the
        // user back to the start of the field mid-typing
        binding.etInput.setSelection(binding.etInput.text?.length ?: 0)
    }

    fun showError(message: String) {
        TransitionManager.beginDelayedTransition(this as ViewGroup)
        binding.etInput.setBackgroundResource(R.drawable.shape_edit_text_error)
        binding.tvError.text = message
        binding.tvError.visibility = VISIBLE
    }

    fun clearError() {
        val transition = AutoTransition().apply { duration = 400 }
        TransitionManager.beginDelayedTransition(this as ViewGroup, transition)
        binding.etInput.setBackgroundResource(R.drawable.shape_edit_text)
        binding.tvError.visibility = GONE
    }
}