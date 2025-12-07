package pl.hexmind.mindshaper.common.ui

import android.content.Context

fun Context.dpToPx(dp: Int): Int {
    return (dp * resources.displayMetrics.density).toInt()
}

fun Context.dpToPx(dp: Float): Float {
    return dp * resources.displayMetrics.density
}