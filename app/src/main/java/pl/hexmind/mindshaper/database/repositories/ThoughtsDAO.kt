package pl.hexmind.mindshaper.database.repositories

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import pl.hexmind.mindshaper.database.models.ThoughtEntity

@Dao
interface ThoughtsDAO {

    @Query("SELECT * FROM thoughts where id = :id")
    suspend fun getById(id : Long) : ThoughtEntity

    // Basic CRUD operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(thought: ThoughtEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(thoughts: List<ThoughtEntity>)

    @Update
    suspend fun update(thought: ThoughtEntity)

    @Delete
    suspend fun delete(thought: ThoughtEntity)

    @Query("DELETE FROM thoughts WHERE id = :id")
    suspend fun deleteById(id: Int)

    // Basic queries
    @Query("SELECT * FROM thoughts ORDER BY created_at DESC")
    fun getAllThoughts(): LiveData<List<ThoughtEntity>>

    @Query("SELECT * FROM thoughts WHERE id = :id")
    suspend fun getThoughtById(id: Int): ThoughtEntity?

    @Query("SELECT * FROM thoughts WHERE domain_id = :domainId")
    suspend fun getThoughtByDomainId(domainId: Int): ThoughtEntity?
}