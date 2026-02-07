package pl.hexmind.mindshaper.activities.stream

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
import pl.hexmind.mindshaper.common.ui.CommonIconsListItem
import pl.hexmind.mindshaper.services.DomainsService
import pl.hexmind.mindshaper.services.ThoughtsService
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
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    // All thoughts from database
    private val allThoughts: LiveData<List<ThoughtDTO>> = thoughtsService.getAllThoughts()

    // Default sort: newest first (CREATED_AT DESCENDING)
    private val _sortConfig = savedStateHandle.getLiveData(
        "sort_config",
        SortConfig(property = SortProperty.CREATED_AT, direction = SortDirection.DESCENDING)
    )
    val sortConfig: LiveData<SortConfig> = _sortConfig

    private val _selectedDomainId = savedStateHandle.getLiveData<Int?>("selected_domain_id", null)
    val selectedDomainId: LiveData<Int?> = _selectedDomainId

    private val _domainsWithIcons = MutableLiveData<List<CommonIconsListItem>>(emptyList())
    val domainsWithIcons: LiveData<List<CommonIconsListItem>> = _domainsWithIcons

    // Combine filter and sort using MediatorLiveData
    val filteredThoughts: MediatorLiveData<List<ThoughtDTO>> = MediatorLiveData<List<ThoughtDTO>>().apply {
        var currentThoughts: List<ThoughtDTO>? = null
        var currentSort: SortConfig? = null
        var currentDomainId: Int? = null

        fun update() {
            val thoughts = currentThoughts
            if (thoughts == null) {
                Timber.d("currentThoughts is null, skipping update")
                return
            }

            val sort = currentSort ?: SortConfig(
                property = SortProperty.CREATED_AT,
                direction = SortDirection.DESCENDING
            )
            val domainId = currentDomainId

            val filtered = filterThoughts(thoughts, domainId)
            val sorted = sortThoughts(filtered, sort)

            value = sorted
        }

        addSource(allThoughts) { thoughts ->
            currentThoughts = thoughts
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

    fun clearDomainFilter() {
        savedStateHandle["selected_domain_id"] = null
        _selectedDomainId.value = null
    }

    private fun filterThoughts(thoughts: List<ThoughtDTO>, domainId: Int?): List<ThoughtDTO> {
        // Filter by domain if selected
        return if (domainId != null) {
            thoughts.filter { it.domainId == domainId }
        } else {
            thoughts
        }
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