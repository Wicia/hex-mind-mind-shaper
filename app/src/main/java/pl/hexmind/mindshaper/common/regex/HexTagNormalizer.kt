package pl.hexmind.mindshaper.common.regex

import java.text.Normalizer
import java.util.Locale

/**
 * Folds a tag name into a comparison key: "żółw" and "zolw" share one.
 *
 * Used for suggestions and for warning that a similar tag already exists - never for merging,
 * since variants are deliberately kept as separate tags.
 */
object HexTagNormalizer {

    // ! Normalizer leaves these alone - they carry a stroke, not a combining mark
    private val STROKED_LETTERS = mapOf('ł' to 'l', 'Ł' to 'L')

    private val COMBINING_MARKS = Regex("\\p{Mn}+")

    fun normalize(name: String): String {
        val withoutStrokes = name.map { character -> STROKED_LETTERS[character] ?: character }
            .joinToString("")

        return Normalizer.normalize(withoutStrokes, Normalizer.Form.NFD)
            .replace(COMBINING_MARKS, "")
            .lowercase(Locale.ROOT)
            .trim()
    }
}
