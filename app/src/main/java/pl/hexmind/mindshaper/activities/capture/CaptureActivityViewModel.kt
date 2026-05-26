package pl.hexmind.mindshaper.activities.capture

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.common.validation.ValidationResult
import pl.hexmind.mindshaper.services.ThoughtsService
import pl.hexmind.mindshaper.services.dto.ThoughtDTO
import pl.hexmind.mindshaper.services.validators.ThoughtValidator
import java.io.File
import javax.inject.Inject

@HiltViewModel
class CaptureActivityViewModel @Inject constructor(
    private val thoughtsService: ThoughtsService,
    private val validator: ThoughtValidator
) : ViewModel() {

    // ========================================
    // DRAFT THOUGHT (in memory, NOT in DB)
    // ========================================

    private val _draftThought = MutableLiveData(ThoughtDTO())
    val draftThought: LiveData<ThoughtDTO> = _draftThought

    // ========================================
    // TEMPORARY CONTENT (not saved to DB yet)
    // ========================================

    private var tempAudioFile: File? = null
    private var tempAudioDuration: Long = 0L
    private var tempPhotoUri: Uri? = null

    // ========================================
    // UPDATE DRAFT (collect data in memory)
    // ========================================

    /**
     * Update rich text in draft (no DB save)
     */
    fun updateRichText(richText: String) {
        _draftThought.value = _draftThought.value?.copy(
            richText = richText
        )
    }

    /**
     * Update hex tags in draft (no DB save)
     */
    fun updateHexTags(subject: String?, project: String?, soulMate: String?) {
        _draftThought.value = _draftThought.value?.copy(
            subject = subject,
            project = project,
            soulMate = soulMate
        )
    }

    /**
     * Store audio file temporarily (no DB save)
     * Just keep reference until final save
     */
    fun saveAudioRecording(file: File, durationMs: Long) {
        tempAudioFile = file
        tempAudioDuration = durationMs

        // Update draft to show "hasAudio" in UI
        _draftThought.value = _draftThought.value?.copy(
            audioDurationMs = durationMs
        )
    }

    /**
     * Store photo URI temporarily (no DB save)
     * Just keep reference until final save
     */
    fun savePhotoUri(uri: Uri) {
        tempPhotoUri = uri

        // Update draft to show "hasPhoto" in UI
        _draftThought.value = _draftThought.value?.copy(
            photoFileSize = 1L // Non-zero = has photo
        )
    }

    // ========================================
    // DELETE CONTENT (just clear local data)
    // ========================================

    /**
     * Delete rich text from draft (no DB operation)
     */
    fun deleteRichText() {
        _draftThought.value = _draftThought.value?.copy(
            richText = null
        )
    }

    /**
     * Delete audio recording (clear temp file)
     */
    fun deleteAudioRecording() {
        tempAudioFile?.delete()
        tempAudioFile = null
        tempAudioDuration = 0L

        _draftThought.value = _draftThought.value?.copy(
            audioDurationMs = null
        )
    }

    /**
     * Delete photo (clear temp URI)
     */
    fun deletePhoto() {
        tempPhotoUri = null

        _draftThought.value = _draftThought.value?.copy(
            photoFileSize = null
        )
    }

    // ========================================
    // VALIDATION TODO: to be moved from here to another class with strings handling
    // ========================================

    /**
     * Validate draft before saving
     * Returns ValidationResult with error message or Valid
     */
    fun validate(): ValidationResult {
        val draft = _draftThought.value ?: return ValidationResult.Error(R.string.common_thought_draft_not_found)

        // Validate hex tags (subject, project, soulMate)
        val subjectResult = validator.validateSubject(draft.subject)
        if (subjectResult is ValidationResult.Error) return subjectResult

        val projectResult = validator.validateProject(draft.project)
        if (projectResult is ValidationResult.Error) return projectResult

        val soulMateResult = validator.validateSoulMates(draft.soulMate)
        if (soulMateResult is ValidationResult.Error) return soulMateResult

        // Validate rich text (if present)
        if (!draft.richText.isNullOrBlank()) {
            val richTextResult = validator.validateRichText(draft.richText)
            if (richTextResult is ValidationResult.Error) return richTextResult
        }

        // Validate audio (if present)
        if (tempAudioFile != null) {
            if (!tempAudioFile!!.exists()) {
                return ValidationResult.Error(R.string.capture_voice_error_not_exists)
            }
            if (tempAudioDuration == 0L) {
                return ValidationResult.Error(R.string.capture_voice_error_empty)
            }
        }

        // Check if at least ONE content exists
        val hasContent = !draft.richText.isNullOrBlank()
                || tempAudioFile != null
                || tempPhotoUri != null

        if (!hasContent) {
            return ValidationResult.Error(R.string.capture_error_no_content)
        }

        return ValidationResult.Valid()
    }

    // ========================================
    // SAVE TO DB (on SAVE fab)
    // ========================================

    /**
     * Save new thought to database
     * Strategy: Save thought first, then update audio/photo if they exist
     *
     *
     * @return Result.success(thoughtId) if saved, Result.failure with exception if error
     */
    suspend fun saveNewThought(): Result<Long> {
        return try {
            val draft = _draftThought.value
                ?: return Result.failure(Exception("No draft available")) // TODO: don't refactor for now

            // Step 1: Always save thought first (with richText + hexTags)
            val thoughtId = thoughtsService.addThought(draft)

            // Step 2: If audio exists → update
            if (tempAudioFile != null) {
                thoughtsService.updateThoughtRecording(
                    thoughtId,
                    tempAudioFile!!,
                    tempAudioDuration
                )
            }

            // Step 3: If photo exists → update
            if (tempPhotoUri != null) {
                val photoFile = thoughtsService.getFileFromUri(tempPhotoUri!!)
                    ?: return Result.failure(Exception("Photo file not found")) // TODO: don't refactor for now

                thoughtsService.updateThoughtPhoto(thoughtId, photoFile)
            }

            Result.success(thoughtId)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========================================
    // UTILITY METHODS
    // ========================================

    /**
     * Create temporary photo URI for camera
     */
    fun createPhotoUri(): Uri {
        return thoughtsService.createPhotoUri()
    }

    /**
     * Get current audio file (for playback preview)
     */
    fun getTempAudioFile(): File? = tempAudioFile

    /**
     * Get current photo URI (for display preview)
     */
    fun getTempPhotoUri(): Uri? = tempPhotoUri

    // ========================================
    // CLEANUP
    // ========================================

    override fun onCleared() {
        super.onCleared()
        // Cleanup temporary files when ViewModel is destroyed
        tempAudioFile?.delete()
    }
}