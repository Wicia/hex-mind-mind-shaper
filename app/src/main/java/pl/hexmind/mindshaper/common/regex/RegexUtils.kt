package pl.hexmind.mindshaper.common.regex

fun String.cleanText(input: String): String {
    val connectorsPattern = """(?i)\s+(?:a|i|w|z|o|u|na|do|od|po|ze|we|ku|by)\s+"""

    return input
        .replace("""[,.\-/]""".toRegex(), "")
        .replace(connectorsPattern.toRegex(), " ")
        .replace("""\s+""".toRegex(), " ")
        .trim()
}