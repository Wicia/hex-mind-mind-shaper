package pl.hexmind.mindshaper.services.mappers

import pl.hexmind.mindshaper.database.models.DomainEntity
import pl.hexmind.mindshaper.services.dto.DomainDTO
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DomainMapper @Inject constructor(){

    fun toEntity(dto: DomainDTO): DomainEntity =
        DomainEntity(
            id = dto.id,
            assetsIconId = dto.assetImageId,
            name = dto.name
        )

    fun toDTO(entity: DomainEntity): DomainDTO =
        DomainDTO(
            id = entity.id,
            name = entity.name,
            assetImageId = entity.assetsIconId
        )

    fun toEntityList(dtos: List<DomainDTO>): List<DomainEntity> =
        dtos.map { toEntity(it) }

    fun toDTOList(entities: List<DomainEntity>): List<DomainDTO> =
        entities.map { toDTO(it) }
}