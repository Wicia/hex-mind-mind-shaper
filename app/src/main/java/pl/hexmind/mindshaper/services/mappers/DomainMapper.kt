package pl.hexmind.mindshaper.services.mappers

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.factory.Mappers
import pl.hexmind.mindshaper.database.models.DomainEntity
import pl.hexmind.mindshaper.services.dto.DomainDTO

@Mapper
interface DomainMapper {

    companion object {
        // ! To solve problems with injecting mapper using Hilt
        val INSTANCE: DomainMapper = Mappers.getMapper(DomainMapper::class.java)
    }

    // Introduce here mapping only when more complex mapping mechanism is needed
    @Mapping(source = "assetsIconId", target = "iconId")
    fun entityToDTO(entity : DomainEntity) : DomainDTO

    // Introduce here mapping only when more complex mapping mechanism is needed
    @Mapping(source = "iconId", target = "assetsIconId")
    fun dtoToEntity(dto : DomainDTO) : DomainEntity

    fun entityListToDtoList(entities: List<DomainEntity>): List<DomainDTO>
}