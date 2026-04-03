package pl.hexmind.mindshaper.activities.settings.widgets

import android.content.Context
import android.util.AttributeSet
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.common.ui.views.lists.HexOptionTiles
import pl.hexmind.mindshaper.services.dto.DefaultCaptureForm

/**
 * Tile-typed selector for the default thought capture form.
 *
 * Usage:
 *   tiles.setSelected(DefaultCaptureForm.TEXT)
 *   tiles.setVoiceEnabled(false)
 *   val form = tiles.getSelected()
 */
class DefaultCaptureFormTiles @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : HexOptionTiles(context, attrs, defStyleAttr) {

    private companion object {
        const val TILE_TEXT  = 0
        const val TILE_VOICE = 1
        const val TILE_PHOTO = 2
    }

    init {
        setOptions(listOf(
            Option(TILE_TEXT,  R.string.settings_default_form_text,  R.drawable.ic_thought_type_rich_text),
            Option(TILE_VOICE, R.string.settings_default_form_voice, R.drawable.ic_thought_type_recording),
            Option(TILE_PHOTO, R.string.settings_default_form_photo, R.drawable.ic_thought_type_photo),
        ))
    }

    fun setSelected(form: DefaultCaptureForm) {
        setSelectedId(when (form) {
            DefaultCaptureForm.TEXT  -> TILE_TEXT
            DefaultCaptureForm.VOICE -> TILE_VOICE
            DefaultCaptureForm.PHOTO -> TILE_PHOTO
        })
    }

    fun getSelected(): DefaultCaptureForm = when (getSelectedId()) {
        TILE_VOICE -> DefaultCaptureForm.VOICE
        TILE_PHOTO -> DefaultCaptureForm.PHOTO
        else       -> DefaultCaptureForm.TEXT
    }

    fun setVoiceEnabled(enabled: Boolean) = setOptionEnabled(TILE_VOICE, enabled)

    fun setPhotoEnabled(enabled: Boolean) = setOptionEnabled(TILE_PHOTO, enabled)
}