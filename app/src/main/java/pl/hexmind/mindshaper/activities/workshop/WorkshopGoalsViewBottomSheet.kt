package pl.hexmind.mindshaper.activities.workshop

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.common.ui.views.lists.HexOptionTiles

/**
 * Which goals the Workshop shows - active or archived, one of them (compact HexOptionTiles). Tap on a tile = choose + close.
 * ! Empty archive: its tile disabled, with a note why
 */
class WorkshopGoalsViewBottomSheet(
    private val isArchiveShown: Boolean,
    private val isArchiveEmpty: Boolean,
    private val onViewSelected: (showArchive: Boolean) -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.workshop_goals_view_bottomsheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (dialog as? BottomSheetDialog)?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED

        view.findViewById<HexOptionTiles>(R.id.tiles_goals_view).apply {
            setOptions(
                listOf(
                    HexOptionTiles.Option(id = OPTION_ACTIVE, labelRes = R.string.workshop_goals_view_active),
                    HexOptionTiles.Option(id = OPTION_ARCHIVED, labelRes = R.string.workshop_goals_view_archived)
                )
            )
            setSelectedId(if (isArchiveShown) OPTION_ARCHIVED else OPTION_ACTIVE)
            setOptionEnabled(OPTION_ARCHIVED, enabled = !isArchiveEmpty)
            onSelectionChanged = { selectedId ->
                onViewSelected(selectedId == OPTION_ARCHIVED)
                dismiss()
            }
        }

        view.findViewById<TextView>(R.id.tv_goals_view_archive_empty).visibility =
            if (isArchiveEmpty) View.VISIBLE else View.GONE
    }

    companion object {
        const val TAG = "WorkshopGoalsViewBottomSheet"
        private const val OPTION_ACTIVE = 0
        private const val OPTION_ARCHIVED = 1
    }
}
