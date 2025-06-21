package com.example.fastnote.data.repositories

import com.example.fastnote.data.models.AreaIdentifier
import com.example.fastnote.data.models.ContextEntity
import com.example.fastnote.data.models.ThoughtEntity

class ThoughtsRepository (
    private val thoughtsDAO: ThoughtsDAO,
    private val contextsDAO: ContextsDAO) {

    fun getAllThoughtsWithContext() = thoughtsDAO.getAllThoughtsWithContext()

    suspend fun createThoughtWithContext(
        essence: String,
        areaIdentifier: AreaIdentifier,
        priority: Int = 3,
        thread: String? = null
    ): Long {
        // First, create or find the context
        val context = ContextEntity(areaIdentifier = areaIdentifier, thread = thread)
        val contextId = contextsDAO.insert(context)

        // Then create the thought with the context ID
        val thought = ThoughtEntity(
            contextId = contextId.toInt(),
            essence = essence,
            priority = priority
        )
        return thoughtsDAO.insert(thought)
    }

    suspend fun createThoughtWithExistingContext(
        essence: String,
        contextId: Int,
        priority: Int = 3
    ): Long {
        val thought = ThoughtEntity(
            contextId = contextId,
            essence = essence,
            priority = priority
        )
        return thoughtsDAO.insert(thought)
    }
}