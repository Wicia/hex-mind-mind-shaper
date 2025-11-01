package pl.hexmind.mindshaper.services.mappers

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.factory.Mappers
import pl.hexmind.mindshaper.database.models.ThoughtEntity
import pl.hexmind.mindshaper.services.dto.ThoughtDTO

/**
 * ! Don't handle here LiveData mappings
 */
@Mapper
interface ThoughtsMapper {

    companion object {
        // ! To solve problems with injecting mapper using Hilt
        val INSTANCE: ThoughtsMapper = Mappers.getMapper(ThoughtsMapper::class.java)
    }

    // Introduce here mapping only when more complex mapping mechanism is needed
    fun entityToDTO(entity : ThoughtEntity) : ThoughtDTO

    // Introduce here mapping only when more complex mapping mechanism is needed
    fun dtoToEntity(dto : ThoughtDTO) : ThoughtEntity

    fun entityListToDtoList(entities: List<ThoughtEntity>): List<ThoughtDTO>
}