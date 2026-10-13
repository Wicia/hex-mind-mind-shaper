package pl.hexmind.mindshaper.services.dto

/**
 * All the flashcards: the sets + the loose ones (no set).
 * The review session, its limits and the reminder go over all of it.
 */
data class FlashcardsLibraryDTO(
    val sets: List<FlashcardSetDTO>,
    val loose: List<FlashcardDTO> = emptyList()
) {
    val isEmpty: Boolean get() = loose.isEmpty() && sets.all { set -> set.flashcards.isEmpty() }
}
