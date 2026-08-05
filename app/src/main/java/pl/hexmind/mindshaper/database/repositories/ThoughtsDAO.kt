package pl.hexmind.mindshaper.database.repositories

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import pl.hexmind.mindshaper.database.models.ThoughtEntity
import pl.hexmind.mindshaper.database.models.ThoughtMetadataUpdate
import pl.hexmind.mindshaper.database.models.ThoughtSubjectRow

@Dao
interface ThoughtsDAO {

    @Query("SELECT * FROM thoughts where id = :id")
    suspend fun getById(id: Long): ThoughtEntity

    @Query("SELECT * FROM thoughts WHERE id = :id")
    fun getThoughtByIdLive(id: Long): LiveData<ThoughtEntity?>

    // Basic CRUD operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(thought: ThoughtEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(thoughts: List<ThoughtEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(icons: List<ThoughtEntity>)

    @Update
    suspend fun update(thought: ThoughtEntity)

    @Delete
    suspend fun delete(thought: ThoughtEntity)

    @Query("DELETE FROM thoughts WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("SELECT id, subject FROM thoughts WHERE id IN (:ids)")
    suspend fun getSubjectsByIds(ids: List<Int>): List<ThoughtSubjectRow>

    // Basic queries
    @Query("SELECT * FROM thoughts ORDER BY created_at DESC")
    fun getAllThoughtsLive(): LiveData<List<ThoughtEntity>>

    @Query("SELECT * FROM thoughts ORDER BY created_at DESC")
    suspend fun getAllThoughts(): List<ThoughtEntity>

    @Query("SELECT * FROM thoughts WHERE domain_id = :domainId")
    suspend fun getThoughtByDomainId(domainId: Int): ThoughtEntity?

    @Query("DELETE FROM thoughts")
    suspend fun clearAll()

    @Update(entity = ThoughtEntity::class)
    suspend fun updateMetadata(metadata: ThoughtMetadataUpdate)

// ========== RICH TEXT NOTES ==========

    @Query("UPDATE THOUGHTS SET rich_text = :richText, updated_at = :updatedAt WHERE id = :thoughtId")
    suspend fun updateRichText(thoughtId: Int, richText: String?, updatedAt: Long)

// ========== AUDIO RECORDINGS ==========

    @Query("SELECT audio_data FROM thoughts WHERE id = :id")
    suspend fun getAudioData(id: Long): ByteArray?

    @Query("UPDATE thoughts SET audio_data = :audioData, audio_duration_ms = :durationMs, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateAudio(id: Long, audioData: ByteArray?, durationMs: Long?, updatedAt: Long)

    @Query("UPDATE thoughts SET audio_data = NULL, audio_duration_ms = NULL, updated_at = :updatedAt WHERE id = :id")
    suspend fun deleteAudio(id: Long, updatedAt: Long)

// ========== PHOTOS ==========

    @Query("UPDATE THOUGHTS SET photo_data = :photoBytes, photo_file_size = :fileSize, updated_at = :updatedAt WHERE id = :thoughtId")
    suspend fun updatePhoto(thoughtId: Long, photoBytes: ByteArray, fileSize: Long, updatedAt: Long)

    @Query("SELECT photo_data FROM THOUGHTS WHERE id = :thoughtId")
    suspend fun getPhotoData(thoughtId: Long): ByteArray?

    @Query("UPDATE THOUGHTS SET photo_data = NULL, photo_file_size = NULL, updated_at = :updatedAt WHERE id = :thoughtId")
    suspend fun deletePhoto(thoughtId: Long, updatedAt: Long)

// ========== ONBOARDING ==========

    // Single column on purpose: the whole row carries audio and photo blobs
    @Query("SELECT id FROM thoughts ORDER BY created_at DESC LIMIT 1")
    suspend fun getNewestThoughtId(): Int?
}