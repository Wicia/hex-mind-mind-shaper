package pl.hexmind.fastnote.data.repositories

import pl.hexmind.fastnote.data.models.ThoughtEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThoughtsRepository @Inject constructor (
    private val thoughtsDAO: ThoughtsDAO) {

    suspend fun getThoughtById(id: Long): ThoughtEntity? {
        return thoughtsDAO.getById(id)
    }

    suspend fun insertThought(thought: ThoughtEntity) {
        thoughtsDAO.insert(thought)
    }

    suspend fun updateThought(thought: ThoughtEntity) {
        thoughtsDAO.update(thought)
    }

    suspend fun deleteThought(thought: ThoughtEntity) {
        thoughtsDAO.delete(thought)
    }
}