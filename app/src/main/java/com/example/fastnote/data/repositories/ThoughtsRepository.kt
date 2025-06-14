package com.example.fastnote.data.repositories

import androidx.lifecycle.LiveData
import com.example.fastnote.data.models.ThoughtEntity

class ThoughtsRepository (private val dao: ThoughtsDAO) {

    fun getThoughts(): LiveData<List<ThoughtEntity>> = dao.getAll()

    suspend fun addThought(thought: ThoughtEntity) = dao.insertAll(thought)

    suspend fun deleteThought(thought: ThoughtEntity) = dao.delete(thought)
}