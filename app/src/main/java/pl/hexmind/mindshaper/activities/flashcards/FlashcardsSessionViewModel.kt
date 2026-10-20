package pl.hexmind.mindshaper.activities.flashcards

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.hexmind.mindshaper.services.FlashcardsReview
import pl.hexmind.mindshaper.services.FlashcardsService
import pl.hexmind.mindshaper.services.dto.FlashcardDTO
import pl.hexmind.mindshaper.services.dto.FlashcardRating
import javax.inject.Inject

@HiltViewModel
class FlashcardsSessionViewModel @Inject constructor(
    private val flashcardsService: FlashcardsService,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // null = one session over all the flashcards (sets + loose)
    private val setId: Int? = savedStateHandle.get<Int>(FlashcardsSessionActivity.EXTRA_SET_ID)

    private val _plan = MutableLiveData<FlashcardsReview.Plan>()

    /** Planned once per session - kept over the activity's restarts */
    val plan: LiveData<FlashcardsReview.Plan> = _plan

    init {
        viewModelScope.launch {
            _plan.value = flashcardsService.planReview(flashcardsService.getLibrary(), setId)
        }
    }

    // ! NonCancellable: "Przerwij" right after the last rating closes the screen (and this scope) - the rating must still land
    fun rate(flashcard: FlashcardDTO, rating: FlashcardRating) {
        viewModelScope.launch {
            withContext(NonCancellable) {
                flashcardsService.rate(flashcard, rating)
            }
        }
    }

    fun restoreProgress(flashcard: FlashcardDTO) {
        viewModelScope.launch {
            withContext(NonCancellable) {
                flashcardsService.restoreProgress(flashcard)
            }
        }
    }
}
