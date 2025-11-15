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
enum class SortDirection(val iconRes: Int) {
    ASCENDING(R.drawable.ic_sort_ascending),
    DESCENDING(R.drawable.ic_sort_descending);

    fun toggle(): SortDirection {
        return when (this) {
            ASCENDING -> DESCENDING
            DESCENDING -> ASCENDING
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