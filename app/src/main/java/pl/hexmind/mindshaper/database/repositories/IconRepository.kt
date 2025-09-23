package pl.hexmind.mindshaper.database.repositories

import android.database.sqlite.SQLiteConstraintException
import androidx.lifecycle.LiveData
import pl.hexmind.mindshaper.database.models.IconEntity
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing icon data operations
 */
@Singleton
class IconRepository @Inject constructor(
    private val iconDao: IconDAO
) {

    suspend fun getAllIcons(): List<IconEntity> {
        try {
            val icons = iconDao.getAllIcons()
            Timber.i("Successfully retrieved ${icons.size} icons")
            return icons
        }
        catch (e: Exception) {
            Timber.e(e, "Failed to get all icons")
            return emptyList()
        }
    }

    suspend fun getIconById(iconId: Int): IconEntity? {
        return try {
            iconDao.getIconById(iconId)
        }
        catch (e: Exception) {
            Timber.e(e, "Failed to get icon by ID: $iconId")
            return null
        }
    }

    suspend fun seedIcons(icons: List<IconEntity>) {
        try {
            iconDao.clearAll()
            iconDao.insertIcons(icons)
            val newRecordsNumber = iconDao.getAllIconIds().size
            Timber.i("Successfully seeded $newRecordsNumber icons")
        }
        catch (e: Exception) {
            Timber.e(e, "Failed to seed icons")
        }
    }
}