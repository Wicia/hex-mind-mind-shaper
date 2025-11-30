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

    @Mapping(target = "hasAudio", expression = "java(entity.getAudioData() != null)")
    @Mapping(target = "audioSizeBytes", expression = "java(entity.getAudioData() != null ? (long)entity.getAudioData().length : null)")
    @Mapping(target = "audioData", ignore = true) // ! not mapping ByteArray -> (too big)
    @Mapping(target = "tempAudioFilePath", ignore = true)
    fun entityToDTO(entity: ThoughtEntity): ThoughtDTO

    @Mapping(target = "audioData", ignore = true)
    @Mapping(target = "audioDurationMs", ignore = true)
    fun dtoToEntity(dto: ThoughtDTO): ThoughtEntity

    fun entityListToDtoList(entities: List<ThoughtEntity>): List<ThoughtDTO>
}