package pl.hexmind.mindshaper.database.models

import androidx.room.Embedded
import androidx.room.Relation

data class DomainWithIcon(
    @Embedded
    val domain: DomainEntity,

    @Relation(
        parentColumn = "assets_icon_id",
        entityColumn = "id"
    )
    val icon: IconEntity?
)