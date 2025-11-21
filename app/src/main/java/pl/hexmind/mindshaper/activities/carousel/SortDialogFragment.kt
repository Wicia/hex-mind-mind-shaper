package pl.hexmind.mindshaper.activities.carousel

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.common.SortConfig
import pl.hexmind.mindshaper.common.SortDirection
import pl.hexmind.mindshaper.common.SortProperty
import pl.hexmind.mindshaper.common.SortPropertyType

/**
 * Dialog for selecting sort property and direction
 */
// TODO: Apply more black background for this dialog like in other places
class SortDialogFragment(
    private val currentConfig: SortConfig,
    private val onSortSelected: (SortConfig) -> Unit
) : DialogFragment() {

    private var selectedProperty: SortProperty = currentConfig.property
    private var selectedDirection: SortDirection = currentConfig.direction

    private lateinit var containerProperties: LinearLayout
    private lateinit var btnSortDirection: MaterialButton
    private lateinit var tvSortDirectionFrom: TextView
    private lateinit var tvSortDirectionTo: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_sort, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        containerProperties = view.findViewById(R.id.container_sort_properties)
        btnSortDirection = view.findViewById(R.id.btn_sort_direction)

        setupPropertyButtons()
        setupDirectionButton()
        updateDirectionButton()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            window?.setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    private fun setupPropertyButtons() {
        containerProperties.removeAllViews()

        SortProperty.entries.forEach { property ->
            val button = createPropertyButton(property)
            containerProperties.addView(button)
        }
    }

    private fun createPropertyButton(property: SortProperty): MaterialButton {
        return MaterialButton(requireContext()).apply {
            text = getString(property.displayNameRes)

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 8)
            }

            // Set style based on selection
            updateButtonStyle(this, property == selectedProperty)

            setOnClickListener {
                selectedProperty = property
                val config = SortConfig(selectedProperty, selectedDirection)
                onSortSelected(config)
                dismiss()
            }
        }
    }

    private fun setupDirectionButton() {
        // Click on entire direction container toggles, applies and dismisses
        view?.findViewById<View>(R.id.btn_sort_direction)?.setOnClickListener {
            selectedDirection = selectedDirection.toggle()
            updateDirectionButton()
            val config = SortConfig(selectedProperty, selectedDirection)
            onSortSelected(config)
            dismiss()
        }
    }

    private fun updateDirectionButton() {
        val resId = selectedDirection.getLabelResByFieldType(selectedProperty.type)
        btnSortDirection.text = requireContext().getString(resId)
    }

    private fun updateButtonStyle(button: MaterialButton, isSelected: Boolean) {
        if (isSelected) {
            button.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.button_used_background))
            button.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
        }
        else {
            button.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.button_not_used_background))
            button.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
        }
    }

    companion object {
        const val TAG = "SortDialogFragment"
    }
}