package com.example.fastnote.db.repositories

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.fastnote.db.ThoughtEntity

@Dao
interface ThoughtsDAO {

    @Query("SELECT * FROM thoughts")
    fun getAll(): LiveData<List<ThoughtEntity>>

    @Query("SELECT * FROM thoughts WHERE id IN (:ids)")
    fun loadAllByIds(ids: IntArray): LiveData<List<ThoughtEntity>>

    @Insert
    suspend fun insertAll(vararg thoughts: ThoughtEntity)

    @Delete
    suspend fun delete(thought: ThoughtEntity)
}