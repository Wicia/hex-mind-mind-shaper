package pl.hexmind.mindshaper.activities.carousel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import pl.hexmind.mindshaper.common.SortConfig
import pl.hexmind.mindshaper.common.SortDirection
import pl.hexmind.mindshaper.common.SortProperty
import pl.hexmind.mindshaper.common.regex.HexTags
import pl.hexmind.mindshaper.common.ui.CommonIconsListItem
import pl.hexmind.mindshaper.services.DomainsService
import pl.hexmind.mindshaper.services.ThoughtsService
import pl.hexmind.mindshaper.services.dto.ThoughtDTO
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * ViewModel for managing carousel data and operations with search and sort functionality
 */
@HiltViewModel
class CarouselViewModel @Inject constructor(
    private val thoughtsService: ThoughtsService,
    private val domainsService: DomainsService,
    private val savedStateHandle: SavedStateHandle // ! To keep search + sort configs when returning from other activities to carousel
) : ViewModel() {

    // All thoughts from database
    private val allThoughts: LiveData<List<ThoughtDTO>> = thoughtsService.getAllThoughts()

    private val _searchQuery = savedStateHandle.getLiveData("search_query", HexTags())

    private val _sortConfig = savedStateHandle.getLiveData("sort_config", SortConfig())
    val sortConfig: LiveData<SortConfig> = _sortConfig

    private val _selectedDomainId = savedStateHandle.getLiveData<Int?>("selected_domain_id", null)
    val selectedDomainId: LiveData<Int?> = _selectedDomainId

    private val _domainsWithIcons = MutableLiveData<List<CommonIconsListItem>>(emptyList())
    val domainsWithIcons: LiveData<List<CommonIconsListItem>> = _domainsWithIcons

    // Combine search and sort using MediatorLiveData
    val filteredThoughts: MediatorLiveData<List<ThoughtDTO>> = MediatorLiveData<List<ThoughtDTO>>().apply {
        var currentThoughts: List<ThoughtDTO>? = null
        var currentQuery: HexTags? = null
        var currentSort: SortConfig? = null
        var currentDomainId: Int? = null

        fun update() {
            val thoughts = currentThoughts
            if (thoughts == null) {
                Timber.d("currentThoughts is null, skipping update")
                return
            }

            val query = currentQuery ?: HexTags()
            val sort = currentSort ?: SortConfig()
            val domainId = currentDomainId

            val filtered = filterThoughts(thoughts, query, domainId)
            val sorted = sortThoughts(filtered, sort)

            value = sorted
        }

        addSource(allThoughts) { thoughts ->
            currentThoughts = thoughts
            update()
        }

        addSource(_searchQuery) { query ->
            currentQuery = query
            update()
        }

        addSource(_sortConfig) { sort ->
            currentSort = sort
            update()
        }

        addSource(_selectedDomainId) { domainId ->
            currentDomainId = domainId
            update()
        }
    }

    fun updateSearchQuery(query: HexTags) {
        savedStateHandle["search_query"] = query
        _searchQuery.value = query
    }

    fun updateSortConfig(config: SortConfig) {
        savedStateHandle["sort_config"] = config
        _sortConfig.value = config
    }

    fun clearSearch() {
        savedStateHandle["search_query"] = HexTags()
        _searchQuery.value = HexTags()
    }

    fun loadDomains() {
        viewModelScope.launch {
            val domains = domainsService.getAllDomainWithIcons()
            _domainsWithIcons.value = domains
        }
    }

    fun updateSelectedDomain(domainId: Int?) {
        savedStateHandle["selected_domain_id"] = domainId
        _selectedDomainId.value = domainId
    }

    fun clearDomainFilter() {
        savedStateHandle["selected_domain_id"] = null
        _selectedDomainId.value = null
    }

    private fun filterThoughts(thoughts: List<ThoughtDTO>, query: HexTags, domainId: Int?): List<ThoughtDTO> {
        var filtered = thoughts

        // Filter by domain if selected
        if (domainId != null) {
            filtered = filtered.filter { it.domainId == domainId }
        }

        // Filter by search query
        if (!query.areCriteriaEmpty()) {
            filtered = filtered.filter { thought ->
                matchesCriteria(thought.thread, query.thread) &&
                matchesCriteria(thought.soulMate, query.soulMate) &&
                matchesCriteria(thought.project, query.project)
            }
        }

        return filtered
    }

    private fun matchesCriteria(fieldValue: String?, searchQuery: String?): Boolean {
        if (searchQuery.isNullOrBlank()) return true
        if (fieldValue.isNullOrBlank()) return false

        // case insensitive match (contains)
        return fieldValue.contains(searchQuery, ignoreCase = true)
    }

    private fun sortThoughts(thoughts: List<ThoughtDTO>, config: SortConfig): List<ThoughtDTO> {
        val comparator: Comparator<ThoughtDTO> = when (config.property) {
            SortProperty.CREATED_AT -> compareBy(nullsLast()) { it.createdAt }
            SortProperty.THREAD -> compareBy(nullsLast()) { it.thread?.lowercase() }
            SortProperty.SOUL_MATE -> compareBy(nullsLast()) { it.soulMate?.lowercase() }
            SortProperty.PROJECT -> compareBy(nullsLast()) { it.project?.lowercase() }
            SortProperty.VALUE -> compareBy(nullsLast()) { it.value }
        }

        val sorted = when (config.direction) {
            SortDirection.ASCENDING -> thoughts.sortedWith(comparator)
            SortDirection.DESCENDING -> thoughts.sortedWith(comparator.reversed())
        }

        return sorted
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

    // TODO: there is same function in DetailsViewModel :)
    fun loadAudioForPlayback(
        thoughtId: Int,
        onAudioReady: (File) -> Unit,
        onError: () -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val audioData = thoughtsService.getAudioData(thoughtId)

                if (audioData == null || audioData.isEmpty()) {
                    Timber.w("No audio data for thought $thoughtId")
                    onError()
                    return@launch
                }

                // Creating temp file for playing
                val tempFile = File.createTempFile("carousel_playback_", ".m4a")
                tempFile.writeBytes(audioData)
                onAudioReady(tempFile)

            } catch (e: Exception) {
                Timber.e(e, "Error loading audio for thought $thoughtId")
                onError()
            }
        }
    }
}