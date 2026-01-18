package pl.hexmind.mindshaper.activities.details

import android.graphics.Bitmap
import android.net.Uri
import android.util.LruCache
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.hexmind.mindshaper.activities.capture.handlers.Recording
import pl.hexmind.mindshaper.common.ui.CommonIconsListItem
import pl.hexmind.mindshaper.services.DomainsService
import pl.hexmind.mindshaper.services.ThoughtsService
import pl.hexmind.mindshaper.services.dto.ThoughtDTO
import pl.hexmind.mindshaper.services.validators.ThoughtValidator
import java.io.File
import javax.inject.Inject

@HiltViewModel
class DetailsViewModel @Inject constructor(
    private val thoughtsService: ThoughtsService,
    private val domainsService: DomainsService,
    private val validator : ThoughtValidator
) : ViewModel() {

    private val thumbnailCache = LruCache<Int, Bitmap>(50)

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
                thoughtsService.updateThoughtMetadata(thought)
            }
        }
    }

    fun updateThread(thread: String) {
        viewModelScope.launch {
            thoughtDetails.value?.let { thought ->
                thought.thread = thread
                thoughtsService.updateThoughtMetadata(thought)
            }
        }
    }

    fun updateSoulMate(soulMate: String) {
        viewModelScope.launch {
            thoughtDetails.value?.let { thought ->
                thought.soulMate = soulMate
                thoughtsService.updateThoughtMetadata(thought)
            }
        }
    }

    fun updateProject(project: String) {
        viewModelScope.launch {
            thoughtDetails.value?.let { thought ->
                thought.project = project
                thoughtsService.updateThoughtMetadata(thought)
            }
        }
    }

    fun increaseValue() {
        updateValue(1)
    }

    fun decreaseValue() {
        updateValue(-1)
    }

    /**
     * Update value with bounds checking
     * @param delta +1 for increase, -1 for decrease
     */
    fun updateValue(delta: Int) {
        viewModelScope.launch {
            thoughtDetails.value?.let { thought ->
                thought.value = validator.getValidThoughtValue(thought.value + delta)
                thoughtsService.updateThoughtMetadata(thought)
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

    fun saveThought(recording: Recording) {
        viewModelScope.launch {
            // TODO: refactor this and move such logic to services layer (or reduce layers count by 1)
            thoughtDetails.value?.let { thought ->
                val thoughtId = thought.id ?: throw IllegalStateException("Thought ID cannot be null")

                if (thought.hasAudio && !recording.fileExists()) {
                    thoughtsService.deleteThoughtAudio(thoughtId)
                    thought.audioDurationMs = null
                }
                else if (recording.fileExists()) {
                    thoughtsService.updateThoughtRecording(
                        thoughtId.toLong(),
                        recording.file!!,
                        recording.duration!!
                    )
                    thought.audioDurationMs = recording.duration
                }

                thoughtsService.updateThoughtMetadata(thought)
                thoughtsService.updateThoughtRichText(thoughtId, thought.richText)
            }
        }
    }

    fun updateRichText(richText: String) {
        viewModelScope.launch {
            thoughtDetails.value?.id?.let { id ->
                thoughtsService.updateThoughtRichText(id, richText)
            }
        }
    }

    /**
     * Exporting file from DB to temp file & prepares amplitudes
     */
    fun loadAudioForPlayback(
        thoughtId: Int,
        onAudioReady: (File) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val audioData = thoughtsService.getAudioData(thoughtId)

                if (audioData == null || audioData.isEmpty()) {
                    return@launch
                }

                // Creating .temp file for playing
                val tempFile = File.createTempFile("playback_", ".m4a")
                tempFile.writeBytes(audioData)
                onAudioReady(tempFile)

            } catch (e: Exception) {
                e.printStackTrace()
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

    suspend fun getIconIdForDomain(domainId: Int): Int? {
        return domainsService.getIconIdForDomain(domainId)
    }

    fun deletePhoto() {
        viewModelScope.launch {
            thoughtDetails.value?.let { thought ->
                val thoughtId = thought.id ?: return@let
                thoughtsService.deleteThoughtPhoto(thoughtId)
                thumbnailCache.remove(thoughtId)
                loadThought(thoughtId)
            }
        }
    }

    // TODO: Use it also in Carousel
    fun loadPhotoThumbnail(thoughtId: Int, onReady: (Bitmap) -> Unit) {
        viewModelScope.launch {
            // Check cache first
            thumbnailCache.get(thoughtId)?.let {
                onReady(it)
                return@launch
            }

            // Load and generate thumbnail
            val photoData = thoughtsService.getPhotoData(thoughtId)
            if (photoData != null && photoData.isNotEmpty()) {
                val thumbnail = withContext(Dispatchers.Default) {
                    thoughtsService.createThumbnail(photoData)
                }
                thumbnail?.let {
                    thumbnailCache.put(thoughtId, it)
                    onReady(it)
                }
            }
        }
    }

    fun createPhotoUri(): Uri {
        return thoughtsService.createPhotoUri()
    }

    fun savePhotoFromUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val photoFile = thoughtsService.getFileFromUri(uri)
                if (photoFile != null) {
                    thoughtDetails.value?.let { thought ->
                        val thoughtId = thought.id ?: return@let
                        thoughtsService.updateThoughtPhoto(thoughtId.toLong(), photoFile)
                        loadThought(thoughtId)
                    }
                }
            } catch (e: Exception) {
                // Error handled in Activity via HexPhotoView.showError
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        thumbnailCache.evictAll()
    }
}