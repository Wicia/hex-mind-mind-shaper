package pl.hexmind.mindshaper.common.ui

object HtmlConverter {

    private const val BULLET_POINT_CHAR = "X "

    fun convertToHtml(text: String): String {
        if (text.isEmpty()) return ""

        val lines = text.split("\n")
        val result = StringBuilder()
        var inList = false
        var inParagraph = false
        var hasContent = false
        var previousLineWasEmpty = false

        for (line in lines) {
            val trimmed = line.trim()

            if (trimmed.startsWith(BULLET_POINT_CHAR, ignoreCase = true)) {
                if (hasContent && !inList) {
                    result.append("<br>")
                }
                if (inParagraph) {  // close paragraph before list
                    result.append("</p>")
                    inParagraph = false
                }

                if (!inList) {
                    result.append("<ul>")
                    inList = true
                }
                result.append("<li>")
                    .append(trimmed.substring(2).trim())
                    .append("</li>")
                previousLineWasEmpty = false
                hasContent = true
            } else if (trimmed.isEmpty()) {
                if (inList) {
                    result.append("</ul>")
                    inList = false
                } else if (inParagraph) {  // close only when paragraph was open
                    result.append("</p>")
                    inParagraph = false
                    hasContent = false
                }
                previousLineWasEmpty = true
            } else {
                if (inList) {
                    result.append("</ul>")
                    inList = false
                }

                if (previousLineWasEmpty) {
                    result.append("<p>").append(trimmed)
                    inParagraph = true  // mark - we are in paragraph
                } else if (hasContent) {
                    result.append("<br>").append(trimmed)
                } else {
                    result.append(trimmed)
                }

                hasContent = true
                previousLineWasEmpty = false
            }
        }

        // Close opened elements at the end
        if (inList) {
            result.append("</ul>")
        } else if (inParagraph) {  // close only when paragraph was opened
            result.append("</p>")
        }

        return result.toString()
    }
}