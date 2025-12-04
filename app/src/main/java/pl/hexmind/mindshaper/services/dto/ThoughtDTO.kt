package pl.hexmind.mindshaper.services.dto

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import pl.hexmind.mindshaper.common.intent.InstantParceler
import pl.hexmind.mindshaper.activities.capture.models.InitialThoughtType
import java.time.Instant

@Parcelize
@TypeParceler<Instant?, InstantParceler>
data class ThoughtDTO(

    var id: Int? = null,
    var createdAt: Instant = Instant.now(),
    var domainId: Int? = null,

    var thread: String? = null,
    var soulMate: String? = null,
    var project: String? = null,
    var value: Int = 1,

    // TODO: save it in DB as "Main Content Type" (drawing, recording, text, image)
    var initialThoughtType: InitialThoughtType = InitialThoughtType.UNKNOWN,

    // RICH TEXT
    var richText: String? = null,

    // RECORDING
    // only light data here (like metadata & no byte arrays)
    var audioDurationMs: Long? = null,
    var hasAudio: Boolean = false,
    @Transient
    var tempAudioFilePath: String? = null // ! Used during recording

) : Parcelable {

    val duration: Long?
        get() = audioDurationMs

    // ! Needed for ByteArray in data class
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ThoughtDTO

        if (id != other.id) return false
        if (createdAt != other.createdAt) return false
        if (domainId != other.domainId) return false
        if (thread != other.thread) return false
        if (richText != other.richText) return false
        if (soulMate != other.soulMate) return false
        if (project != other.project) return false
        if (value != other.value) return false
        if (initialThoughtType != other.initialThoughtType) return false
        if (audioDurationMs != other.audioDurationMs) return false
        if (hasAudio != other.hasAudio) return false
        if (tempAudioFilePath != other.tempAudioFilePath) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id ?: 0
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + (domainId ?: 0)
        result = 31 * result + (thread?.hashCode() ?: 0)
        result = 31 * result + (richText?.hashCode() ?: 0)
        result = 31 * result + (soulMate?.hashCode() ?: 0)
        result = 31 * result + (project?.hashCode() ?: 0)
        result = 31 * result + value
        result = 31 * result + initialThoughtType.hashCode()
        result = 31 * result + (audioDurationMs?.hashCode() ?: 0)
        result = 31 * result + hasAudio.hashCode()
        result = 31 * result + (tempAudioFilePath?.hashCode() ?: 0)
        return result
    }
}