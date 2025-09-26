package pl.hexmind.mindshaper.activities

import kotlinx.parcelize.Parceler
import java.time.Instant

/**
 * For handling Instant fields in objects passed between activities in parceler serializer
 */
object InstantParceler : Parceler<Instant?> {
    override fun create(parcel: android.os.Parcel): Instant? {
        val time = parcel.readLong()
        return if (time == -1L) null else Instant.ofEpochMilli(time)
    }

    override fun Instant?.write(parcel: android.os.Parcel, flags: Int) {
        parcel.writeLong(this?.toEpochMilli() ?: -1L)
    }
}