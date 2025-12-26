package pl.hexmind.mindshaper.common

/* Saved system name/id for value specification
*   6 - system with min value = 1 and max value = 6
*  10 - system with min value = 1 and max value = 10
 */
enum class ThoughtValueSystem (val minValue : Int, val maxValue : Int) {
    STANDARD_6(1, 6),
    STANDARD_10(1, 10)
}