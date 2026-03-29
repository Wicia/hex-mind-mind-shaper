package pl.hexmind.mindshaper.database.repositories

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import pl.hexmind.mindshaper.database.models.PathEntity
import pl.hexmind.mindshaper.database.models.PathWithSteps

@Dao
interface PathDAO {

    // Today's active paths
    @Transaction
    @Query("""
        SELECT * FROM PATHS
        WHERE last_drawn_date = :today
          AND status != 'COMPLETED'
        ORDER BY path_key ASC
    """)
    suspend fun getActiveTodayWithSteps(today: Long): List<PathWithSteps>

    // Random path eligible for pick
    @Query("""
        SELECT * FROM PATHS
        WHERE status != 'COMPLETED'
          AND (last_drawn_date IS NULL OR last_drawn_date < :today)
        ORDER BY RANDOM()
        LIMIT 1
    """)
    suspend fun getRandomPickable(today: Long): PathEntity?

    @Query("SELECT * FROM PATHS WHERE path_key = :pathKey")
    suspend fun getByKey(pathKey: String): PathEntity?

    @Update
    suspend fun update(path: PathEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(paths: List<PathEntity>)

    @Query("SELECT COUNT(*) FROM PATHS")
    suspend fun count(): Int
}