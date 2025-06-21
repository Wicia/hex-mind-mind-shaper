package pl.hexmind.fastnote.data.repositories

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

import pl.hexmind.fastnote.data.models.AreaEntity
import pl.hexmind.fastnote.data.models.AreaIdentifier

@Dao
interface AreaDAO {

    @Query("SELECT * FROM areas ORDER BY id ASC")
    suspend fun getAllAreas(): List<AreaEntity>

    @Query("SELECT * FROM areas WHERE id = :id")
    suspend fun getAreaById(id: Int): AreaEntity?

    suspend fun getAreaByIdentifier(identifier: AreaIdentifier): AreaEntity? {
        return getAreaById(identifier.value)
    }

    @Query("SELECT id FROM areas")
    suspend fun getUsedAreaIds(): List<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArea(area: AreaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(areas: List<AreaEntity>)

    @Update
    suspend fun updateArea(area: AreaEntity)

    @Delete
    suspend fun deleteArea(area: AreaEntity)

    suspend fun getNextAvailableAreaId(): AreaIdentifier? {
        val usedIds = getUsedAreaIds()
        return AreaIdentifier.availableAreas()
            .firstOrNull { it.value !in usedIds }
    }

    @Query("SELECT COUNT(*) FROM AREAS")
    suspend fun getCount(): Int

    suspend fun isAreaIdAvailable(identifier: AreaIdentifier): Boolean {
        return identifier.value >= 0 && getAreaById(identifier.value) == null
    }
}