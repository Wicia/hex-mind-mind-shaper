package pl.hexmind.mindshaper.database.repositories

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import pl.hexmind.mindshaper.database.models.GoalEntity
import pl.hexmind.mindshaper.database.models.GoalWithSteps

@Dao
interface GoalDAO {

    // ! @Relation runs two queries (1 goal -> 2 its steps) - @Transaction keeps them one snapshot

    @Transaction
    @Query("SELECT * FROM GOALS WHERE status = 'ARCHIVED' ORDER BY last_modified_at DESC")
    suspend fun getArchivedWithSteps(): List<GoalWithSteps>

    @Transaction
    @Query("SELECT * FROM GOALS WHERE status = 'ACTIVE' ORDER BY importance DESC, last_modified_at DESC")
    suspend fun getAllWithSteps(): List<GoalWithSteps>

    // Needed for efficient single-entity updates (avoid full table scan)
    @Query("SELECT * FROM GOALS WHERE id = :goalId")
    suspend fun getById(goalId: Int): GoalEntity?

    // Status-agnostic on purpose
    @Transaction
    @Query("SELECT * FROM GOALS WHERE id = :goalId")
    suspend fun getByIdWithSteps(goalId: Int): GoalWithSteps?

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
