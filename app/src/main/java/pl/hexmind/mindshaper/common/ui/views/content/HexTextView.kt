package pl.hexmind.mindshaper.common.ui.views.content

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BulletSpan
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.text.style.URLSpan
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.content.withStyledAttributes
import androidx.core.net.toUri
import com.google.android.material.button.MaterialButton
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonSpansFactory
import io.noties.markwon.SoftBreakAddsNewLinePlugin
import org.commonmark.node.ListItem
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.common.regex.HexTagsUtils
import pl.hexmind.mindshaper.common.ui.dialogs.ActionsDialog
import timber.log.Timber

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
                emphasizeInlineTags(textView)
                linkifyWebUrls(textView)
            }
        })
        .build()

    private val textView: TextView
    val btnDelete: MaterialButton

    private var mode: Mode = Mode.EDIT_DISPLAY
    private var callback: TextCallback? = null

    // Link under the finger since ACTION_DOWN - opened on ACTION_UP only if the finger is still on it
    private var pressedLink: ClickableSpan? = null

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

    @SuppressLint("ClickableViewAccessibility")
    private fun setupListeners() {
        textView.setOnClickListener {
            callback?.onTextClicked()
        }

        textView.setOnTouchListener { _, event -> handleLinkTouch(event) }

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
     * Renders inline @person / #project tags as bold, colored words without their markers.
     * Only the displayed text changes - [originalText] (edited in the editor) keeps the markers.
     */
    private fun emphasizeInlineTags(textView: TextView) {
        val rendered = textView.text
        val matches = HexTagsUtils.EMBEDDED_TAG_PATTERN.findAll(rendered).toList()
        if (matches.isEmpty()) return

        val builder = SpannableStringBuilder(rendered)
        val tagColor = ContextCompat.getColor(context, R.color.rich_text_inline_tag)

        // ! Go backwards - deleting a marker shifts every index after it
        matches.asReversed().forEach { match ->
            val start = match.range.first
            val end = match.range.last + 1
            builder.setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            builder.setSpan(ForegroundColorSpan(tagColor), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            builder.delete(start, start + 1)
        }

        textView.text = builder
    }

    /**
     * Turns plain web addresses (http(s)://..., www....) into links - blue & underlined via textColorLink.
     * Markdown links [label](url) are already URLSpans from Markwon, so their ranges are skipped.
     * Every link (plain or Markdown) gets its own font, so it stands out from the Alegreya prose.
     */
    private fun linkifyWebUrls(textView: TextView) {
        val builder = SpannableStringBuilder(textView.text)

        WEB_URL_PATTERN.findAll(builder).toList().forEach { match ->
            val start = match.range.first
            val end = match.range.last + 1
            if (builder.getSpans(start, end, URLSpan::class.java).isNotEmpty()) return@forEach

            val url = if (match.value.startsWith("www.", ignoreCase = true)) "https://${match.value}" else match.value
            builder.setSpan(URLSpan(url), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        val links = builder.getSpans(0, builder.length, URLSpan::class.java)
        if (links.isEmpty()) return

        ResourcesCompat.getFont(context, R.font.montserrat_regular)?.let { linkFont ->
            links.forEach { link ->
                val start = builder.getSpanStart(link)
                val end = builder.getSpanEnd(link)
                builder.setSpan(TypefaceSpan(linkFont), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                builder.setSpan(RelativeSizeSpan(LINK_SIZE_RATIO), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }

        textView.text = builder
    }

    /**
     * Read-only mode only: a tap on a link opens it, any other touch falls through to the parent
     * (e.g. Stream item opening the thought). LinkMovementMethod is not an option - it swallows every touch.
     */
    private fun handleLinkTouch(event: MotionEvent): Boolean {
        if (mode != Mode.DISPLAY_ONLY) return false

        val link = findLinkAt(event)
        return when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                pressedLink = link
                link != null
            }
            MotionEvent.ACTION_UP -> {
                val pressed = pressedLink ?: return false
                pressedLink = null
                if (pressed == link) openLink(pressed)
                true
            }
            MotionEvent.ACTION_CANCEL -> {
                val wasPressed = pressedLink != null
                pressedLink = null
                wasPressed
            }
            else -> pressedLink != null
        }
    }

    private fun findLinkAt(event: MotionEvent): ClickableSpan? {
        val spanned = textView.text as? Spanned ?: return null
        val layout = textView.layout ?: return null

        val x = event.x.toInt() - textView.totalPaddingLeft + textView.scrollX
        val y = event.y.toInt() - textView.totalPaddingTop + textView.scrollY
        val line = layout.getLineForVertical(y)

        // ! Empty space right of a line's last word maps to its last character - not a tap on the link
        if (x < layout.getLineLeft(line) || x > layout.getLineRight(line)) return null

        val offset = layout.getOffsetForHorizontal(line, x.toFloat())
        return spanned.getSpans(offset, offset, ClickableSpan::class.java).firstOrNull()
    }

    private fun openLink(link: ClickableSpan) {
        if (link !is URLSpan) {
            link.onClick(textView)
            return
        }

        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, link.url.toUri()))
        }
        catch (e: ActivityNotFoundException) {
            Timber.w(e, "No app to open link: ${link.url}")
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
            .setPrimaryAction(context.getString(R.string.common_deletion_dialog_yes_2), caution = true) {
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

        // Montserrat runs wider than Alegreya - at the same size a link looks oversized next to the prose
        private const val LINK_SIZE_RATIO = 0.85f

        // Trailing punctuation ("zobacz https://x.pl.") belongs to the sentence, not to the address
        private val WEB_URL_PATTERN = Regex("(?:https?://|www\\.)\\S+?(?=[.,;:!?)\\]\"']*(?:\\s|$))", RegexOption.IGNORE_CASE)
    }
}