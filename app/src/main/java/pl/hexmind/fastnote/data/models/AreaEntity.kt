package pl.hexmind.fastnote.data.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// #TODO when project is getting more & more complex -> improve code by adding Thought DTO + mapper
@Entity(tableName = "AREAS")
data class AreaEntity(

    @PrimaryKey
    val id: Int = 0,

    @ColumnInfo(name = "reference")
    val reference: String,

    @ColumnInfo(name = "custom_name")
    val customName: String? = null,
)