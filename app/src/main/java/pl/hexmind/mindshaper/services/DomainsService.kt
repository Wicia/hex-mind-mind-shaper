package pl.hexmind.mindshaper.services

import android.graphics.drawable.Drawable
import pl.hexmind.mindshaper.common.ui.CommonIconsListItem
import pl.hexmind.mindshaper.database.repositories.DomainRepository
import pl.hexmind.mindshaper.services.dto.DomainDTO
import pl.hexmind.mindshaper.services.mappers.DomainMapper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DomainsService @Inject constructor(
    private val domainRepository : DomainRepository,
    private val domainIconsService: DomainIconsService
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
            // Get specified or default domain icon
            val iconDrawable: Drawable? = when {
                domainWithIcon.icon != null -> {
                    domainIconsService.getDrawableByName(domainWithIcon.icon.drawableName)
                }
                else -> {
                    domainIconsService.getDefaultIcon()
                }
            }
            val iconId : Int? = when {
                domainWithIcon.icon != null -> {
                    domainWithIcon.icon.id
                }
                else -> {
                    null
                }
            }

            val domainId = domainWithIcon.domain.id
            CommonIconsListItem(
                iconDrawable = iconDrawable!!,
                iconEntityId = iconId,
                labelText = domainWithIcon.domain.name,
                labelEntityId = domainId
            )
        }
        return map
    }
}