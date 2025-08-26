package pl.hexmind.fastnote.data.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// #TODO when project is getting more & more complex -> improve code by adding Thought DTO + mapper
@Entity(tableName = "DOMAINS")
data class DomainEntity(

    @PrimaryKey
    val id: Int = 0, // TODO: Ref to DomainIdentifier - do sth about it?

    @ColumnInfo(name = "assets_icon_id")
    val assetsIconId: Int,

    @ColumnInfo(name = "custom_name")
    val customName: String? = null,
)