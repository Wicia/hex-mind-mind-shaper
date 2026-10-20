package pl.hexmind.mindshaper.activities.flashcards

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import pl.hexmind.mindshaper.services.FlashcardsReview
import pl.hexmind.mindshaper.services.FlashcardsService
import pl.hexmind.mindshaper.services.dto.FlashcardDTO
import pl.hexmind.mindshaper.services.dto.FlashcardRating
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class FlashcardsSessionViewModel @Inject constructor(
    private val flashcardsService: FlashcardsService,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // null = one session over all the flashcards (sets + loose)
    private val setId: Int? = savedStateHandle.get<Int>(FlashcardsSessionActivity.EXTRA_SET_ID)

    val isSetSession: Boolean
        get() = setId != null

    private val _plan = MutableLiveData<FlashcardsReview.Plan>()

    /** Planned once per session - kept over the activity's restarts */
    val plan: LiveData<FlashcardsReview.Plan> = _plan

    private val _tomorrowCount = MutableLiveData<Int>()

    /** Flashcards waiting tomorrow in the session's scope - counted at the end of the session */
    val tomorrowCount: LiveData<Int> = _tomorrowCount

    // ! Fair lock: writes land in the order of ratings / undos, the tomorrow count only after all of them
    private val writes = Mutex()
    private var countJob: Job? = null

    init {
        viewModelScope.launch {
            _plan.value = flashcardsService.planReview(flashcardsService.getLibrary(), setId)
        }
    }

    // ! NonCancellable: "Przerwij" right after the last rating closes the screen (and this scope) - the rating must still land
    fun rate(flashcard: FlashcardDTO, rating: FlashcardRating) {
        viewModelScope.launch {
            withContext(NonCancellable) {
                writes.withLock { flashcardsService.rate(flashcard, rating) }
            }
        }
    }

    fun restoreProgress(flashcard: FlashcardDTO) {
        viewModelScope.launch {
            withContext(NonCancellable) {
                writes.withLock { flashcardsService.restoreProgress(flashcard) }
            }
        }
    }

    /** A count still waiting from before an undo is dropped - only the last end of the session counts */
    fun countTomorrow() {
        countJob?.cancel()
        countJob = viewModelScope.launch {
            val count = writes.withLock {
                flashcardsService.planReview(flashcardsService.getLibrary(), setId, LocalDate.now().plusDays(1)).queue.size
            }
            _tomorrowCount.value = count
        }
    }
}
