package pl.hexmind.mindshaper.activities.stream

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import pl.hexmind.mindshaper.common.ui.views.lists.SortConfig
import pl.hexmind.mindshaper.common.ui.views.lists.SortDirection
import pl.hexmind.mindshaper.common.ui.views.lists.SortProperty
import pl.hexmind.mindshaper.common.regex.HexTags
import pl.hexmind.mindshaper.common.ui.views.lists.CommonIconsListItem
import pl.hexmind.mindshaper.services.DomainsService
import pl.hexmind.mindshaper.services.ThoughtsService
import pl.hexmind.mindshaper.common.dormant.ThoughtState
import pl.hexmind.mindshaper.services.ThoughtStatusService
import pl.hexmind.mindshaper.services.dto.ThoughtDTO
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * ViewModel for managing stream (vertical feed) data with sort and filter functionality
 */
@HiltViewModel
class StreamViewModel @Inject constructor(
    private val thoughtsService: ThoughtsService,
    private val domainsService: DomainsService,
    private val savedStateHandle: SavedStateHandle,
    private val thoughtStatusService: ThoughtStatusService
) : ViewModel() {

    // All thoughts from database
    private val allThoughts: LiveData<List<ThoughtDTO>> = thoughtsService.getAllThoughts()

    private val _searchQuery = savedStateHandle.getLiveData("search_query", HexTags())

    // Default sort: recently updated first
    private val _sortConfig = savedStateHandle.getLiveData(
        "sort_config",
        SortConfig(property = SortProperty.UPDATED_AT, direction = SortDirection.DESCENDING)
    )
    val sortConfig: LiveData<SortConfig> = _sortConfig

    private val _selectedDomainId = savedStateHandle.getLiveData<Int?>("selected_domain_id", null)
    val selectedDomainId: LiveData<Int?> = _selectedDomainId

    private val _showActive  = savedStateHandle.getLiveData("show_active", true)
    val showActive: LiveData<Boolean> = _showActive

    private val _showDormant = savedStateHandle.getLiveData("show_dormant", false)
    val showDormant: LiveData<Boolean> = _showDormant

    private val _domainsWithIcons = MutableLiveData<List<CommonIconsListItem>>(emptyList())
    val domainsWithIcons: LiveData<List<CommonIconsListItem>> = _domainsWithIcons

    // counted over ALL thoughts, not the filtered list (shows how much is there to reveal)
    // ! plain functions - a mapped LiveData stays cold until observed, so .value would always be 0
    fun countActiveThoughts(): Int =
        allThoughts.value.orEmpty().count { thought -> thoughtStatusService.computeState(thought) != ThoughtState.DORMANT }

    fun countDormantThoughts(): Int =
        allThoughts.value.orEmpty().count { thought -> thoughtStatusService.computeState(thought) == ThoughtState.DORMANT }

    // Combine filter and sort using MediatorLiveData
    val filteredThoughts: MediatorLiveData<List<ThoughtDTO>> = MediatorLiveData<List<ThoughtDTO>>().apply {
        var currentThoughts: List<ThoughtDTO>? = null
        var currentQuery: HexTags? = null
        var currentSort: SortConfig? = null
        var currentDomainId: Int? = null
        var currentShowActive: Boolean  = true
        var currentShowDormant: Boolean = false

        fun update() {
            val thoughts = currentThoughts
            if (thoughts == null) {
                Timber.d("currentThoughts is null, skipping update")
                return
            }

            val query = currentQuery ?: HexTags()
            val sort = currentSort ?: SortConfig(
                property = SortProperty.UPDATED_AT,
                direction = SortDirection.DESCENDING
            )
            val domainId = currentDomainId

            val filtered = filterThoughts(thoughts, query, domainId, currentShowActive, currentShowDormant)
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

        addSource(_showActive) { show ->
            currentShowActive = show
            update()
        }

        addSource(_showDormant) { show ->
            currentShowDormant = show
            update()
        }
    }

    fun updateSearchQuery(query: HexTags) {
        savedStateHandle["search_query"] = query
        _searchQuery.value = query
    }

    fun clearSearch() {
        savedStateHandle["search_query"] = HexTags()
        _searchQuery.value = HexTags()
    }

    fun updateSortConfig(config: SortConfig) {
        savedStateHandle["sort_config"] = config
        _sortConfig.value = config
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

    fun updateShowActive(show: Boolean) {
        savedStateHandle["show_active"] = show
        _showActive.value = show
    }

    fun updateShowDormant(show: Boolean) {
        savedStateHandle["show_dormant"] = show
        _showDormant.value = show
    }

    private fun filterThoughts(thoughts: List<ThoughtDTO>, query: HexTags, domainId: Int?, showActive: Boolean, showDormant: Boolean): List<ThoughtDTO> {
        var filtered = thoughts

        // Filter by state — keep thought if its state category is checked
        filtered = filtered.filter { thought ->
            when (thoughtStatusService.computeState(thought)) {
                ThoughtState.DORMANT  -> showDormant
                else                  -> showActive // ACTIVE, WARNING, LOCKED
            }
        }

        if (domainId != null) {
            filtered = filtered.filter { it.domainId == domainId }
        }

        if (!query.areCriteriaEmpty()) {
            filtered = filtered.filter { thought ->
                matchesCriteria(thought.subject, query.subject) &&
                        matchesCriteria(thought.soulMate, query.soulMate) &&
                        matchesCriteria(thought.project, query.project)
            }
        }

        return filtered
    }

    private fun matchesCriteria(fieldValue: String?, searchQuery: String?): Boolean {
        if (searchQuery.isNullOrBlank()) return true
        if (fieldValue.isNullOrBlank()) return false
        return fieldValue.contains(searchQuery, ignoreCase = true)
    }

    private fun sortThoughts(thoughts: List<ThoughtDTO>, config: SortConfig): List<ThoughtDTO> {
        val comparator: Comparator<ThoughtDTO> = when (config.property) {
            SortProperty.CREATED_AT -> compareBy(nullsLast()) { it.createdAt }
            SortProperty.UPDATED_AT -> compareBy(nullsLast()) { it.updatedAt }
            SortProperty.SUBJECT -> compareBy(nullsLast()) { it.subject?.lowercase() }
            SortProperty.SOUL_MATE -> compareBy(nullsLast()) { it.soulMate?.lowercase() }
            SortProperty.PROJECT -> compareBy(nullsLast()) { it.project?.lowercase() }
            SortProperty.VALUE -> compareBy(nullsLast()) { it.value }
        }

        return when (config.direction) {
            SortDirection.ASCENDING -> thoughts.sortedWith(comparator)
            SortDirection.DESCENDING -> thoughts.sortedWith(comparator.reversed())
        }
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
                val tempFile = File.createTempFile("stream_playback_", ".m4a")
                tempFile.writeBytes(audioData)
                onAudioReady(tempFile)

            } catch (e: Exception) {
                Timber.e(e, "Error loading audio for thought $thoughtId")
                onError()
            }
        }
    }

    fun loadPhotoForDisplay(thoughtId: Int, onPhotoReady: (ByteArray) -> Unit) {
        viewModelScope.launch {
            val photoData = thoughtsService.getPhotoData(thoughtId)
            if (photoData != null && photoData.isNotEmpty()) {
                onPhotoReady(photoData)
            }
        }
    }
}