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

    @Mapping(source = "id", target = "id")
    @Mapping(source = "createdAt", target = "createdAt")
    @Mapping(source = "thread", target = "thread")
    @Mapping(source = "richText", target = "richText")
    @Mapping(source = "domainId", target = "domainId")
    @Mapping(source = "commonStory", target = "project") // TODO: rename column
    fun entityToDTO(entity : ThoughtEntity) : ThoughtDTO

    @Mapping(source = "id", target = "id")
    @Mapping(source = "createdAt", target = "createdAt")
    @Mapping(source = "thread", target = "thread")
    @Mapping(source = "richText", target = "richText")
    @Mapping(source = "domainId", target = "domainId")
    @Mapping(source = "project", target = "commonStory")
    fun dtoToEntity(dto : ThoughtDTO) : ThoughtEntity

    fun entityListToDtoList(entities: List<ThoughtEntity>): List<ThoughtDTO>
}