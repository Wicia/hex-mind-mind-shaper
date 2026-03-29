package pl.hexmind.mindshaper.database.repositories

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import pl.hexmind.mindshaper.database.models.PathStepEntity

@Dao
interface PathStepDAO {

    @Query("SELECT * FROM PATH_STEPS WHERE path_key = :pathKey ORDER BY position ASC")
    suspend fun getStepsByPath(pathKey: String): List<PathStepEntity>

    @Query("SELECT COUNT(*) FROM PATH_STEPS WHERE path_key = :pathKey")
    suspend fun countByPath(pathKey: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(steps: List<PathStepEntity>)
}