package pl.hexmind.fastnote.data

import androidx.room.Room
import pl.hexmind.fastnote.R
import pl.hexmind.fastnote.data.models.DomainEntity
import pl.hexmind.fastnote.data.repositories.DomainDAO
import pl.hexmind.fastnote.services.AppSettingsStorage
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for initializing database on app startup
 */
@Singleton
class DatabaseInitializer @Inject constructor(
    private val domainDAO: DomainDAO,
    private val storage : AppSettingsStorage
) {

    // Initialize database with default domains if needed
    suspend fun initializeIfNeeded() {
        storage.setCurrentDBVersion(9)
        val context = storage.getApplicationContext()
        val storedDBVersion = storage.getCurrentDBVersion()
        val loadedDBVersion = AppDatabase.DB_VERSION

        if (storedDBVersion != loadedDBVersion) {
            storage.setCurrentDBVersion(loadedDBVersion)

            domainDAO.clearAll()
            val defaultDomains = context.resources.getStringArray(R.array.settings_domains_default_names_list)
            val entities: MutableList<DomainEntity> = mutableListOf()

            for (domainId in defaultDomains.indices) {
                entities.add(DomainEntity(name = defaultDomains[domainId], assetsIconId = domainId))
            }

            domainDAO.insertAll(entities)
        }
    }
}