package pl.hexmind.mindshaper.services

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.collection.LruCache
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import pl.hexmind.mindshaper.database.repositories.IconRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IconsService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: IconRepository
) {

    // ! Simple cache for 40 icons + buffer
    private val iconCache = LruCache<Int, Drawable>(50)

    suspend fun getAvailableIconsIds(): List<Int> {
        return try {
            if (iconCache.size() == 0) {
                preloadAllIcons()
            }
            return iconCache.snapshot().keys.toList()
        }
        catch (e: Exception) {
            Timber.e(e, "Failed to get available icon IDs")
            emptyList()
        }
    }

    /**
     * Batch load multiple icons for better performance
     */
    fun loadIconsBatch(iconNumbers: List<Int>): Map<Int, Drawable> {
        val results = mutableMapOf<Int, Drawable>()
        iconNumbers.forEach { number ->
            results[number] = iconCache[number]!!
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
                    val drawable = getDrawableByName(iconEntity.drawableName)
                    drawable?.let { iconCache.put(id, it) }
                }
            }
        }
        catch (e: Exception) {
            Timber.e(e, "Failed to preload icons")
        }
    }

    suspend fun getDrawableIcon(id: Int): Drawable {
        // Check cache first
        iconCache[id]?.let { return it }

        // Load from database if not cached
        val iconEntity = repository.getIconById(id)
        iconEntity?.let { entity ->
            val drawable = getDrawableByName(entity.drawableName)
            drawable?.let {
                iconCache.put(id, it)
                return it
            }
        }

        // Return default if failed
        return getDefaultIcon()
    }

    private fun getDefaultIcon(): Drawable {
        return ContextCompat.getDrawable(context, android.R.drawable.ic_menu_info_details)
            ?: throw IllegalStateException("Cannot load default icon")
    }

    fun getDrawableByName(drawableName: String): Drawable? {
        return try {
            val resId = context.resources.getIdentifier(
                drawableName,
                "drawable",
                context.packageName
            )

            if (resId == 0) {
                Timber.w("Drawable resource not found: $drawableName")
                return null
            }

            return ResourcesCompat.getDrawable(context.resources, resId, context.theme)

        } catch (e: Exception) {
            Timber.e(e, "Failed to load drawable: $drawableName")
            null
        }
    }
}