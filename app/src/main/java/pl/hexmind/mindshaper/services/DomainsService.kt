package pl.hexmind.mindshaper.services

import pl.hexmind.mindshaper.database.repositories.DomainRepository
import pl.hexmind.mindshaper.services.dto.DomainDTO
import pl.hexmind.mindshaper.services.mappers.DomainMapper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DomainsService @Inject constructor(
    private val repository : DomainRepository,
    private val mapper : DomainMapper
){
    suspend fun updateDomain(dto : DomainDTO){
        val entity = mapper.toEntity(dto)
        repository.updateDomain(entity)
    }

    suspend fun getAllDomains() : List<DomainDTO>{
        val domains = repository.getAllDomains()
        return mapper.toDTOList(domains)
    }
}
