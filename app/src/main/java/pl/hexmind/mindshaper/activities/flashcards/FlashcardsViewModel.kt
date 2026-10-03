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
import javax.inject.Inject

@HiltViewModel
class FlashcardsViewModel @Inject constructor(
    private val flashcardsService: FlashcardsService
) : ViewModel() {

    // Live - a rating stored from the scrim refreshes the counts on its own
    val sets: LiveData<List<FlashcardSetDTO>> = flashcardsService.getAllSetsLive()

    /** @param setId null = one session over all the sets */
    fun planReview(sets: List<FlashcardSetDTO>, setId: Int? = null): FlashcardsReview.Plan =
        flashcardsService.planReview(sets, setId)

    fun statsOf(set: FlashcardSetDTO): FlashcardsReview.SetStats =
        flashcardsService.statsOf(set)

    // ! Stored one by one - a quick series could outrun the refresh of [sets]
    fun addFlashcard(setId: Int, flashcard: FlashcardDTO) {
        viewModelScope.launch {
            flashcardsService.addFlashcard(setId, flashcard)
        }
    }

    /** Empty set - the flashcards are added right after, in a series */
    fun addSet(name: String, onCreated: (setId: Int) -> Unit) {
        viewModelScope.launch {
            onCreated(flashcardsService.addSet(name, emptyList()))
        }
    }

    fun deleteSet(setId: Int) {
        viewModelScope.launch {
            flashcardsService.deleteSet(setId)
        }
    }

    fun rate(flashcard: FlashcardDTO, rating: FlashcardRating) {
        viewModelScope.launch {
            flashcardsService.rate(flashcard, rating)
        }
    }
}
