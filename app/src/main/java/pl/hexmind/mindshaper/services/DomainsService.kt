package pl.hexmind.mindshaper.services

import pl.hexmind.mindshaper.common.ui.CommonIconsListItem
import pl.hexmind.mindshaper.database.repositories.DomainRepository
import pl.hexmind.mindshaper.services.dto.DomainDTO
import pl.hexmind.mindshaper.services.mappers.DomainMapper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DomainsService @Inject constructor(
    private val domainRepository : DomainRepository,
    private val iconsService: IconsService
){
    suspend fun updateDomain(dto : DomainDTO){
        val entity = DomainMapper.INSTANCE.dtoToEntity(dto)
        domainRepository.updateDomain(entity)
    }

    suspend fun getAllDomains() : List<DomainDTO>{
        val domains = domainRepository.getAllDomains()
        return DomainMapper.INSTANCE.entityListToDtoList(domains)
    }

    suspend fun getIconIdForDomain(domainId: Int): Int? {
        val domain = domainRepository.getDomainById(domainId) ?: return null
        return domain.assetsIconId
    }

    suspend fun getAllDomainWithIcons() : List<CommonIconsListItem> {
        val domainsWithIcons = domainRepository.getAllDomainsWithIcons()
        val map = domainsWithIcons.map { domainWithIcon ->
            val icon = iconsService.getDrawableByName(domainWithIcon.icon?.drawableName!!)
            val domainId = domainWithIcon.domain.id
            CommonIconsListItem(labelSourceId = domainId!!, iconSourceId = domainWithIcon.icon.id!!, iconDrawable = icon!!, labelText = domainWithIcon.domain.name)
        }
        return map
    }
}