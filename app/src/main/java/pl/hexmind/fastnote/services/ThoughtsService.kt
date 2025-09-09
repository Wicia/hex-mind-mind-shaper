package pl.hexmind.fastnote.services

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import pl.hexmind.fastnote.data.models.ThoughtEntity
import pl.hexmind.fastnote.data.repositories.ThoughtsRepository
import pl.hexmind.fastnote.services.dto.ThoughtDTO
import pl.hexmind.fastnote.services.mappers.ThoughtsMapper
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
}