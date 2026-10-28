package pl.hexmind.mindshaper.activities.settings.widgets

import android.content.Context
import android.util.AttributeSet
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.common.ui.views.lists.HexOptionTiles
import pl.hexmind.mindshaper.services.dto.ColorPalette

/**
 * Tile-typed selector for the color palette of the app
 *
 * Usage:
 *   tiles.setSelected(ColorPalette.ORANGE)
 *   val palette = tiles.getSelected()
 */
class ColorPaletteTiles @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : HexOptionTiles(context, attrs, defStyleAttr) {

    private companion object {
        const val TILE_ORANGE = 0
        const val TILE_COFFEE = 1
    }

    init {
        setOptions(listOf(
            Option(TILE_ORANGE, R.string.settings_color_palette_orange),
            Option(TILE_COFFEE, R.string.settings_color_palette_coffee),
        ))
    }

    fun setSelected(palette: ColorPalette) {
        setSelectedId(when (palette) {
            ColorPalette.ORANGE -> TILE_ORANGE
            ColorPalette.COFFEE -> TILE_COFFEE
        })
    }

    fun getSelected(): ColorPalette = when (getSelectedId()) {
        TILE_COFFEE -> ColorPalette.COFFEE
        else        -> ColorPalette.ORANGE
    }
}
