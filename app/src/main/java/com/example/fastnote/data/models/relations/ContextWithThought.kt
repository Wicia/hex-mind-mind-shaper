package com.example.fastnote.data.models.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.example.fastnote.data.models.ContextEntity
import com.example.fastnote.data.models.ThoughtEntity

/**
 * Data class to represent a Context with its associated Thought
 * Used for reverse relationship queries
 */
data class ContextWithThought(
    @Embedded val context: ContextEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "context_id"
    )
    val thought: ThoughtEntity?
)