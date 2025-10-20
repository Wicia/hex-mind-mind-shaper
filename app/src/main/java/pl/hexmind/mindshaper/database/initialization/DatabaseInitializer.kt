package pl.hexmind.mindshaper.database.initialization

import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.database.AppDatabase
import pl.hexmind.mindshaper.database.models.DomainEntity
import pl.hexmind.mindshaper.database.models.IconEntity
import pl.hexmind.mindshaper.database.repositories.DomainRepository
import pl.hexmind.mindshaper.database.repositories.IconRepository
import pl.hexmind.mindshaper.services.AppSettingsStorage
import timber.log.Timber
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

    /**
     * Creates list of IconEntity from drawable resources with "ic_domain" prefix
     */
    private fun createIconsList(): List<IconEntity> {
        val iconsEntities = mutableListOf<IconEntity>()

        try {
            // Using reflection to retrieve all properties/fields from R.drawable
            val drawableClass = R.drawable::class.java
            val fields = drawableClass.fields

            val iconFields = fields
                .filter { it.name.startsWith("z_ic_domain") }
                .sortedBy { it.name }

            Timber.d("Found ${iconFields.size} icon resources with prefix 'ic_domain'")

            iconFields.forEach { field ->
                try {
                    val drawableName = field.name
                    val resId = field.getInt(null) // Getting Resource ID

                    if (resId != 0) {
                        iconsEntities.add(IconEntity(drawableName = drawableName))
                        Timber.d("Added icon: $drawableName (resId: $resId)")
                    } else {
                        Timber.w("Invalid resource ID for: $drawableName")
                    }
                } catch (e: Exception) {
                    Timber.w(e, "Failed to process drawable field: ${field.name}")
                }
            }

        } catch (e: Exception) {
            Timber.e(e, "Failed to load drawable resources via reflection")
        }

        if (iconsEntities.isEmpty()) {
            Timber.w("No icons found with prefix 'ic_domain' in drawable resources!")
        }

        return iconsEntities
    }
}