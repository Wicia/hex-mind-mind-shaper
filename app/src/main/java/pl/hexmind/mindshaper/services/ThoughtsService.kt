package pl.hexmind.mindshaper.services

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import pl.hexmind.mindshaper.database.models.ThoughtEntity
import pl.hexmind.mindshaper.database.models.ThoughtMetadataUpdate
import pl.hexmind.mindshaper.database.repositories.ThoughtsRepository
import pl.hexmind.mindshaper.services.dto.ThoughtDTO
import pl.hexmind.mindshaper.services.mappers.ThoughtsMapper
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThoughtsService @Inject constructor(
    private val repository: ThoughtsRepository
) {

    /**
     * Get all thoughts (reactive/LiveData)
     */
    fun getAllThoughts(): LiveData<List<ThoughtDTO>> {
        val result = repository.getAllThoughtsLive()
        return entityLiveDataToDtoLiveData(result)
    }

    fun getThoughtByIdLive(id: Int): LiveData<ThoughtDTO?> {
        val entityLiveData = repository.getThoughtByIdLive(id.toLong())
        return entityLiveData.map { entityThought ->
            entityThought?.let { ThoughtsMapper.INSTANCE.entityToDTO(it) }
        }
    }

    private fun entityLiveDataToDtoLiveData(entities: LiveData<List<ThoughtEntity>>): LiveData<List<ThoughtDTO>> {
        return entities.map { list ->
            ThoughtsMapper.INSTANCE.entityListToDtoList(list)
        }
    }

    suspend fun addThought(thought: ThoughtDTO) {
        val entity = ThoughtsMapper.INSTANCE.dtoToEntity(thought)
        repository.insertThought(entity)
    }

    suspend fun deleteThoughtById(id: Int) {
        repository.deleteThoughtById(id)
    }

    suspend fun updateThought(thought: ThoughtDTO) {
        val entity = ThoughtsMapper.INSTANCE.dtoToEntity(thought)
        repository.updateThought(entity)
    }

    // === Sophisticated methods for updating specific part of thought (rich text, recording...)

    suspend fun updateThoughtMetadata(thought: ThoughtDTO) {
        val metadata = ThoughtMetadataUpdate(
            id = thought.id!!,
            domainId = thought.domainId,
            thread = thought.thread,
            soulMate = thought.soulMate,
            project = thought.project,
            value = thought.value
        )
        repository.updateThoughtMetadata(metadata)
    }

    suspend fun updateThoughtRichText(thoughtId: Int, richText: String?) {
        repository.updateRichText(thoughtId, richText)
    }

    /**
     * Save thought with audio recording.
     * Audio file is passed as separate object instead of byte data/array in DTO
     * @param dto ThoughtDTO with thought data
     * @param audioFile Temporary file with audio recording
     * @return ID of saved thought
     */
    suspend fun addThoughtWithAudio(dto: ThoughtDTO, audioFile: File): Long {
        val entity = ThoughtsMapper.INSTANCE.dtoToEntity(dto)
        val thoughtId = repository.insertThought(entity)

        updateThoughtRecording(thoughtId, audioFile, dto.duration ?: 0L)

        return thoughtId
    }

    suspend fun updateThoughtRecording(thoughtId : Long, audioFile: File, duration : Long) {
        repository.saveAudioFromFile(
            thoughtId = thoughtId,
            audioFile = audioFile,
            durationMs = duration
        )
    }

    suspend fun getAudioData(thoughtId: Int): ByteArray? {
        return repository.getAudioData(thoughtId.toLong())
    }
}