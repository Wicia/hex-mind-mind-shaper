package pl.hexmind.mindshaper.database.repositories

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import pl.hexmind.mindshaper.database.models.StepEntity

@Dao
interface StepDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(step: StepEntity): Long

    @Update
    suspend fun update(step: StepEntity)

    @Query("DELETE FROM GOAL_STEPS WHERE id = :stepId")
    suspend fun deleteById(stepId: Int)

    @Query("SELECT * FROM GOAL_STEPS WHERE goal_id = :goalId ORDER BY position ASC")
    suspend fun getByGoalId(goalId: Int): List<StepEntity>

    // Needed for efficient single-entity updates
    @Query("SELECT * FROM GOAL_STEPS WHERE id = :stepId")
    suspend fun getById(stepId: Int): StepEntity?

    @Query("UPDATE GOAL_STEPS SET position = :position WHERE id = :id")
    suspend fun updatePosition(id: Int, position: Int)

    // #! Business rule — only 0 or 1 step ever has linked thought with given id
    @Query("SELECT * FROM GOAL_STEPS WHERE thought_id = :thoughtId LIMIT 1")
    suspend fun findByThoughtId(thoughtId: Int): StepEntity?

    // === BACKUP management (snapshot restore) ===

    @Query("SELECT * FROM GOAL_STEPS")
    suspend fun getAllSteps(): List<StepEntity>

    @Query("DELETE FROM GOAL_STEPS")
    suspend fun clearAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(steps: List<StepEntity>)
}
