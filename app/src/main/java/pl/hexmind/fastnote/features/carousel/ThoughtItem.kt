package pl.hexmind.fastnote.features.carousel

import java.util.UUID

/**
 * Data class representing a single thought item
 */
// TODO: Przeniesc do /data?
data class ThoughtItem(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val tags : String,
    val timestamp: Long = System.currentTimeMillis()
)