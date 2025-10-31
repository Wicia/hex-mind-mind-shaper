package pl.hexmind.mindshaper.database.repositories

import androidx.lifecycle.LiveData
import pl.hexmind.mindshaper.database.models.ThoughtEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThoughtsRepository @Inject constructor (
    private val thoughtsDAO: ThoughtsDAO) {

    suspend fun getThoughtById(id: Long): ThoughtEntity? {
        return thoughtsDAO.getById(id)
    }

    fun getThoughtByIdLive(id: Long): LiveData<ThoughtEntity?> {
        return thoughtsDAO.getThoughtByIdLive(id)
    }

    fun getAllThoughtsLive(): LiveData<List<ThoughtEntity>> {
        return thoughtsDAO.getAllThoughtsLive()
    }

    suspend fun getAllThoughts(): List<ThoughtEntity> {
        return thoughtsDAO.getAllThoughts()
    }

    suspend fun insertThought(thought: ThoughtEntity): Long {
        return thoughtsDAO.insert(thought)
    }

    suspend fun updateThought(thought: ThoughtEntity) {
        thoughtsDAO.update(thought)
    }

    suspend fun deleteThoughtById(id: Int) {
        thoughtsDAO.deleteById(id)
    }
}