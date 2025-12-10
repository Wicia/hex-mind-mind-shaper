package pl.hexmind.mindshaper.activities.capture.models

import android.os.Parcel
import android.os.Parcelable

enum class ThoughtMainContentType(val dbCode : String) : Parcelable {
    UNKNOWN("U"),
    RICH_TEXT("T"),
    RECORDING("R"),
    PHOTO("P"),
    DRAWING("D");

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(ordinal)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<ThoughtMainContentType> {
        override fun createFromParcel(parcel: Parcel): ThoughtMainContentType {
            return entries[parcel.readInt()]
        }

        override fun newArray(size: Int): Array<ThoughtMainContentType?> {
            return arrayOfNulls(size)
        }

        fun fromDbCode(code: String): ThoughtMainContentType {
            return entries.find { it.dbCode == code } ?: UNKNOWN
        }
    }
}