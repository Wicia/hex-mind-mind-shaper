package pl.hexmind.mindshaper.activities.flashcards

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import pl.hexmind.mindshaper.services.FlashcardsService
import pl.hexmind.mindshaper.services.dto.FlashcardDTO
import pl.hexmind.mindshaper.services.dto.FlashcardSetDTO
import javax.inject.Inject

@HiltViewModel
class FlashcardsViewModel @Inject constructor(
    private val flashcardsService: FlashcardsService
) : ViewModel() {

    // Live - a session stored from the scrim refreshes the set's card on its own
    val sets: LiveData<List<FlashcardSetDTO>> = flashcardsService.getAllSetsLive()

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

    fun updateSession(flashcards: List<FlashcardDTO>) {
        viewModelScope.launch {
            flashcardsService.updateSession(flashcards)
        }
    }
}
