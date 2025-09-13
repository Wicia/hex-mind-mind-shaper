package pl.hexmind.fastnote.services

import pl.hexmind.fastnote.database.repositories.DomainRepository
import pl.hexmind.fastnote.services.dto.DomainDTO
import pl.hexmind.fastnote.services.mappers.DomainMapper
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
