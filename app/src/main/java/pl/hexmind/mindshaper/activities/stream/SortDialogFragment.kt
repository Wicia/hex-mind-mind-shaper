package pl.hexmind.mindshaper.activities.stream

import android.graphics.Typeface
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.common.ui.dpToPx
import pl.hexmind.mindshaper.common.ui.views.HexTreeConnector
import pl.hexmind.mindshaper.common.ui.views.lists.SortConfig
import pl.hexmind.mindshaper.common.ui.views.lists.SortDirection
import pl.hexmind.mindshaper.common.ui.views.lists.SortProperty

/**
 * Bottom sheet for selecting sort property and direction.
 */
class SortDialogFragment(
    private val currentConfig: SortConfig,
    private val onSortSelected: (SortConfig) -> Unit
) : BottomSheetDialogFragment() {

    private var selectedProperty: SortProperty = currentConfig.property
    private var selectedDirection: SortDirection = currentConfig.direction

    private lateinit var llSortProperties: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.stream_sort_bottomsheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Force showing full-height bottom sheet
        (dialog as? BottomSheetDialog)?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED

        llSortProperties = view.findViewById(R.id.ll_sort_properties)
        // Tree branches of direction rows reach up into the gap above them
        llSortProperties.clipChildren = false

        rebuildList()
    }

    private fun rebuildList() {
        llSortProperties.removeAllViews()

        SortProperty.entries.forEach { property ->
            llSortProperties.addView(createPropertyButton(property))

            // Expand direction rows below the selected property
            if (property == selectedProperty) {
                SortDirection.entries.forEachIndexed { index, direction ->
                    llSortProperties.addView(createDirectionRow(direction, isFirst = index == 0))
                }
            }
        }
    }

    private fun createPropertyButton(property: SortProperty): MaterialButton {
        val themedContext = ContextThemeWrapper(requireContext(), R.style.SecondaryActionButton)
        return MaterialButton(themedContext).apply {
            text = getString(property.displayNameRes)
            gravity = Gravity.START or Gravity.CENTER_VERTICAL

            val isSelected = property == selectedProperty

            // Checkmark icon on selected property
            if (isSelected) {
                setIconResource(R.drawable.ic_action_approve)
                iconGravity = MaterialButton.ICON_GRAVITY_START
                iconTint = ContextCompat.getColorStateList(requireContext(), R.color.text_primary)
            } else {
                icon = null
            }

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 0)
            }

            minimumWidth = 400 // px

            updateButtonStyle(this, isSelected)

            setOnClickListener {
                selectedProperty = property
                rebuildList()
            }
        }
    }

    // Tree branch from the property above + direction button
    private fun createDirectionRow(direction: SortDirection, isFirst: Boolean): LinearLayout {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            clipChildren = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(requireContext().dpToPx(DIRECTION_ROW_INDENT_DP), DIRECTION_ROW_MARGIN_PX, 0, DIRECTION_ROW_MARGIN_PX)
            }

            addView(
                HexTreeConnector(requireContext()).apply {
                    // Rows' bottom + top margins make the gap between two direction rows
                    if (!isFirst) gapAbove = 2 * DIRECTION_ROW_MARGIN_PX
                },
                LinearLayout.LayoutParams(
                    requireContext().dpToPx(HexTreeConnector.WIDTH_DP),
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
            )
            addView(createDirectionButton(direction))
        }
    }

    private fun createDirectionButton(direction: SortDirection): MaterialButton {
        val themedContext = ContextThemeWrapper(requireContext(), R.style.SecondaryActionButton)
        return MaterialButton(themedContext).apply {
            text = getString(direction.getLabelResByFieldType(selectedProperty.type))
            gravity = Gravity.START or Gravity.CENTER_VERTICAL

            val isSelected = direction == selectedDirection

            // No icon on direction buttons
            icon = null

            // Indent comes from the tree connector in front of it
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            minimumWidth = 400 // px

            updateButtonStyle(this, isSelected)

            setOnClickListener {
                selectedDirection = direction
                onSortSelected(SortConfig(selectedProperty, selectedDirection))
                dismiss()
            }
        }
    }

    private fun updateButtonStyle(button: MaterialButton, isSelected: Boolean) {
        if (isSelected) {
            button.strokeColor = ContextCompat.getColorStateList(requireContext(), R.color._palette_lvl_3)
            button.strokeWidth = 2 // px
            button.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.button_secondary_enabled_background))
            button.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            button.setTypeface(button.typeface, Typeface.BOLD)
        }
        else {
            button.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.button_secondary_enabled_background))
            button.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            button.setTypeface(button.typeface, Typeface.NORMAL)
        }
    }

    companion object {
        const val TAG = "SortDialogFragment"
        private const val DIRECTION_ROW_MARGIN_PX = 4
        // Shifts the branch with its direction button to the right, under the property button
        private const val DIRECTION_ROW_INDENT_DP = 8
    }
}