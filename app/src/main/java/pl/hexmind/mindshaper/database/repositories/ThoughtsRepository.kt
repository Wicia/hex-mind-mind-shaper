package pl.hexmind.mindshaper.database.repositories

import androidx.lifecycle.LiveData
import pl.hexmind.mindshaper.database.models.ThoughtEntity
import pl.hexmind.mindshaper.database.models.ThoughtMetadataUpdate
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThoughtsRepository @Inject constructor (
    private val thoughtsDAO: ThoughtsDAO
) {

    fun getThoughtByIdLive(id: Long): LiveData<ThoughtEntity?> {
        require(id > 0) { "Thought ID must be positive" }
        return thoughtsDAO.getThoughtByIdLive(id)
    }

    fun getAllThoughtsLive(): LiveData<List<ThoughtEntity>> {
        return thoughtsDAO.getAllThoughtsLive()
    }

    suspend fun insertThought(thought: ThoughtEntity): Long {
        return thoughtsDAO.insert(thought)
    }

    suspend fun updateThought(thought: ThoughtEntity) {
        thoughtsDAO.update(thought)
    }

    suspend fun deleteThoughtById(id: Int) {
        require(id > 0) { "Thought ID must be positive" }
        thoughtsDAO.deleteById(id)
    }

    /**
     * Save audio from file to database
     * Automatically deletes temp file after saving
     */
    suspend fun saveAudioFromFile(thoughtId: Long, audioFile: File, durationMs: Long) {
        require(audioFile.exists()) { "Audio file does not exist: ${audioFile.absolutePath}" }
        require(audioFile.length() > 0) { "Audio file is empty" }

        val audioBytes = audioFile.readBytes()
        thoughtsDAO.updateAudio(thoughtId, audioBytes, durationMs)
        audioFile.delete()
    }

    /**
     * Get audio data for thought
     */
    suspend fun getAudioData(thoughtId: Long): ByteArray? {
        require(thoughtId > 0) { "Thought ID must be positive" }
        return thoughtsDAO.getAudioData(thoughtId)
    }

    suspend fun updateThoughtMetadata(metadata: ThoughtMetadataUpdate) {
        thoughtsDAO.updateMetadata(metadata)
    }

    suspend fun updateRichText(thoughtId: Int, richText: String?) {
        thoughtsDAO.updateRichText(thoughtId, richText)
    }
}