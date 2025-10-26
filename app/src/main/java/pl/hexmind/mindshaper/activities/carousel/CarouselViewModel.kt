package pl.hexmind.mindshaper.activities.carousel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import pl.hexmind.mindshaper.services.ThoughtsService
import pl.hexmind.mindshaper.services.dto.ThoughtDTO
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for managing carousel data and operations
 */
@HiltViewModel
class CarouselViewModel @Inject constructor(
    private val thoughtsService: ThoughtsService
) : ViewModel() {

    // Reactive list of all thoughts - automatically updates UI when database changes
    val allThoughts: LiveData<List<ThoughtDTO>> = thoughtsService.getAllThoughts()

    fun deleteThought(thought: ThoughtDTO) {
        viewModelScope.launch {
            thought.id?.let { thoughtId ->
                Timber.d("Deleting thought: $thoughtId")
                thoughtsService.deleteThoughtById(thoughtId)
            } ?: run {
                Timber.w("Cannot delete thought without ID")
            }
        }
    }
}