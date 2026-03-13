package pl.hexmind.mindshaper.common.formatting

fun android.content.res.Resources.colorStateList(
    colorRes: Int,
    theme: android.content.res.Resources.Theme
) = android.content.res.ColorStateList.valueOf(getColor(colorRes, theme))