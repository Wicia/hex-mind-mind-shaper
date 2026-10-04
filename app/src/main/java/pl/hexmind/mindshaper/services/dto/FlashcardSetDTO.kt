package pl.hexmind.mindshaper.services.dto

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import pl.hexmind.mindshaper.common.intent.InstantParceler
import java.time.Instant

/**
 * A named set of flashcards. id == null - not saved yet (the set is just being created)
 */
@Parcelize
@TypeParceler<Instant?, InstantParceler>
data class FlashcardSetDTO(
    val id: Int? = null,
    val name: String,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val flashcards: List<FlashcardDTO> = emptyList()
) : Parcelable
