package pl.hexmind.mindshaper.common

import android.os.Parcelable
import androidx.annotation.StringRes
import kotlinx.parcelize.Parcelize
import pl.hexmind.mindshaper.R

/**
 * Complete sort configuration
 */
@Parcelize
data class SortConfig(
    val property: SortProperty = SortProperty.CREATED_AT,
    val direction: SortDirection = SortDirection.DESCENDING
) : Parcelable

/**
 * Properties available for sorting thoughts
 */
@Parcelize
enum class SortProperty(
    @StringRes val displayNameRes: Int,
    val type : SortPropertyType) : Parcelable {
    VALUE(R.string.sort_property_value, SortPropertyType.NUMBER),
    CREATED_AT(R.string.sort_property_created_at, SortPropertyType.DATE),
    THREAD(R.string.sort_property_thread, SortPropertyType.TEXT),
    SOUL_MATE(R.string.sort_property_soul_mate, SortPropertyType.TEXT),
    PROJECT(R.string.sort_property_project, SortPropertyType.TEXT);
}

@Parcelize
enum class SortPropertyType : Parcelable {
    DATE,
    TEXT,
    NUMBER
}

/**
 * Sort direction - ascending or descending
 */
@Parcelize
enum class SortDirection(val resSortLabelText: Int,
                         val resSortLabelNumber: Int,
                         val resSortLabelDate: Int) : Parcelable {
    ASCENDING(
        R.string.sort_text_asc_label,
        R.string.sort_number_asc_label,
        R.string.sort_date_asc_label

    ),
    DESCENDING(
        R.string.sort_text_desc_label,
        R.string.sort_number_desc_label,
        R.string.sort_date_desc_label
    );

    fun toggle(): SortDirection {
        return when (this) {
            ASCENDING -> DESCENDING
            DESCENDING -> ASCENDING
        }
    }

    fun getLabelResByFieldType(fieldType : SortPropertyType) : Int{
        return when (fieldType) {
            SortPropertyType.TEXT -> {
                this.resSortLabelText
            }

            SortPropertyType.NUMBER -> {
                this.resSortLabelNumber
            }

            SortPropertyType.DATE -> {
                this.resSortLabelDate
            }
        }
    }
}