package pl.hexmind.mindshaper.services.mappers

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import pl.hexmind.mindshaper.database.models.ThoughtEntity
import pl.hexmind.mindshaper.services.dto.ThoughtDTO
import org.mapstruct.factory.Mappers

@Mapper
interface ThoughtsMapper {

    companion object {
        // Static instance - no Hilt needed, always works
        val INSTANCE: ThoughtsMapper = Mappers.getMapper(ThoughtsMapper::class.java)
    }

    @Mapping(source = "essence", target = "essence")
    @Mapping(source = "createdAt", target = "createdAt")
    fun entityToDTO(entity : ThoughtEntity) : ThoughtDTO

    @Mapping(source = "essence", target = "essence")
    @Mapping(source = "createdAt", target = "createdAt")
    fun dtoToEntity(dto : ThoughtDTO) : ThoughtEntity

    fun entityListToDtoList(entities: List<ThoughtEntity>): List<ThoughtDTO>
}