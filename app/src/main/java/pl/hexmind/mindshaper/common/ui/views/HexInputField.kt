package pl.hexmind.mindshaper.common.ui.views

import android.content.Context
import android.text.InputFilter
import android.text.InputType
import android.text.Spanned
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
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

    // inline hint - placeholder lives in the EditText itself and disappears on focus, no floating label
    private var inlineHint = false

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

                inlineHint = typedArray.getBoolean(R.styleable.HexInputField_inlineHint, false)
                if (inlineHint) applyInlineHintMode()

                if (typedArray.getBoolean(R.styleable.HexInputField_compact, false)) applyCompactMode()

                val inputFilters = mutableListOf<InputFilter>()

                val maxLength = typedArray.getInt(R.styleable.HexInputField_hexMaxLength, 0)
                if (maxLength > 0) inputFilters += InputFilter.LengthFilter(maxLength)

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
                if (maxLines > 1) {
                    binding.etInput.isSingleLine = false
                    // ! EditText.maxLines only caps the visible height - Enter kept adding lines past it
                    inputFilters += MaxLineBreaksFilter(maxLines)
                }

                if (inputFilters.isNotEmpty()) binding.etInput.filters = inputFilters.toTypedArray()

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
            if (inlineHint) binding.etInput.hint = if (hasFocus) null else restingHint
            externalFocusListener?.invoke(hasFocus)
        }
    }

    private fun applyInlineHintMode() {
        twoStateHint = false
        binding.tilInput.hint = null
        binding.tilInput.isHintEnabled = false
        binding.etInput.hint = restingHint

        // The top margin only makes room for the floating label - nothing floats here
        binding.etInput.updateLayoutParams<MarginLayoutParams> { topMargin = 0 }
    }

    /** Slimmer resting height - the field still grows line by line (maxLines) as text is typed. */
    private fun applyCompactMode() {
        val verticalPadding = (COMPACT_VERTICAL_PADDING_DP * resources.displayMetrics.density).toInt()
        binding.etInput.setPadding(
            binding.etInput.paddingLeft, verticalPadding,
            binding.etInput.paddingRight, verticalPadding
        )
    }

    /**
     * Allows at most [maxLines] lines of text: on the last line Enter does nothing.
     * Pasted text keeps its first allowed line breaks, the rest become spaces.
     */
    private class MaxLineBreaksFilter(private val maxLines: Int) : InputFilter {

        override fun filter(
            source: CharSequence, start: Int, end: Int,
            dest: Spanned, dstart: Int, dend: Int
        ): CharSequence? {
            val incoming = source.subSequence(start, end)
            if (!incoming.contains(LINE_BREAK)) return null // Nothing to limit - keep the input as is

            // Line breaks left in the text once the replaced range [dstart, dend) is gone
            val keptBreaks = dest.count { it == LINE_BREAK } - dest.subSequence(dstart, dend).count { it == LINE_BREAK }
            var breaksLeft = maxLines - 1 - keptBreaks

            // A lone Enter on the last line is simply swallowed
            if (incoming.length == 1 && breaksLeft <= 0) return ""

            return buildString {
                incoming.forEach { character ->
                    when {
                        character != LINE_BREAK -> append(character)
                        breaksLeft > 0          -> { append(character); breaksLeft-- }
                        else                    -> append(' ')
                    }
                }
            }
        }

        companion object {
            private const val LINE_BREAK = '\n'
        }
    }

    companion object {
        private const val COMPACT_VERTICAL_PADDING_DP = 6
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
        if (inlineHint) {
            binding.etInput.hint = if (binding.etInput.hasFocus()) null else hint
        }
        else {
            binding.tilInput.hint = hint
        }
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
        // ! Called on every keystroke - without this guard each one started a layout transition, which
        // animated a multiline field growing on Enter and hid the typed text for a moment
        if (!binding.tvError.isVisible) return

        val transition = AutoTransition().apply { duration = 400 }
        TransitionManager.beginDelayedTransition(this as ViewGroup, transition)
        binding.etInput.setBackgroundResource(R.drawable.shape_edit_text)
        binding.tvError.visibility = GONE
    }
}