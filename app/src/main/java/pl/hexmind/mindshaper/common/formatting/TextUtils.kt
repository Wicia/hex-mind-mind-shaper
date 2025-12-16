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