package pl.hexmind.mindshaper.database.repositories

import pl.hexmind.mindshaper.database.models.DomainEntity
import pl.hexmind.mindshaper.database.models.DomainWithIcon
import pl.hexmind.mindshaper.database.models.IconEntity
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DomainRepository @Inject constructor(
    private val domainDAO: DomainDAO
) {
    suspend fun getAllDomains(): List<DomainEntity> {
        return domainDAO.getAllDomains()
    }

    suspend fun getDomainById(id: Int): DomainEntity? {
        return domainDAO.getDomainById(id)
    }

    suspend fun getAllDomainsWithIcons(): List<DomainWithIcon> {
        return domainDAO.getAllDomainsWithIcons()
    }

    suspend fun updateDomain(domain: DomainEntity) {
        domainDAO.updateDomain(domain)
    }

    suspend fun seedDomains(domains: List<DomainEntity>) {
        try {
            domainDAO.clearAll()
            domainDAO.insertAll(domains)
            val newRecordsNumber = domainDAO.getUsedDomainIds().size
            Timber.i("Successfully seeded $newRecordsNumber domains")
        }
        catch (e: Exception) {
            Timber.e(e, "Failed to seed domains")
        }
    }
}