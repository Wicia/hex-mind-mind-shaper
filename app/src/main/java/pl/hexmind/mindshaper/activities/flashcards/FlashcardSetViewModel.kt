package pl.hexmind.mindshaper.activities.flashcards

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
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
class FlashcardSetViewModel @Inject constructor(
    private val flashcardsService: FlashcardsService,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Key matches FlashcardSetActivity.EXTRA_SET_ID ("setId")
    val setId: Int = savedStateHandle.get<Int>("setId") ?: 0

    // null = the set is gone (deleted)
    val set: LiveData<FlashcardSetDTO?> = flashcardsService.getSetByIdLive(setId)

    // The set's own session follows the limits counted over all the flashcards (new per day)
    val library: LiveData<FlashcardsLibraryDTO> = flashcardsService.getLibraryLive()

    fun planReview(library: FlashcardsLibraryDTO): FlashcardsReview.Plan =
        flashcardsService.planReview(library, setId)

    fun updateSet(name: String, flashcards: List<FlashcardDTO>) {
        viewModelScope.launch {
            flashcardsService.updateSet(setId, name, flashcards)
        }
    }

    // ! Stored one by one, not by swapping the whole list - a quick series could outrun the refresh of [set]
    fun addFlashcard(flashcard: FlashcardDTO) {
        viewModelScope.launch {
            flashcardsService.addFlashcard(setId, flashcard)
        }
    }

    /** One transaction - the imported flashcards go to the end of the set in the order of the text */
    fun importFlashcards(flashcards: List<FlashcardDTO>) {
        viewModelScope.launch {
            flashcardsService.addFlashcards(setId, flashcards)
        }
    }

    fun updateFlashcard(flashcard: FlashcardDTO) {
        viewModelScope.launch {
            flashcardsService.updateFlashcard(setId, flashcard)
        }
    }

    fun deleteFlashcard(flashcard: FlashcardDTO) {
        val flashcardId = flashcard.id ?: return
        viewModelScope.launch {
            flashcardsService.deleteFlashcard(setId, flashcardId)
        }
    }

    /** @param toSetId null = to the loose flashcards */
    fun moveFlashcard(flashcard: FlashcardDTO, toSetId: Int?) {
        val flashcardId = flashcard.id ?: return
        viewModelScope.launch {
            flashcardsService.moveFlashcard(flashcardId, fromSetId = setId, toSetId = toSetId)
        }
    }

    fun rate(flashcard: FlashcardDTO, rating: FlashcardRating) {
        viewModelScope.launch {
            flashcardsService.rate(flashcard, rating)
        }
    }
}
