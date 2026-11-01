package pl.hexmind.mindshaper.services.dto

import android.content.res.Resources
import androidx.annotation.StyleRes
import pl.hexmind.mindshaper.R

/*
 * Color palette of the app - an overlay with the paletteLvl0..3 theme attrs (read by res/color/_palette_lvl_*).
 * ! Applied before an activity inflates anything and on the application theme (notifications)
 */
enum class ColorPalette(@StyleRes val overlayRes: Int) {
    ORANGE(R.style.ThemeOverlay_MindShaper_Palette_Orange),
    COFFEE(R.style.ThemeOverlay_MindShaper_Palette_Coffee);

    fun applyTo(theme: Resources.Theme) = theme.applyStyle(overlayRes, true)
}
