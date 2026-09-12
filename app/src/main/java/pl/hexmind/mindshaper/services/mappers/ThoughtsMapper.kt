package pl.hexmind.mindshaper.services.mappers

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Named
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

    // ! Tags left to the service - they live in HEX_TAGS now, not on the entity
    @Mapping(target = "person", ignore = true)
    @Mapping(target = "project", ignore = true)
    @Mapping(target = "people", ignore = true)
    @Mapping(target = "projects", ignore = true)
    @Mapping(target = "tempAudioFilePath", ignore = true) // Only used during recording
    @Mapping(target = "tempPhotoFilePath", ignore = true) // Only used during taking a photo
    fun entityToDTO(entity: ThoughtEntity): ThoughtDTO

    @Mapping(target = "audioData", ignore = true)
    @Mapping(target = "audioDurationMs", ignore = true)
    @Mapping(target = "photoData", ignore = true)
    @Mapping(target = "photoFileSize", ignore = true)
    fun dtoToEntity(dto: ThoughtDTO): ThoughtEntity

    fun entityListToDtoList(entities: List<ThoughtEntity>): List<ThoughtDTO>
}