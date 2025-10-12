package pl.hexmind.mindshaper.services.dto

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import pl.hexmind.mindshaper.activities.InstantParceler
import java.time.Instant

@Parcelize
@TypeParceler<Instant?, InstantParceler>
data class ThoughtDTO(
    var id : Int? = null,
    var createdAt: Instant? = Instant.now(),
    var domainIconId : Int? = null,
    var thread : String? = "",
    var essence : String? = "",
    var richText: String? = ""
) : Parcelable