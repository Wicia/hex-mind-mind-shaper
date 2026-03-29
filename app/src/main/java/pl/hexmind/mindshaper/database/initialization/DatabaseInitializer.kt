package pl.hexmind.mindshaper.database.initialization

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.database.AppDatabase
import pl.hexmind.mindshaper.database.models.DomainEntity
import pl.hexmind.mindshaper.database.models.IconEntity
import pl.hexmind.mindshaper.database.models.PathEntity
import pl.hexmind.mindshaper.database.models.PathStepEntity
import pl.hexmind.mindshaper.database.repositories.DomainRepository
import pl.hexmind.mindshaper.database.repositories.IconRepository
import pl.hexmind.mindshaper.database.repositories.PathRepository
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
    private val pathRepository: PathRepository,
    private val storage: AppSettingsStorage
) {

    companion object {
        private const val DOMAIN_ICON_PREFIX = "z_ic_domain"
    }

    suspend fun initializeIfNeeded() {
        // only during first app launch perform db setup/seeding
        if (storage.getCurrentDBVersion() == -1) { // TODO - to be replaced with better seeding mechanism
            storage.setCurrentDBVersion(AppDatabase.DB_VERSION)
            seedIcons()
            seedDomains()
        }
        // Paths seeded separately — idempotent, handles app updates
        seedPaths()
    }

    suspend fun seedDomains() {
        val context = storage.getApplicationContext()
        val defaultDomains = context.resources.getStringArray(R.array.settings_domains_default_names_list)
        val entities: MutableList<DomainEntity> = mutableListOf()

        for (domainId in defaultDomains.indices) {
            entities.add(DomainEntity(name = defaultDomains[domainId], assetsIconId = null))
        }

        domainRepository.seedDomains(entities)
    }

    suspend fun seedIcons() {
        try {
            val iconsList = createIconsList()
            iconRepository.seedIcons(iconsList)
            Timber.i("Database seeded with ${iconsList.size} icons")
        }
        catch (e: Exception) {
            Timber.e(e, "Failed to seed icons")
        }
    }

    suspend fun seedPaths() {
        // TODO: To be applied extended mechanism for comparing json <-> DB state and updating if needed (path = #hash)
        val pathSeeds = loadPathsFromJson() ?: run {
            Timber.e("Failed to load paths seed from JSON — skipping seed")
            return
        }

        pathSeeds.forEach { pathSeed ->
            // skip if this key already exists — idempotent per-entry
            if (pathRepository.getPathByKey(pathSeed.key) != null) return@forEach

            pathRepository.insertPaths(
                listOf(PathEntity(pathKey = pathSeed.key, category = pathSeed.category))
            )
            pathRepository.insertSteps(
                pathSeed.steps.mapIndexed { index, content ->
                    PathStepEntity(pathKey = pathSeed.key, position = index, content = content)
                }
            )
        }

        Timber.i("Paths seeded from JSON: ${pathSeeds.size} paths")
    }

    /**
     * Reads res/raw/paths_seed.json and parses it into a list of seed objects.
     *
     * @return null if the file is missing or malformed.
     */
    private fun loadPathsFromJson(): List<PathSeedEntry>? {
        return try {
            val context = storage.getApplicationContext()
            val json = context.resources
                .openRawResource(R.raw.paths_seed)
                .bufferedReader()
                .use { it.readText() }

            val type = object : TypeToken<List<PathSeedEntry>>() {}.type
            Gson().fromJson(json, type)
        }
        catch (e: Exception) {
            Timber.e(e, "Failed to parse paths_seed.json")
            null
        }
    }

    /**
     * Local model matching the JSON structure (for seeding).
     */
    private data class PathSeedEntry(
        @SerializedName("key") val key: String,
        @SerializedName("category") val category: String,
        @SerializedName("steps") val steps: List<String>
    )

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
                .filter { it.name.startsWith(DOMAIN_ICON_PREFIX) }
                .sortedBy { it.name }

            Timber.d("Found ${iconFields.size} icon resources with prefix 'ic_domain'")

            iconFields.forEach { field ->
                try {
                    val drawableName = field.name
                    val resId = field.getInt(null) // Getting Resource ID
                    if (resId != 0) {
                        iconsEntities.add(IconEntity(drawableName = drawableName))
                        Timber.d("Added icon: $drawableName (resId: $resId)")
                    }
                    else {
                        Timber.w("Invalid resource ID for: $drawableName")
                    }
                }
                catch (e: Exception) {
                    Timber.w(e, "Failed to process drawable field: ${field.name}")
                }
            }
        }
        catch (e: Exception) {
            Timber.e(e, "Failed to load drawable resources via reflection")
        }

        if (iconsEntities.isEmpty()) {
            Timber.w("No icons found with prefix 'ic_domain' in drawable resources!")
        }

        return iconsEntities
    }
}