package pl.hexmind.mindshaper.common.regex

fun String.removeWordsConnectors(): String {
    val connectorsPattern = """(?i)\s+(?:a|i|w|z|o|u|na|do|od|po|ze|we|ku|by)\s+"""

    return this
        .replace("""[,.\-/]""".toRegex(), "")
        .replace(connectorsPattern.toRegex(), " ")
        .replace("""\s+""".toRegex(), " ")
        .trim()
}

fun String.convertToWords(): List<String> {
    return this
        .removeWordsConnectors()
        .split("\\s+".toRegex())
}

fun String.getWordsCount() : Int {
    return this.trim().split("\\s+".toRegex()).size
}