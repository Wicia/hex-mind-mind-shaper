package com.example.fastnote.db.repositories

import androidx.lifecycle.LiveData
import com.example.fastnote.db.ThoughtEntity

class ThoughtsRepository (private val dao: ThoughtsDAO) {

    fun getThoughts(): LiveData<List<ThoughtEntity>> = dao.getAll()

    suspend fun addThought(thought: ThoughtEntity) = dao.insertAll(thought)

    suspend fun deleteThought(thought: ThoughtEntity) = dao.delete(thought)
}