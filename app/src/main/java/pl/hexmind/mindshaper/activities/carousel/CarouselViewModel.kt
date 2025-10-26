package pl.hexmind.mindshaper.activities.carousel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
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

    // Search query LiveData for real-time filtering
    private val _searchQuery = MutableLiveData<String>("")
    val searchQuery: LiveData<String> = _searchQuery

    // Using MediatorLiveData for reactive filtering
    val filteredThoughts: LiveData<List<ThoughtDTO>> = MediatorLiveData<List<ThoughtDTO>>().apply {
        addSource(allThoughts) { thoughts ->
            value = filterThoughts(thoughts, _searchQuery.value ?: "")
        }
        addSource(_searchQuery) { query ->
            value = filterThoughts(allThoughts.value ?: emptyList(), query)
        }
    }

    /**
     * Update search query for real-time filtering
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        Timber.d("Search query updated: $query")
    }

    /**
     * Clear search query and show all thoughts
     */
    fun clearSearch() {
        _searchQuery.value = ""
        Timber.d("Search cleared")
    }

    /**
     * Filter thoughts by thread with wildcard support (* -> .*)
     */
    private fun filterThoughts(thoughts: List<ThoughtDTO>, query: String): List<ThoughtDTO> {
        // Empty query = show all thoughts
        if (query.isBlank()) {
            return thoughts
        }

        return try {
            val regexPattern = convertWildcardsToRegex(query)
            val regex = Regex(regexPattern, RegexOption.IGNORE_CASE)

            thoughts.filter { thought ->
                thought.thread?.let { thread ->
                    regex.containsMatchIn(thread)
                } ?: false
            }
        } catch (e: Exception) {
            // If regex fails (invalid pattern), fallback to simple contains
            Timber.w(e, "Regex pattern failed, using simple contains")
            thoughts.filter { thought ->
                thought.thread?.contains(query, ignoreCase = true) ?: false
            }
        }
    }

    /**
     * Convert user wildcards (*) to regex pattern with escaped special characters
     */
    private fun convertWildcardsToRegex(query: String): String {
        // Split by asterisks to preserve them as wildcards
        val parts = query.split("*")

        // Escape regex special characters in each part (except asterisks)
        val escapedParts = parts.map { part ->
            Regex.escape(part)
        }

        // Join with .* (regex wildcard)
        return escapedParts.joinToString(".*")
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