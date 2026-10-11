package pl.hexmind.mindshaper.activities.flashcards

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import pl.hexmind.mindshaper.services.FlashcardsReview
import pl.hexmind.mindshaper.services.FlashcardsService
import pl.hexmind.mindshaper.services.dto.FlashcardDTO
import pl.hexmind.mindshaper.services.dto.FlashcardRating
import pl.hexmind.mindshaper.services.dto.FlashcardSetDTO
import pl.hexmind.mindshaper.services.dto.FlashcardsLibraryDTO
import javax.inject.Inject

@HiltViewModel
class FlashcardsViewModel @Inject constructor(
    private val flashcardsService: FlashcardsService
) : ViewModel() {

    // Live - a rating stored from the scrim refreshes the counts on its own
    val library: LiveData<FlashcardsLibraryDTO> = flashcardsService.getLibraryLive()

    /** @param setId null = one session over all the flashcards (sets + loose) */
    fun planReview(library: FlashcardsLibraryDTO, setId: Int? = null): FlashcardsReview.Plan =
        flashcardsService.planReview(library, setId)

    fun statsOf(set: FlashcardSetDTO): FlashcardsReview.SetStats =
        flashcardsService.statsOf(set)

    /**
     * @param setId null = a loose flashcard
     * ! Stored one by one - a quick series could outrun the refresh of [library]
     */
    fun addFlashcard(setId: Int?, flashcard: FlashcardDTO) {
        viewModelScope.launch {
            flashcardsService.addFlashcard(setId, flashcard)
        }
    }

    fun updateLooseFlashcard(flashcard: FlashcardDTO) {
        viewModelScope.launch {
            flashcardsService.updateFlashcard(null, flashcard)
        }
    }

    fun deleteLooseFlashcard(flashcard: FlashcardDTO) {
        val flashcardId = flashcard.id ?: return
        viewModelScope.launch {
            flashcardsService.deleteFlashcard(null, flashcardId)
        }
    }

    /** Empty set - the flashcards are added right after, in a series */
    fun addSet(name: String, onCreated: (setId: Int) -> Unit) {
        viewModelScope.launch {
            onCreated(flashcardsService.addSet(name, emptyList()))
        }
    }

    /** @param keepFlashcards true = the set's flashcards become loose */
    fun deleteSet(setId: Int, keepFlashcards: Boolean) {
        viewModelScope.launch {
            flashcardsService.deleteSet(setId, keepFlashcards)
        }
    }

    fun rate(flashcard: FlashcardDTO, rating: FlashcardRating) {
        viewModelScope.launch {
            flashcardsService.rate(flashcard, rating)
        }
    }
}
