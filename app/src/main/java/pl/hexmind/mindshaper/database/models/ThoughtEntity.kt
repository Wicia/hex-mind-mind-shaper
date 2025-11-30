package pl.hexmind.mindshaper.database.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Index
import java.time.Instant

@Entity(
    tableName = "THOUGHTS",
    foreignKeys = [
        ForeignKey(
            entity = DomainEntity::class,
            parentColumns = ["id"],
            childColumns = ["domain_id"],
            onDelete = ForeignKey.SET_NULL // ! When child entity is deleted = don't delete this (parent) entity
        )
    ],
    indices = [Index(value = ["domain_id"])]
)
data class ThoughtEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,

    @ColumnInfo(name = "domain_id")
    val domainId: Int?,

    @ColumnInfo(name = "thread")
    val thread: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant = Instant.now(),

    @ColumnInfo(name = "rich_text")
    val richText: String? = null,

    @ColumnInfo(name = "soul_mate")
    val soulMate: String? = null,

    @ColumnInfo(name = "project")
    val project: String? = null,

    @ColumnInfo(name = "value")
    val value: Int = 1,

    @ColumnInfo(name = "audio_data", typeAffinity = ColumnInfo.BLOB)
    val audioData: ByteArray? = null,

    @ColumnInfo(name = "audio_duration_ms")
    val audioDurationMs: Long? = null

) {
    // ! Needed for ByteArray in data class
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ThoughtEntity

        if (id != other.id) return false
        if (domainId != other.domainId) return false
        if (thread != other.thread) return false
        if (createdAt != other.createdAt) return false
        if (richText != other.richText) return false
        if (soulMate != other.soulMate) return false
        if (project != other.project) return false
        if (value != other.value) return false
        if (audioData != null) {
            if (other.audioData == null) return false
            if (!audioData.contentEquals(other.audioData)) return false
        } else if (other.audioData != null) return false
        if (audioDurationMs != other.audioDurationMs) return false

        return true
    }

    // ! Needed for ByteArray in data class
    override fun hashCode(): Int {
        var result = id ?: 0
        result = 31 * result + (domainId ?: 0)
        result = 31 * result + (thread?.hashCode() ?: 0)
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + (richText?.hashCode() ?: 0)
        result = 31 * result + (soulMate?.hashCode() ?: 0)
        result = 31 * result + (project?.hashCode() ?: 0)
        result = 31 * result + value
        result = 31 * result + (audioData?.contentHashCode() ?: 0)
        result = 31 * result + (audioDurationMs?.hashCode() ?: 0)
        return result
    }
}