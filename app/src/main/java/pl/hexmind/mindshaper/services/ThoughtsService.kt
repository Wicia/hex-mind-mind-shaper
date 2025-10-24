package pl.hexmind.mindshaper.services

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import pl.hexmind.mindshaper.database.models.ThoughtEntity
import pl.hexmind.mindshaper.database.repositories.ThoughtsRepository
import pl.hexmind.mindshaper.services.dto.ThoughtDTO
import pl.hexmind.mindshaper.services.mappers.ThoughtsMapper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThoughtsService @Inject constructor(
    private val repository : ThoughtsRepository
){

    /**
     * Get all thoughts (reactive/LiveData)
     */
    fun getAllThoughts(): LiveData<List<ThoughtDTO>> {
        val result = repository.getAllThoughts()
        return entityLiveDataToDtoLiveData(result)
    }

    private fun entityLiveDataToDtoLiveData(entities: LiveData<List<ThoughtEntity>>): LiveData<List<ThoughtDTO>> {
        return entities.map { list ->
            ThoughtsMapper.INSTANCE.entityListToDtoList(list)
        }
    }

    suspend fun addThought(thought: ThoughtDTO){
        val entity = ThoughtsMapper.INSTANCE.dtoToEntity(thought)
        repository.insertThought(entity)
    }

    suspend fun deleteThought(thought: ThoughtDTO){
        repository.deleteThoughtById(thought.id ?: throw IllegalArgumentException("Thought ID cannot be null"))
    }

    suspend fun deleteThoughtById(id: Int){
        repository.deleteThoughtById(id)
    }

    suspend fun updateThought(thought: ThoughtDTO){
        val entity = ThoughtsMapper.INSTANCE.dtoToEntity(thought)
        repository.updateThought(entity)
    }
}