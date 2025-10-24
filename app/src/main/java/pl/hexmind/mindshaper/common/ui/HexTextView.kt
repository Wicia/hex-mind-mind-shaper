package pl.hexmind.mindshaper.common.ui

import android.content.Context
import android.text.Editable
import android.text.Html
import android.text.Spanned
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import org.xml.sax.XMLReader
import pl.hexmind.mindshaper.R

/**
 * Wrapper for TextView which adds extra features related to displaying bullet points lists
 */
class HexTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    var originalText: String = ""
        set(value) {
            field = value
            updateFormattedText()
        }

    val formattedText: CharSequence
        get() = text

    private val bulletSymbol = "●"
    private val bulletPointsListOpeningChar = "X "
    private val bulletPointsListOpeningTag = "<bullet>"
    private val bulletPointsListClosingTag = "</bullet>"
    private fun updateFormattedText() {
        val html = convertToHtml(originalText)
        text = Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT, null, CustomTagHandler())
    }

    private fun convertToHtml(text: String): String {
        val lines = text.split("\n")
        val result = StringBuilder()

        for (line in lines) {
            val trimmed = line.trim()

            if (trimmed.startsWith(bulletPointsListOpeningChar, ignoreCase = true)) {
                result.append(bulletPointsListOpeningTag)
                    .append(trimmed.substring(2))
                    .append("<br>")
                    .append(bulletPointsListClosingTag)
            } else {
                if (line.isNotEmpty()) {
                    result.append(line).append("<br>")
                }
            }
        }

        return result.toString()
    }

    private inner class CustomTagHandler : Html.TagHandler {
        private var bulletStart = -1

        override fun handleTag(
            opening: Boolean,
            tag: String,
            output: Editable,
            xmlReader: XMLReader
        ) {
            if (tag.equals("bullet", ignoreCase = true)) {
                if (opening) {
                    bulletStart = output.length
                } else {
                    if (bulletStart != -1) {
                        val end = output.length

                        val spanEnd = if (end > 0 && output[end - 1] == '\n') {
                            end
                        }
                        else {
                            output.append('\n')
                            output.length
                        }

                        output.setSpan(
                            UnicodeSymbolSpan(
                                gapWidth = 60,
                                symbol = bulletSymbol,
                                color = context.getColor(R.color.text_bullet_point),
                                textSize = 1.0f
                            ),
                            bulletStart,
                            spanEnd,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        bulletStart = -1
                    }
                }
            }
        }
    }
}