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
    var id : Int? = null,
    var createdAt: Instant? = Instant.now(),
    var domainId : Int? = null,
    var thread : String? = null,
    var richText: String? = null,
    var soulMate: String? = null,
    var project: String? = null,
    var initialThoughtType: InitialThoughtType = InitialThoughtType.UNKNOWN
) : Parcelable