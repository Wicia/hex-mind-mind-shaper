package pl.hexmind.fastnote.data.models

enum class AreaIdentifier (val value: Int) {

    NOT_SET(-1),
    AREA_1(0),
    AREA_2(1),
    AREA_3(2),
    AREA_4(3),
    AREA_5(4),
    AREA_6(5),
    AREA_7(6),
    AREA_8(7),
    AREA_9(8);

    companion object {
        fun fromInt(value: Int): AreaIdentifier {
            return entries.find { it.value == value }
                ?: throw IllegalArgumentException("AreaIdentifier must be between 0-8, got: $value")
        }

        // Or with default fallback
        fun fromIntOrDefault(value: Int, default: AreaIdentifier = NOT_SET): AreaIdentifier {
            return entries.find { it.value == value } ?: default
        }
    }
}