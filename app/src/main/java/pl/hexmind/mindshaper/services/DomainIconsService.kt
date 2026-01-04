package pl.hexmind.mindshaper.services

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.database.repositories.IconRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DomainIconsService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: IconRepository
) {

    // ! Simple cache for icons
    private val iconCache = HashMap<Int, Int>() // iconId -> resourceId

    suspend fun getAvailableIconsIds(): List<Int> {
        return try {
            if (iconCache.isEmpty()) {
                preloadAllIcons()
            }
            iconCache.keys.toList()
        }
        catch (e: Exception) {
            Timber.e(e, "Failed to get available icon IDs")
            emptyList()
        }
    }

    /**
     * Batch load multiple icons for better performance
     */
    fun loadIconsBatch(iconNumbers: List<Int>): Map<Int, Int> {
        val results = mutableMapOf<Int, Int>()
        iconNumbers.forEach { number ->
            iconCache[number]?.let { resourceId ->
                results[number] = resourceId
            }
        }
        return results
    }

    /**
     * Catching all icons at app startup
     */
    suspend fun preloadAllIcons() {
        try {
            val allIcons = repository.getAllIcons()

            for (iconEntity in allIcons) {
                iconEntity.id?.let { id ->
                    val resourceId = getResourceIdByName(iconEntity.drawableName)
                    if (resourceId != 0) {
                        iconCache[id] = resourceId
                    }
                }
            }
        }
        catch (e: Exception) {
            Timber.e(e, "Failed to preload icons")
        }
    }

    suspend fun getIconResourceId(id: Int): Int {
        // Check cache first
        iconCache[id]?.let { return it }

        // Load from database if not cached
        val iconEntity = repository.getIconById(id)
        iconEntity?.let { entity ->
            val resourceId = getResourceIdByName(entity.drawableName)
            if (resourceId != 0) {
                iconCache[id] = resourceId
                return resourceId
            }
        }

        // Return default if failed
        return R.drawable.ic_domain_none
    }

    fun getResourceIdByName(drawableName: String): Int {
        return try {
            val resId = context.resources.getIdentifier(
                drawableName,
                "drawable",
                context.packageName
            )

            if (resId == 0) {
                Timber.w("Drawable resource not found: $drawableName")
            }
            resId
        }
        catch (e: Exception) {
            Timber.e(e, "Failed to get resource ID: $drawableName")
            0
        }
    }
}