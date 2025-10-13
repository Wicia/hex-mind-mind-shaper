package pl.hexmind.mindshaper.services.dto

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import pl.hexmind.mindshaper.activities.InstantParceler
import pl.hexmind.mindshaper.activities.capture.models.InitialThoughtType
import java.time.Instant

@Parcelize
@TypeParceler<Instant?, InstantParceler>
data class ThoughtDTO(
    var id : Int? = null,
    var createdAt: Instant? = Instant.now(),
    var domainIconId : Int? = null,
    var thread : String? = "",
    var richText: String? = "",
    var initialThoughtType: InitialThoughtType = InitialThoughtType.UNKNOWN
) : Parcelable