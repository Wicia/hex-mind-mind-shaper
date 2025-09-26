package pl.hexmind.mindshaper.services.dto

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import pl.hexmind.mindshaper.activities.InstantParceler
import java.time.Instant

@Parcelize
@TypeParceler<Instant?, InstantParceler>
data class ThoughtDTO(
    var essence : String = "",
    var createdAt: Instant? = Instant.now(),
    var id : Int? = null,
    var domainIconId : Int? = null,
    var thread : String = "",
    var richText: String = ""
) : Parcelable