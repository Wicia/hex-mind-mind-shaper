package pl.hexmind.mindshaper.activities.settings.widgets

import android.content.Context
import android.util.AttributeSet
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.common.ui.views.lists.HexOptionTiles
import pl.hexmind.mindshaper.common.ui.views.values.ThoughtValueSystem

/**
 * Tile-typed selector for the thought value system
 *
 * Usage:
 *   tiles.setSelected(ThoughtValueSystem.STANDARD_10)
 *   val system = tiles.getSelected()
 */
class ThoughtValueSystemTiles @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : HexOptionTiles(context, attrs, defStyleAttr) {

    private companion object {
        const val TILE_1_6  = 0
        const val TILE_1_10 = 1
    }

    init {
        setOptions(listOf(
            Option(TILE_1_6,  R.string.settings_thoughts_value_system_6),
            Option(TILE_1_10, R.string.settings_thoughts_value_system_10),
        ))
    }

    fun setSelected(system: ThoughtValueSystem) {
        setSelectedId(when (system) {
            ThoughtValueSystem.STANDARD_6  -> TILE_1_6
            ThoughtValueSystem.STANDARD_10 -> TILE_1_10
        })
    }

    fun getSelected(): ThoughtValueSystem = when (getSelectedId()) {
        TILE_1_6 -> ThoughtValueSystem.STANDARD_6
        else   -> ThoughtValueSystem.STANDARD_10
    }
}