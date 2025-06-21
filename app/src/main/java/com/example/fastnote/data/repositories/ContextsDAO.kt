package com.example.fastnote.data.repositories

import androidx.lifecycle.LiveData
import com.example.fastnote.data.models.ThoughtEntity

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.fastnote.data.models.ContextEntity

@Dao

interface ContextsDAO {

    // Basic CRUD operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(context: ContextEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(contexts: List<ContextEntity>)

    @Update
    suspend fun update(context: ContextEntity)

    @Delete
    suspend fun delete(context: ContextEntity)

    @Query("DELETE FROM contexts WHERE id = :id")
    suspend fun deleteById(id: Int)

    // Basic queries
    @Query("SELECT * FROM contexts ORDER BY id")
    fun getAllContexts(): LiveData<List<ContextEntity>>

    @Query("SELECT * FROM contexts WHERE id = :id")
    suspend fun getContextById(id: Int): ContextEntity?

    @Query("SELECT * FROM contexts WHERE areaIdentifier = :areaIdentifier")
    fun getContextsByArea(areaIdentifier: String): LiveData<List<ContextEntity>>

    @Query("SELECT * FROM contexts WHERE thread = :thread")
    fun getContextsByThread(thread: String): LiveData<List<ContextEntity>>

    @Query("SELECT * FROM contexts WHERE areaIdentifier = :areaIdentifier AND thread = :thread")
    suspend fun getContextByAreaAndThread(areaIdentifier: String, thread: String): ContextEntity?
}