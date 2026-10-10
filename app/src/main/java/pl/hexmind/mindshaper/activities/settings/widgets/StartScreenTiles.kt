package pl.hexmind.mindshaper.activities.settings.widgets

import android.content.Context
import android.util.AttributeSet
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.common.ui.views.lists.HexOptionTiles
import pl.hexmind.mindshaper.services.dto.StartScreen

/**
 * Tile-typed selector for the screen opened on app start.
 *
 * Usage:
 *   tiles.setSelected(StartScreen.HOME)
 *   val screen = tiles.getSelected()
 */
class StartScreenTiles @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : HexOptionTiles(context, attrs, defStyleAttr) {

    private companion object {
        const val TILE_HOME       = 0
        const val TILE_STREAM     = 1
        const val TILE_WORKSHOP   = 2
        const val TILE_FLASHCARDS = 3
    }

    init {
        setOptions(listOf(
            Option(TILE_HOME,       R.string.settings_start_screen_home,       R.drawable.ic_activity_home),
            Option(TILE_STREAM,     R.string.settings_start_screen_stream,     R.drawable.ic_activity_stream),
            Option(TILE_FLASHCARDS, R.string.settings_start_screen_flashcards, R.drawable.ic_activity_flashcards),
            Option(TILE_WORKSHOP,   R.string.settings_start_screen_workshop,   R.drawable.ic_activity_workshop),
        ))
    }

    fun setSelected(screen: StartScreen) {
        setSelectedId(when (screen) {
            StartScreen.HOME       -> TILE_HOME
            StartScreen.STREAM     -> TILE_STREAM
            StartScreen.FLASHCARDS -> TILE_FLASHCARDS
            StartScreen.WORKSHOP   -> TILE_WORKSHOP
        })
    }

    fun getSelected(): StartScreen = when (getSelectedId()) {
        TILE_STREAM     -> StartScreen.STREAM
        TILE_FLASHCARDS -> StartScreen.FLASHCARDS
        TILE_WORKSHOP   -> StartScreen.WORKSHOP
        else            -> StartScreen.HOME
    }

    // Flashcards feature off = tile grayed out, a selected one falls back to Home
    fun setFlashcardsEnabled(enabled: Boolean) = setOptionEnabled(TILE_FLASHCARDS, enabled)
}
