package pl.hexmind.fastnote.activities.capture.models

import android.os.Parcel
import android.os.Parcelable

enum class InitialThoughtType : Parcelable {
    UNKNOWN,
    NOTE,
    VOICE,
    PHOTO,
    DRAWING;

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(ordinal)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<InitialThoughtType> {
        override fun createFromParcel(parcel: Parcel): InitialThoughtType {
            return entries[parcel.readInt()]
        }

        override fun newArray(size: Int): Array<InitialThoughtType?> {
            return arrayOfNulls(size)
        }
    }
}