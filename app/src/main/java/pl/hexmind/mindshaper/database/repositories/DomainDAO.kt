package pl.hexmind.mindshaper.database.repositories

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import pl.hexmind.mindshaper.database.models.DomainEntity
import pl.hexmind.mindshaper.database.models.DomainWithIcon

@Dao
interface DomainDAO {

    @Query("SELECT * FROM domains ORDER BY id ASC")
    suspend fun getAllDomains(): List<DomainEntity>

    @Query("SELECT * FROM domains WHERE id = :id")
    suspend fun getDomainById(id: Int): DomainEntity?

    @Transaction
    @Query("SELECT * FROM DOMAINS")
    suspend fun getAllDomainsWithIcons(): List<DomainWithIcon>

    @Query("SELECT id FROM domains")
    suspend fun getUsedDomainIds(): List<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDomain(domain: DomainEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(icons: List<DomainEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(domains: List<DomainEntity>)

    @Update
    suspend fun updateDomain(domain: DomainEntity)

    @Delete
    suspend fun deleteDomain(domain: DomainEntity)

    @Query("SELECT COUNT(*) FROM domains")
    suspend fun getCount(): Int

    @Query("DELETE FROM domains")
    suspend fun clearAll()
}