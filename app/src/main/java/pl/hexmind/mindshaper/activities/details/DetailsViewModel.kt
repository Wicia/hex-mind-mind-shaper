package pl.hexmind.mindshaper.activities.details

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import pl.hexmind.mindshaper.common.ui.CommonIconsListItem
import pl.hexmind.mindshaper.services.DomainsService
import pl.hexmind.mindshaper.services.ThoughtsService
import pl.hexmind.mindshaper.services.dto.ThoughtDTO
import javax.inject.Inject

@HiltViewModel
class DetailsViewModel @Inject constructor(
    private val thoughtsService: ThoughtsService,
    private val domainsService: DomainsService
) : ViewModel() {

    private val _thoughtId = MutableLiveData<Int>()

    // ! switchMap to observe and react when changes in ID happens
    val thoughtDetails: LiveData<ThoughtDTO?> = _thoughtId.switchMap { id ->
        thoughtsService.getThoughtByIdLive(id)
    }

    private val _domainsWithIcons = MutableLiveData<List<CommonIconsListItem>>(emptyList())
    val domainsWithIcons: LiveData<List<CommonIconsListItem>> = _domainsWithIcons

    fun loadThought(id: Int) {
        _thoughtId.value = id
    }

    fun loadDomains() {
        viewModelScope.launch {
            val domains = domainsService.getAllDomainWithIcons()
            _domainsWithIcons.value = domains
        }
    }

    fun updateDomain(domainId: Int) {
        viewModelScope.launch {
            thoughtDetails.value?.let { thought ->
                thought.domainId = domainId
                thoughtsService.updateThought(thought)
            }
        }
    }

    fun updateThread(thread: String) {
        viewModelScope.launch {
            thoughtDetails.value?.let { thought ->
                thought.thread = thread
                thoughtsService.updateThought(thought)
            }
        }
    }

    fun updateRichText(richText: String) {
        viewModelScope.launch {
            thoughtDetails.value?.let { thought ->
                thought.richText = richText
                thoughtsService.updateThought(thought)
            }
        }
    }

    fun updateSoulName(soulName: String) {
        viewModelScope.launch {
            thoughtDetails.value?.let { thought ->
                thought.soulName = soulName
                thoughtsService.updateThought(thought)
            }
        }
    }

    fun updateProject(project: String) {
        viewModelScope.launch {
            thoughtDetails.value?.let { thought ->
                thought.project = project
                thoughtsService.updateThought(thought)
            }
        }
    }

    fun saveThought() {
        viewModelScope.launch {
            thoughtDetails.value?.let { thought ->
                thoughtsService.updateThought(thought)
            }
        }
    }

    suspend fun getIconIdForDomain(domainId: Int): Int? {
        return domainsService.getIconIdForDomain(domainId)
    }
}