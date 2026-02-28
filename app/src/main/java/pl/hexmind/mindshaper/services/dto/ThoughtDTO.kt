package pl.hexmind.mindshaper.services.dto

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import pl.hexmind.mindshaper.common.intent.InstantParceler
import pl.hexmind.mindshaper.activities.capture.models.ThoughtMainContentType
import java.time.Instant

@Parcelize
@TypeParceler<Instant?, InstantParceler>
data class ThoughtDTO(

    var id: Int? = null,
    var createdAt: Instant = Instant.now(),
    var updatedAt: Instant = Instant.now(),

    // Links
    var domainId: Int? = null,
    var thread: String? = null,
    var soulMate: String? = null,
    var project: String? = null,

    var value: Int = 1,

    var mainContentType: ThoughtMainContentType = ThoughtMainContentType.UNKNOWN,

// ========= RICH TEXT =========

    var richText: String? = null,

// ========= VOICE RECORDING =========

    // only light data here (like metadata & no byte arrays)
    var audioDurationMs: Long? = null,

    @Transient
    var tempAudioFilePath: String? = null, // ! Used ONLY during recording

// ========= PHOTO =========

    var photoFileSize: Long? = null,

    @Transient
    var tempPhotoFilePath: String? = null

) : Parcelable {

    val hasPhoto: Boolean
        get() = (photoFileSize ?: 0) > 0

    val duration: Long?
        get() = audioDurationMs

    val hasAudio: Boolean
        get() = (audioDurationMs ?: 0) > 0

    val hasText: Boolean
        get() = !richText.isNullOrBlank()

    // ! Needed for ByteArray in data class
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ThoughtDTO

        if (id != other.id) return false
        if (createdAt != other.createdAt) return false
        if (updatedAt != other.updatedAt) return false
        if (domainId != other.domainId) return false
        if (thread != other.thread) return false
        if (richText != other.richText) return false
        if (soulMate != other.soulMate) return false
        if (project != other.project) return false
        if (value != other.value) return false
        if (mainContentType != other.mainContentType) return false
        if (audioDurationMs != other.audioDurationMs) return false
        if (tempAudioFilePath != other.tempAudioFilePath) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id ?: 0
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + updatedAt.hashCode()
        result = 31 * result + (domainId ?: 0)
        result = 31 * result + (thread?.hashCode() ?: 0)
        result = 31 * result + (richText?.hashCode() ?: 0)
        result = 31 * result + (soulMate?.hashCode() ?: 0)
        result = 31 * result + (project?.hashCode() ?: 0)
        result = 31 * result + value
        result = 31 * result + mainContentType.hashCode()
        result = 31 * result + (audioDurationMs?.hashCode() ?: 0)
        result = 31 * result + (tempAudioFilePath?.hashCode() ?: 0)
        return result
    }
}