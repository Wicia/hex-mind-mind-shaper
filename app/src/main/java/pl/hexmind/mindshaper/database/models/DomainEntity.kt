package pl.hexmind.mindshaper.database.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "DOMAINS",
    foreignKeys = [
        ForeignKey(
            entity = IconEntity::class,
            parentColumns = ["id"],
            childColumns = ["assets_icon_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["assets_icon_id"])]
)
data class DomainEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,

    @ColumnInfo(name = "assets_icon_id")
    val assetsIconId: Int? = null,

    @ColumnInfo(name = "name")
    val name: String,
)