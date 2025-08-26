package pl.hexmind.fastnote.data.repositories

import pl.hexmind.fastnote.data.models.DomainEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DomainRepository @Inject constructor(
    private val domainDAO: DomainDAO
) {

    suspend fun getAllDomains(): List<DomainEntity> {
        return domainDAO.getAllDomains()
    }

    suspend fun getDomainById(id: Long): DomainEntity? {
        return domainDAO.getDomainById(id)
    }

    suspend fun insertDomain(domain: DomainEntity) {
        domainDAO.insertDomain(domain)
    }

    suspend fun updateDomain(domain: DomainEntity) {
        domainDAO.updateDomain(domain)
    }

    suspend fun deleteDomain(domain: DomainEntity) {
        domainDAO.deleteDomain(domain)
    }
}