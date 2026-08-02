package pl.hexmind.mindshaper.common.ui.views.content

import android.content.Context
import android.text.Spannable
import android.text.Spanned
import android.text.style.BulletSpan
import android.text.style.RelativeSizeSpan
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.content.withStyledAttributes
import com.google.android.material.button.MaterialButton
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonSpansFactory
import io.noties.markwon.SoftBreakAddsNewLinePlugin
import org.commonmark.node.ListItem
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.common.ui.dialogs.ActionsDialog

/**
 * Rich text display view using Markdown rendering.
 */
class HexTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    enum class Mode {
        EDIT_DISPLAY,
        DISPLAY_ONLY
    }

    interface TextCallback {
        fun onTextClicked()
        fun onTextDeleted()
    }

    private val markwon: Markwon = Markwon.builder(context)
        // Users writing a text expect a line break for single Enter (need to override default CommonMark's feature)
        .usePlugin(SoftBreakAddsNewLinePlugin.create())
        .usePlugin(object : AbstractMarkwonPlugin() {

            override fun configureSpansFactory(builder: MarkwonSpansFactory.Builder) {
                builder.setFactory(ListItem::class.java) { _, _ ->
                    BulletSpan(
                        24,
                        ContextCompat.getColor(context, R.color.graphite_light),
                        8
                    )
                }
            }

            override fun beforeSetText(textView: TextView, markdown: Spanned) {
                // Apply Alegreya font after Markwon sets the text
                textView.typeface = ResourcesCompat.getFont(textView.context, R.font.alegreya_regular)
            }

            override fun afterSetText(textView: TextView) {
                shrinkParagraphGaps(textView)
            }
        })
        .build()

    private val textView: TextView
    val btnDelete: MaterialButton

    private var mode: Mode = Mode.EDIT_DISPLAY
    private var callback: TextCallback? = null

    var originalText: String = ""
        set(value) {
            field = value
            renderMarkdown()
        }

    /**
     * ! Controls whether click events should propagate to parent view.
     * - true (default): Click events pass through to parent (e.g., RecyclerView item)
     * - false: This view handles clicks directly via setOnClickListener
     */
    var propagateClickEventsToParent: Boolean = true
        set(value) {
            field = value
            applyClickBehavior()
        }

    init {
        inflate(context, R.layout.common_markdown_text_view, this)
        orientation = VERTICAL

        textView = findViewById(R.id.markdown_text_view)
        btnDelete = findViewById(R.id.btn_delete)

        attrs?.let {
            context.withStyledAttributes(it, R.styleable.HexTextView) {
                val modeValue = getInt(R.styleable.HexTextView_richTextMode, 0)
                mode = if (modeValue == 1) Mode.DISPLAY_ONLY else Mode.EDIT_DISPLAY
                setupUIForMode()
            }
        }

        setupListeners()
    }

    private fun setupUIForMode() {
        when (mode) {
            Mode.EDIT_DISPLAY -> {
                btnDelete.visibility = VISIBLE
                propagateClickEventsToParent = false
            }
            Mode.DISPLAY_ONLY -> {
                btnDelete.visibility = GONE
                propagateClickEventsToParent = true
            }
        }
    }

    private fun setupListeners() {
        textView.setOnClickListener {
            callback?.onTextClicked()
        }

        btnDelete.setOnClickListener {
            showDeleteConfirmation()
        }
    }

    private fun renderMarkdown() {
        markwon.setMarkdown(textView, originalText)
        applyClickBehavior()
    }

    // To override CommonMark behavior - turning a single Enter into a space (users expect a line break instead)
    private fun shrinkParagraphGaps(textView: TextView) {
        val spannable = textView.text as? Spannable ?: return

        var breakIndex = spannable.indexOf(BLOCK_SEPARATOR)
        while (breakIndex >= 0) {
            spannable.setSpan(
                RelativeSizeSpan(PARAGRAPH_GAP_RATIO),
                breakIndex + 1,
                breakIndex + 2,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            breakIndex = spannable.indexOf(BLOCK_SEPARATOR, breakIndex + 2)
        }
    }

    /**
     * Applies click behavior configuration after Markdown rendering.
     * Must be called after setMarkdown() as it resets these properties.
     */
    private fun applyClickBehavior() {
        textView.movementMethod = null
        textView.isClickable = !propagateClickEventsToParent
        textView.isFocusable = !propagateClickEventsToParent
    }

    private fun showDeleteConfirmation() {
        ActionsDialog.Builder(context)
            .setTitle(context.getString(R.string.details_rich_text_removing_header))
            .setDescription(context.getString(R.string.details_rich_text_removing_content))
            .setCautionAction(context.getString(R.string.common_deletion_dialog_yes_2)) {
                callback?.onTextDeleted()
            }
            .show()
    }

    // ===========================================
    //      Public API Methods
    // ===========================================

    fun setCallback(callback: TextCallback) {
        this.callback = callback
    }

    fun getText(): String = originalText

    companion object {
        private const val BLOCK_SEPARATOR = "\n\n"

        // 1.0 = a full empty line, which reads too airy at 18sp
        private const val PARAGRAPH_GAP_RATIO = 0.6f
    }
}