package pl.hexmind.mindshaper.common.formatting

import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.widget.TextView

fun TextView.setColoredText(
    fullText: String,
    coloredPart: String,
    color: Int
) {
    val spannable = SpannableString(fullText)
    val startIndex = fullText.indexOf(coloredPart)

    if (startIndex >= 0) {
        spannable.setSpan(
            ForegroundColorSpan(color),
            startIndex,
            startIndex + coloredPart.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    this.text = spannable
}

// Single-letter word + the whitespace after it (a no-break space counts - "i w domu" chains)
private val ORPHAN_REGEX = Regex("(?<=^|[\\s\u00A0])(\\p{L})\\s+")

/**
 * No single-letter words ("i", "w", "z"...) left hanging at the end of a line:
 * the space after each one becomes a no-break space, the word wraps together with the next one
 */
fun String.withoutOrphans(): String = replace(ORPHAN_REGEX, "$1\u00A0")
