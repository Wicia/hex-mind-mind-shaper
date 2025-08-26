package pl.hexmind.fastnote.data

import pl.hexmind.fastnote.data.models.DomainEntity
import pl.hexmind.fastnote.data.models.DomainIdentifier
import pl.hexmind.fastnote.data.repositories.DomainDAO
import javax.inject.Inject
import javax.inject.Singleton

// Service for initializing database on app startup
@Singleton
class DatabaseInitializer @Inject constructor(
    private val domainDAO: DomainDAO
) {

    // Initialize database with default domains if needed
    suspend fun initializeIfNeeded() {
        if (domainDAO.getCount() > 0) return

        val entities: MutableList<DomainEntity> = mutableListOf()
        val availableIdentifiers = DomainIdentifier.availableIdentifiers()
        for (domainId in availableIdentifiers) {
            entities.add(DomainEntity(assetsIconId = domainId))
        }

        domainDAO.insertAll(entities)
    }
}