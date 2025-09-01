package pl.hexmind.fastnote.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import pl.hexmind.fastnote.R
import pl.hexmind.fastnote.data.models.DomainEntity
import pl.hexmind.fastnote.data.repositories.DomainDAO
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for initializing database on app startup
 */
@Singleton
class DatabaseInitializer @Inject constructor(
    private val domainDAO: DomainDAO,
    @ApplicationContext private val context : Context
) {

    // Initialize database with default domains if needed
    suspend fun initializeIfNeeded() {
        if (domainDAO.getCount() > 0) return

        val defaultDomains = context.resources.getStringArray(R.array.settings_domains_default_names_list)
        val entities: MutableList<DomainEntity> = mutableListOf()

        for (domainId in defaultDomains.indices) {
            entities.add(DomainEntity(name = defaultDomains[domainId], assetsIconId = domainId))
        }

        domainDAO.insertAll(entities)
    }
}