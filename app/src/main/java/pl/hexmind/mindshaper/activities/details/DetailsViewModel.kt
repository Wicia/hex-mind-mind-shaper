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
import kotlinx.coroutines.launch
import pl.hexmind.mindshaper.common.ui.dialogs.HexTags
import pl.hexmind.mindshaper.common.ui.views.lists.CommonIconsListItem
import pl.hexmind.mindshaper.common.validation.ValidationResult
import pl.hexmind.mindshaper.services.DomainsService
import pl.hexmind.mindshaper.services.GoalsService
import pl.hexmind.mindshaper.services.ThoughtsService
import pl.hexmind.mindshaper.services.dto.GuidelineWithGoalDTO
import pl.hexmind.mindshaper.services.dto.ThoughtDTO
import pl.hexmind.mindshaper.services.validators.ThoughtValidator
import java.io.File
import javax.inject.Inject

@HiltViewModel
class DetailsViewModel @Inject constructor(
    private val thoughtsService: ThoughtsService,
    private val domainsService: DomainsService,
    private val goalsService: GoalsService,
    private val validator : ThoughtValidator
) : ViewModel() {

    private val thumbnailCache = LruCache<Int, Bitmap>(50)

    private val _thoughtId = MutableLiveData<Int>()

    // ! switchMap to observe and react when changes in ID happens
    val thoughtDetails: LiveData<ThoughtDTO?> = _thoughtId.switchMap { id ->
        thoughtsService.getThoughtByIdLive(id)
    }


    // Domains
    private val _domainsWithIcons = MutableLiveData<List<CommonIconsListItem>>(emptyList())
    val domainsWithIcons: LiveData<List<CommonIconsListItem>> = _domainsWithIcons

    // Linked guideline
    private val _linkedGuideline = MutableLiveData<GuidelineWithGoalDTO?>(null)
    val linkedGuideline: LiveData<GuidelineWithGoalDTO?> = _linkedGuideline

    fun loadThought(id: Int) {
        _thoughtId.value = id
        refreshLinkedGuideline()
    }

    fun validateTags(tags: HexTags): ValidationResult {
        val personResult = validator.validateSoulMates(tags.person)
        if (personResult is ValidationResult.Error) return personResult

        val projectResult = validator.validateProject(tags.project)
        if (projectResult is ValidationResult.Error) return projectResult

        return ValidationResult.Valid()
    }

    fun updateHexTags(tags: HexTags) {
        viewModelScope.launch {
            thoughtDetails.value?.let { thought ->
                thought.soulMate = tags.person ?: ""
                thought.project = tags.project ?: ""
                thought.domainId = tags.domainId
                thoughtsService.updateThoughtMetadata(thought)
            }
        }
    }

    fun loadDomains() {
        viewModelScope.launch {
            val domains = domainsService.getAllDomainWithIcons()
            _domainsWithIcons.value = domains
        }
    }

    fun updateSubject(subject: String) {
        viewModelScope.launch {
            thoughtDetails.value?.let { thought ->
                thought.subject = subject
                thoughtsService.updateThoughtMetadata(thought)
            }
        }
    }

    fun increaseValue() {
        if (canIncreaseValue()) {
            updateValue(1)
        }
        else { // Go back to MIN value for thought
            viewModelScope.launch {
                thoughtDetails.value?.let { thought ->
                    thought.value = validator.getThoughtValueMin()
                    thoughtsService.updateThoughtMetadata(thought)
                }
            }
        }
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

    fun saveAudioRecording(file: File, durationMs: Long) {
        viewModelScope.launch {
            thoughtDetails.value?.let { thought ->
                val thoughtId = thought.id ?: return@let
                thoughtsService.updateThoughtRecording(
                    thoughtId.toLong(),
                    file,
                    durationMs
                )
                thought.audioDurationMs = durationMs
                loadThought(thoughtId)  // Refresh UI
            }
        }
    }

    fun deleteAudioRecording() {
        viewModelScope.launch {
            thoughtDetails.value?.let { thought ->
                val thoughtId = thought.id ?: return@let
                thoughtsService.deleteThoughtAudio(thoughtId)
                thought.audioDurationMs = null
                loadThought(thoughtId)  // Refresh UI
            }
        }
    }

    /**
     * Invoking updating mechanism to reset dormant state ("moving" thought to ACTIVE state automatically)
     */
    fun restoreFromDormant() {
        viewModelScope.launch {
            thoughtDetails.value?.let { thought ->
                thoughtsService.updateThoughtMetadata(thought)
            }
        }
    }

    fun deleteRichText() {
        viewModelScope.launch {
            thoughtDetails.value?.let { thought ->
                val thoughtId = thought.id ?: return@let
                thoughtsService.updateThoughtRichText(thoughtId, null) // TODO: add new method for deletion?
                thought.richText = null
                loadThought(thoughtId)  // Refresh UI
            }
        }
    }

    // ── Linked guideline ──────────────────────────────────

    /**
     * Refreshes the linked-guideline state for the currently loaded thought.
     * Call after every change (link / unlink / swap) and on screen open.
     */
    fun refreshLinkedGuideline() {
        val thoughtId = _thoughtId.value ?: return
        viewModelScope.launch {
            _linkedGuideline.value = goalsService.findGuidelineLinkedToThought(thoughtId)
        }
    }

    fun linkToGuideline(guidelineId: Int) {
        val thoughtId = _thoughtId.value ?: return
        viewModelScope.launch {
            goalsService.linkThoughtToGuideline(thoughtId, guidelineId)
            refreshLinkedGuideline()
        }
    }

    fun unlinkFromGuideline() {
        val linkedGuideline = _linkedGuideline.value ?: return
        // Optimistic clear so UI flips immediately
        _linkedGuideline.value = null
        viewModelScope.launch {
            goalsService.unlinkThought(linkedGuideline.guidelineId, alsoDeleteThought = false)
        }
    }

    override fun onCleared() {
        super.onCleared()
        thumbnailCache.evictAll()
    }
}