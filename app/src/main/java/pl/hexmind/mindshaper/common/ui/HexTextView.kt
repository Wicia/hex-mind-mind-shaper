package pl.hexmind.mindshaper.common.ui

import android.content.Context
import android.util.AttributeSet
import org.sufficientlysecure.htmltextview.HtmlTextView

/**
 * Custom TextView that automatically converts "X item" format to HTML bullet lists
 */
class HexTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : HtmlTextView(context, attrs, defStyleAttr) {

    var originalText: String = ""
        set(value) {
            field = value
            updateFormattedText()
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

    private fun updateFormattedText() {
        val html = HtmlConverter.convertToHtml(originalText)
        setHtml(html)
        applyClickBehavior()
    }

    /**
     * Applies click behavior configuration after HTML rendering
     * Must be called after setHtml() as it resets these properties
     */
    private fun applyClickBehavior() {
        movementMethod = null
        isClickable = !propagateClickEventsToParent
        isFocusable = !propagateClickEventsToParent
    }
}