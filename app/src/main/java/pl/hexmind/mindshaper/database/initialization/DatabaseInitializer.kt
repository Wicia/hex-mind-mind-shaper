package pl.hexmind.mindshaper.database.initialization

import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.database.AppDatabase
import pl.hexmind.mindshaper.database.models.DomainEntity
import pl.hexmind.mindshaper.database.models.IconEntity
import pl.hexmind.mindshaper.database.repositories.DomainRepository
import pl.hexmind.mindshaper.database.repositories.IconRepository
import pl.hexmind.mindshaper.services.AppSettingsStorage
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for initializing database on app startup
 */
@Singleton
class DatabaseInitializer @Inject constructor(
    private val domainRepository: DomainRepository,
    private val iconRepository: IconRepository,
    private val storage : AppSettingsStorage
) {

    suspend fun initializeIfNeeded() {
        val storedDBVersion = storage.getCurrentDBVersion()
        val loadedDBVersion = AppDatabase.Companion.DB_VERSION

        if (storedDBVersion != loadedDBVersion) {
            storage.setCurrentDBVersion(loadedDBVersion)
            seedIcons()
            seedDomains()
        }
    }

    suspend fun seedDomains(){
        val context = storage.getApplicationContext()

        val defaultDomains = context.resources.getStringArray(R.array.settings_domains_default_names_list)
        val entities: MutableList<DomainEntity> = mutableListOf()

        for (domainId in defaultDomains.indices) {
            entities.add(
                DomainEntity(
                    name = defaultDomains[domainId],
                    assetsIconId = null
                )
            )
        }

        domainRepository.seedDomains(entities)
    }

    suspend fun seedIcons() {
        try {
            val iconsList = createIconsList()
            iconRepository.seedIcons(iconsList)
            Timber.Forest.i("Database seeded with ${iconsList.size} icons")
        }
        catch (e: Exception) {
            Timber.Forest.e(e, "Failed to seed icons")
        }
    }

    private fun createIconsList(): List<IconEntity> {
        val iconsEntities = mutableListOf<IconEntity>()
        val filesNames = getAvailableIconFiles()
        iconsEntities.addAll(assetToByteData(filesNames))

        return iconsEntities
    }

    fun getAvailableIconFiles(): List<String> {
        val context = storage.getApplicationContext()
        try {
            val files = context.assets.list("domains") ?: emptyArray()
            val iconNumbers = files
                .filter {
                    it.startsWith("ic_domain_") && it.endsWith(".svg")
                }
                .sorted()

            return iconNumbers
        }
        catch (e: IOException) {
            return emptyList()
        }
    }

    private fun assetToByteData(fileNames: List<String>): List<IconEntity> {
        return fileNames.map { fileName ->
            try {
                val context = storage.getApplicationContext()
                val inputStream = context.assets.open("domains/$fileName")
                val iconData = inputStream.readBytes()
                inputStream.close()

                IconEntity(
                    iconData = iconData
                )
            }
            catch (e: Exception) {
                Timber.Forest.w(e, "Failed to load icon: $fileName")
                return mutableListOf()
            }
        }
    }
}