package pl.hexmind.mindshaper.common

import androidx.annotation.StringRes
import pl.hexmind.mindshaper.R

/**
 * Properties available for sorting thoughts
 */
enum class SortProperty(@StringRes val displayNameRes: Int, val type : SortPropertyType) {
    VALUE(R.string.sort_property_value, SortPropertyType.NUMBER),
    CREATED_AT(R.string.sort_property_created_at, SortPropertyType.DATE),
    THREAD(R.string.sort_property_thread, SortPropertyType.TEXT),
    SOUL_MATE(R.string.sort_property_soul_mate, SortPropertyType.TEXT),
    PROJECT(R.string.sort_property_project, SortPropertyType.TEXT);

    companion object {
        fun getDefault() = CREATED_AT
    }
}

enum class SortPropertyType{
    DATE,
    TEXT,
    NUMBER
}

/**
 * Sort direction - ascending or descending
 */
enum class SortDirection(val iconRes: Int, // TODO - probably to be removed + ic_sort_asc & desc
                         val resSortLabelText: Int,
                         val resSortLabelNumber: Int,
                         val resSortLabelDate: Int) {
    ASCENDING(
        R.drawable.ic_sort_ascending,
        R.string.sort_text_asc_label,
        R.string.sort_number_asc_label,
        R.string.sort_date_asc_label

    ),
    DESCENDING(
        R.drawable.ic_sort_descending,
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

/**
 * Complete sort configuration
 */
data class SortConfig(
    val property: SortProperty = SortProperty.getDefault(),
    val direction: SortDirection = SortDirection.DESCENDING
)