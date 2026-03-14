package pl.hexmind.mindshaper.database.repositories

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import pl.hexmind.mindshaper.database.models.GuidelineEntity

@Dao
interface GuidelineDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(guideline: GuidelineEntity): Long

    @Update
    suspend fun update(guideline: GuidelineEntity)

    @Query("DELETE FROM GOAL_GUIDELINES WHERE id = :guidelineId")
    suspend fun deleteById(guidelineId: Int)

    @Query("SELECT * FROM GOAL_GUIDELINES WHERE goal_id = :goalId ORDER BY position ASC")
    suspend fun getByGoalId(goalId: Int): List<GuidelineEntity>

    // Needed for efficient single-entity updates
    @Query("SELECT * FROM GOAL_GUIDELINES WHERE id = :guidelineId")
    suspend fun getById(guidelineId: Int): GuidelineEntity?

    @Query("UPDATE GOAL_GUIDELINES SET position = :position WHERE id = :id")
    suspend fun updatePosition(id: Int, position: Int)
}