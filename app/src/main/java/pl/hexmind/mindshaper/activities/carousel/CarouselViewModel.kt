package pl.hexmind.mindshaper.activities.carousel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import pl.hexmind.mindshaper.common.regex.HexTags
import pl.hexmind.mindshaper.services.ThoughtsService
import pl.hexmind.mindshaper.services.dto.ThoughtDTO
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for managing carousel data and operations with search functionality
 */
@HiltViewModel
class CarouselViewModel @Inject constructor(
    private val thoughtsService: ThoughtsService
) : ViewModel() {

    // All thoughts from database
    private val allThoughts: LiveData<List<ThoughtDTO>> = thoughtsService.getAllThoughts()

    private val _searchQuery = MutableLiveData<HexTags>()
    val searchQuery: LiveData<HexTags> = _searchQuery  // ! Leave it - maybe UI needs it

    val filteredThoughts: LiveData<List<ThoughtDTO>> = _searchQuery.switchMap { query ->
        allThoughts.map { thoughts -> filterThoughts(thoughts, query) }
    }

    /**
     * Update search query for real-time filtering
     */
    fun updateSearchQuery(query: HexTags) {
        _searchQuery.value = query
        Timber.d("Search query updated: $query")
    }

    /**
     * Clear search query and show all thoughts
     */
    fun clearSearch() {
        _searchQuery.value = HexTags()
        Timber.d("Search cleared")
    }

    private fun filterThoughts(thoughts: List<ThoughtDTO>, query: HexTags): List<ThoughtDTO> {
        if (query.areCriteriaEmpty()) return thoughts

        return thoughts.filter { thought ->
            matchesCriteria(thought.thread, query.thread) &&
            matchesCriteria(thought.soulMate, query.soulMate) &&
            matchesCriteria(thought.project, query.project)
        }
    }

    private fun matchesCriteria(fieldValue: String?, searchQuery: String?): Boolean {
        if (searchQuery.isNullOrBlank()) return true
        if (fieldValue.isNullOrBlank()) return false

        // case insensitive match (contains)
        return fieldValue.contains(searchQuery, ignoreCase = true)
    }

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