package pl.hexmind.mindshaper.common.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.databinding.ViewHexTagsInputBinding

/**
 * One tag type in the sheet: a field, a row of suggestions, and the chosen tags as pills.
 */
class HexTagsInputView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding = ViewHexTagsInputBinding.inflate(LayoutInflater.from(context), this)

    private val tagNames = mutableListOf<String>()

    var suggestionsProvider: (suspend (String) -> List<String>)? = null
    var queryRunner: ((suspend () -> Unit) -> Unit)? = null
    var onTagsChanged: (() -> Unit)? = null

    init {
        orientation = VERTICAL

        binding.hifTag.addTextChangedListener { text ->
            commitOnSpace(text)
            refreshSuggestions()
        }

        // The tick commits whatever is typed, exactly as a trailing space would
        binding.hifTag.setOnConfirmClickListener { commitCurrentText() }
    }

    fun setHint(hintRes: Int) {
        binding.hifTag.setHint(context.getString(hintRes))
    }

    fun getTags(): List<String> = tagNames.toList()

    fun setTags(names: List<String>) {
        tagNames.clear()
        tagNames += names.map { name -> normalize(name) }.filter { name -> name.isNotEmpty() }
        renderChips()
    }

    // Only space commits a tag here - a single tag never needs a comma
    private fun commitOnSpace(text: String) {
        if (text.isEmpty() || !text.last().isWhitespace()) return

        addTag(text)
        binding.hifTag.setText("")
    }

    // Tick tap: same outcome as a trailing space, minus the whitespace requirement
    private fun commitCurrentText() {
        val typedText = binding.hifTag.getText()
        if (typedText.isEmpty()) return

        addTag(typedText)
        binding.hifTag.setText("")
        binding.hifTag.requestFocusOnField()
    }

    private fun commitSuggestion(name: String) {
        addTag(name)
        binding.hifTag.setText("")
        binding.hifTag.requestFocusOnField()
    }

    private fun addTag(rawName: String) {
        val name = normalize(rawName)
        if (name.isEmpty() || tagNames.contains(name)) return

        tagNames += name
        renderChips()
        onTagsChanged?.invoke()
    }

    private fun removeTag(name: String) {
        tagNames -= name
        renderChips()
        onTagsChanged?.invoke()
    }

    private fun renderChips() {
        binding.cgTags.removeAllViews()
        tagNames.forEach { name -> binding.cgTags.addView(buildSelectedChip(name)) }
        binding.cgTags.isVisible = tagNames.isNotEmpty()
    }

    private fun buildSelectedChip(name: String): View {
        val chip = LayoutInflater.from(context)
            .inflate(R.layout.common_hex_tags_selected, binding.cgTags, false)

        chip.findViewById<TextView>(R.id.tv_tag_name).text = name
        chip.findViewById<ImageView>(R.id.iv_remove).setOnClickListener { removeTag(name) }
        return chip
    }

    private fun refreshSuggestions() {
        val provider = suggestionsProvider ?: return
        val runner = queryRunner ?: return

        val typedText = binding.hifTag.getText()

        if (typedText.isEmpty()) {
            binding.llSuggestionsRow.isVisible = false
            return
        }

        runner {
            val names = provider(typedText).filter { name -> !tagNames.contains(name) }

            binding.llSuggestions.removeAllViews()
            names.forEach { name -> binding.llSuggestions.addView(buildSuggestionChip(name)) }

            val wasHidden = !binding.llSuggestionsRow.isVisible
            binding.llSuggestionsRow.isVisible = names.isNotEmpty()

            // Fade the row in only when it first appears, not on every keystroke
            if (names.isNotEmpty() && wasHidden) {
                binding.llSuggestionsRow.startAnimation(
                    AnimationUtils.loadAnimation(context, R.anim.fade_in_suggestions)
                )
            }
        }
    }

    private fun buildSuggestionChip(name: String): TextView {
        val chip = LayoutInflater.from(context)
            .inflate(R.layout.common_hex_tags_suggestion, binding.llSuggestions, false) as TextView

        chip.text = name
        chip.setOnClickListener { commitSuggestion(name) }
        return chip
    }

    private fun normalize(rawName: String): String =
        rawName.trim().lowercase()
}
