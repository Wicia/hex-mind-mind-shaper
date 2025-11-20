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
import pl.hexmind.mindshaper.services.validators.ThoughtValidator
import javax.inject.Inject

@HiltViewModel
class DetailsViewModel @Inject constructor(
    private val thoughtsService: ThoughtsService,
    private val domainsService: DomainsService,
    private val validator : ThoughtValidator
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

    fun updateSoulMate(soulMate: String) {
        viewModelScope.launch {
            thoughtDetails.value?.let { thought ->
                thought.soulMate = soulMate
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

    /**
     * Increase value by 1 (max 10)
     */
    fun increaseValue() {
        updateValue(1)
    }

    /**
     * Decrease value by 1 (min 1)
     */
    fun decreaseValue() {
        updateValue(-1)
    }

    /**
     * Update value with bounds checking (1-10)
     * @param delta +1 for increase, -1 for decrease
     */
    fun updateValue(delta: Int) {
        viewModelScope.launch {
            thoughtDetails.value?.let { thought ->
                thought.value = validator.getValidThoughtValue(thought.value + delta)
                thoughtsService.updateThought(thought)
            }
        }
    }

    fun canIncreaseValue(): Boolean {
        return thoughtDetails.value?.let {
            validator.canIncreaseValue(it.value)
        } ?: false
    }

    fun canDecreaseValue(): Boolean {
        return thoughtDetails.value?.let {
            validator.canDecreaseValue(it.value)
        } ?: false
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