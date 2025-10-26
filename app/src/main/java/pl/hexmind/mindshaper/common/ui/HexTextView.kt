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

    private val bulletPointsListOpeningChar = "X "

    private fun updateFormattedText() {
        val html = convertToHtml(originalText)
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

    private fun convertToHtml(text: String): String {
        if (text.isEmpty()) return ""

        val lines = text.split("\n")
        val result = StringBuilder()
        var inList = false

        for (line in lines) {
            val trimmed = line.trim()

            if (trimmed.startsWith(bulletPointsListOpeningChar, ignoreCase = true)) {
                if (!inList) {
                    result.append("<ul>")
                    inList = true
                }
                result.append("<li>")
                    .append(trimmed.substring(2))
                    .append("</li>")
            } else {
                if (inList) {
                    result.append("</ul>")
                    inList = false
                }
                if (trimmed.isNotEmpty()) {
                    result.append(trimmed).append("<br>")
                }
            }
        }

        if (inList) {
            result.append("</ul>")
        }

        return result.toString()
    }
}