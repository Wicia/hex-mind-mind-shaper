package pl.hexmind.mindshaper.database.repositories

import androidx.lifecycle.LiveData
import pl.hexmind.mindshaper.database.models.ThoughtEntity
import pl.hexmind.mindshaper.database.models.relations.ThoughtWithDomain
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThoughtsRepository @Inject constructor (
    private val thoughtsDAO: ThoughtsDAO) {

    /**
     * Get single thought by ID for editing purposes
     */
    suspend fun getThoughtById(id: Long): ThoughtEntity? {
        return thoughtsDAO.getById(id)
    }

    /**
     * Get all thoughts as reactive LiveData - automatically updates UI when data changes
     */
    fun getAllThoughts(): LiveData<List<ThoughtEntity>> {
        return thoughtsDAO.getAllThoughts()
    }

    /**
     * Get all thoughts with domain relationship as reactive LiveData
     */
    fun getAllThoughtsWithDomain(): LiveData<List<ThoughtWithDomain>> {
        return thoughtsDAO.getAllThoughtsWithDomain()
    }

    /**
     * Insert new thought into database
     */
    suspend fun insertThought(thought: ThoughtEntity): Long {
        return thoughtsDAO.insert(thought)
    }

    /**
     * Update existing thought in database
     */
    suspend fun updateThought(thought: ThoughtEntity) {
        thoughtsDAO.update(thought)
    }

    /**
     * Delete thought from database
     */
    suspend fun deleteThought(thought: ThoughtEntity) {
        thoughtsDAO.delete(thought)
    }

    /**
     * Get single thought with domain by ID
     */
    suspend fun getThoughtWithDomainById(thoughtId: Int): ThoughtWithDomain? {
        return thoughtsDAO.getThoughtWithDomain(thoughtId)
    }
}