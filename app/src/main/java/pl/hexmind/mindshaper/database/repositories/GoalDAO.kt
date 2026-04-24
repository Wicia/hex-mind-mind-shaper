package pl.hexmind.mindshaper.database.repositories

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import pl.hexmind.mindshaper.database.models.GoalEntity
import pl.hexmind.mindshaper.database.models.GoalWithGuidelines

@Dao
interface GoalDAO {

    @Transaction
    @Query("SELECT * FROM GOALS ORDER BY priority ASC, last_modified_at DESC")
    suspend fun getAllWithGuidelines(): List<GoalWithGuidelines>

    // Needed for efficient single-entity updates (avoid full table scan)
    @Query("SELECT * FROM GOALS WHERE id = :goalId")
    suspend fun getById(goalId: Int): GoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: GoalEntity): Long

    @Update
    suspend fun update(goal: GoalEntity)

    @Query("DELETE FROM GOALS WHERE id = :goalId")
    suspend fun deleteById(goalId: Int)

    // === BACKUP management (snapshot restore) ===

    @Query("SELECT * FROM GOALS")
    suspend fun getAllGoals(): List<GoalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(goals: List<GoalEntity>)

    @Query("DELETE FROM GOALS")
    suspend fun clearAll()
}