package pl.hexmind.fastnote.data.repositories

import androidx.lifecycle.LiveData
import pl.hexmind.fastnote.data.models.ThoughtEntity

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import pl.hexmind.fastnote.data.models.relations.ThoughtWithDomain

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

    // Relationship queries - these are the important ones for 1:1 relationships
    @Transaction
    @Query("SELECT * FROM thoughts ORDER BY created_at DESC")
    fun getAllThoughtsWithDomain(): LiveData<List<ThoughtWithDomain>>

    @Transaction
    @Query("SELECT * FROM thoughts WHERE id = :thoughtId")
    suspend fun getThoughtWithDomain(thoughtId: Int): ThoughtWithDomain?
}