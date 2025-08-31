package pl.hexmind.fastnote.data.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "DOMAINS")
data class DomainEntity(

    @PrimaryKey
    val id: Int = 0,

    @ColumnInfo(name = "assets_icon_id")
    val assetsIconId: Int,

    @ColumnInfo(name = "name")
    val name: String,
)