package pl.hexmind.mindshaper.activities.flashcards

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import pl.hexmind.mindshaper.services.FlashcardsService
import pl.hexmind.mindshaper.services.dto.FlashcardDTO
import pl.hexmind.mindshaper.services.dto.FlashcardSetDTO
import javax.inject.Inject

@HiltViewModel
class FlashcardSetViewModel @Inject constructor(
    private val flashcardsService: FlashcardsService,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Key matches FlashcardSetActivity.EXTRA_SET_ID ("setId")
    private val setId: Int = savedStateHandle.get<Int>("setId") ?: 0

    // null = the set is gone (deleted)
    val set: LiveData<FlashcardSetDTO?> = flashcardsService.getSetByIdLive(setId)

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

    fun updateSession(flashcards: List<FlashcardDTO>) {
        viewModelScope.launch {
            flashcardsService.updateSession(flashcards)
        }
    }

    fun deleteSet() {
        viewModelScope.launch {
            flashcardsService.deleteSet(setId)
        }
    }
}
