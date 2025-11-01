package pl.hexmind.mindshaper.database.repositories

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import pl.hexmind.mindshaper.database.models.IconEntity

@Dao
interface IconDAO {

    @Query("SELECT * FROM icons")
    suspend fun getAllIcons(): List<IconEntity>

    @Query("SELECT id FROM icons")
    suspend fun getAllIconIds(): List<Int>

    @Query("SELECT * FROM icons WHERE id = :iconId")
    suspend fun getIconById(iconId: Int): IconEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIcons(icons: List<IconEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(icons: List<IconEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIcon(icon: IconEntity): Long

    @Query("DELETE FROM icons")
    suspend fun clearAll()
}