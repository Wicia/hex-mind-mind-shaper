package pl.hexmind.fastnote.services

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.caverock.androidsvg.SVG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import pl.hexmind.fastnote.R
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable

/**
 * Loader for icons organized by domains in assets/domains/ folder structure
 */
class DomainIconLoader(private val context: Context) {

    private val iconCache = mutableMapOf<Int, Drawable>()
    private var availableIcons: List<Int>? = null

    /**
     * Batch load multiple icons for better performance
     */
    suspend fun loadIconsBatch(iconNumbers: List<Int>): Map<Int, Drawable> {
        return withContext(Dispatchers.IO) {
            val results = mutableMapOf<Int, Drawable>()
            iconNumbers.forEach { number ->
                loadIconWithFallback(number).let { drawable ->
                    results[number] = drawable
                }
            }
            results
        }
    }

    /**
     * Load icon with fallback to default if loading fails
     */
    private suspend fun loadIconWithFallback(iconNumber: Int): Drawable {
        return loadIcon(iconNumber) ?: getDefaultIcon() ?:
        ContextCompat.getDrawable(context, android.R.drawable.ic_menu_gallery)!! // Placeholder
    }

    /**
     * Load icon by number with caching
     */
    suspend fun loadIcon(iconNumber: Int): Drawable? {
        // Return cached version if exists
        iconCache[iconNumber]?.let { return it }

        return withContext(Dispatchers.IO) {
            try {
                loadSVGFromAssets(iconNumber)?.also { drawable ->
                    iconCache[iconNumber] = drawable
                } ?: getDefaultIcon()
            } catch (e: Exception) {
                getDefaultIcon()
            }
        }
    }

    private fun loadSVGFromAssets(iconNumber: Int): Drawable? {
        return try {
            val inputStream = context.assets.open("domains/ic_domain_$iconNumber.svg")
            val svg = SVG.getFromInputStream(inputStream)

            // Set proper size - 64dp for better visibility
            val sizeDp = 64
            val sizePx = (sizeDp * context.resources.displayMetrics.density).toInt()

            // Create bitmap with proper size
            val bitmap = createBitmap(sizePx, sizePx)
            val canvas = android.graphics.Canvas(bitmap)

            // Important: Set SVG document size to match our bitmap
            svg.documentWidth = sizePx.toFloat()
            svg.documentHeight = sizePx.toFloat()

            // Render SVG to fill entire canvas
            svg.renderToCanvas(canvas)

            // Create drawable that will scale properly
            val drawable = bitmap.toDrawable(context.resources)
            drawable.setTargetDensity(context.resources.displayMetrics)
            drawable

        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get all available icon numbers by scanning assets folder
     */
    suspend fun getAvailableIcons(): List<Int> {
        // Return cached if available
        availableIcons?.let { return it }

        return withContext(Dispatchers.IO) {
            try {
                val files = context.assets.list("domains") ?: emptyArray()
                val iconNumbers = files
                    .filter { it.startsWith("ic_domain_") && it.endsWith(".svg") }
                    .mapNotNull { fileName ->
                        // Extract number from ic_domain_X.svg
                        val numberPart = fileName.removePrefix("ic_domain_").removeSuffix(".svg")
                        numberPart.toIntOrNull()
                    }
                    .sorted()

                availableIcons = iconNumbers
                iconNumbers
            } catch (e: IOException) {
                emptyList()
            }
        }
    }

    /**
     * Get random icon number from available icons
     */
    suspend fun getRandomIconNumber(): Int? {
        val icons = getAvailableIcons()
        return if (icons.isNotEmpty()) {
            icons.random()
        } else null
    }

    /**
     * Check if icon with given number exists
     */
    suspend fun iconExists(iconNumber: Int): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                context.assets.open("domains/ic_domain_$iconNumber.svg").use {
                    true
                }
            } catch (e: IOException) {
                false
            }
        }
    }

    /**
     * Get total count of available icons
     */
    suspend fun getIconCount(): Int {
        return getAvailableIcons().size
    }

    /**
     * Get first N icons for quick display
     */
    suspend fun getFirstIcons(count: Int): List<Int> {
        return getAvailableIcons().take(count)
    }

    /**
     * Get default fallback icon
     */
    private fun getDefaultIcon(): Drawable? {
        return ContextCompat.getDrawable(context, R.drawable.ic_domain_default)
    }

    /**
     * TODO: Gdze tego najlepiej użyć?
     * Preload first N icons for better performance
     */
    suspend fun preloadIcons(count: Int = 20) {
        withContext(Dispatchers.IO) {
            val iconsToPreload = getFirstIcons(count)
            iconsToPreload.forEach { iconNumber ->
                loadIcon(iconNumber)
            }
        }
    }

    /**
     * TODO: Gdze tego najlepiej użyć?
     * Clear cache to free memory
     */
    fun clearCache() {
        iconCache.clear()
        availableIcons = null
    }
}