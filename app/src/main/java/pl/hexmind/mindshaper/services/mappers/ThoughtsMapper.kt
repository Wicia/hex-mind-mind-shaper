package pl.hexmind.mindshaper.services.mappers

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import pl.hexmind.mindshaper.database.models.ThoughtEntity
import pl.hexmind.mindshaper.services.dto.ThoughtDTO
import org.mapstruct.factory.Mappers

@Mapper
interface ThoughtsMapper {

    companion object {
        // ! To solve problems with injecting mapper using Hilt
        val INSTANCE: ThoughtsMapper = Mappers.getMapper(ThoughtsMapper::class.java)
    }

    @Mapping(source = "id", target = "id")
    @Mapping(source = "createdAt", target = "createdAt")
    @Mapping(source = "thread", target = "thread")
    @Mapping(source = "richText", target = "richText")
    @Mapping(source = "domainId", target = "domainId")
    fun entityToDTO(entity : ThoughtEntity) : ThoughtDTO

    @Mapping(source = "id", target = "id")
    @Mapping(source = "createdAt", target = "createdAt")
    @Mapping(source = "thread", target = "thread")
    @Mapping(source = "richText", target = "richText")
    @Mapping(source = "domainId", target = "domainId")
    fun dtoToEntity(dto : ThoughtDTO) : ThoughtEntity

    fun entityListToDtoList(entities: List<ThoughtEntity>): List<ThoughtDTO>
}