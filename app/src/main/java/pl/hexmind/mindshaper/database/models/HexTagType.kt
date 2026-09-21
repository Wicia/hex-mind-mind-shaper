package pl.hexmind.mindshaper.database.models

/**
 * Kinds of hex tag a thought can carry. Stored as the enum name in HEX_TAGS.type.
 *
 * A thought's subject stays a plain column - it is a headline of that one thought,
 * not a label shared between thoughts.
 */
enum class HexTagType {
    PERSON,
    PROJECT
}
