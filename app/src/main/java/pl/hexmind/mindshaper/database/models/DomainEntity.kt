package pl.hexmind.mindshaper.database.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "DOMAINS")
data class DomainEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,

    @ColumnInfo(name = "assets_icon_id")
    val assetsIconId: Int,

    @ColumnInfo(name = "name")
    val name: String,
)