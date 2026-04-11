package pl.hexmind.mindshaper.activities.settings

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import pl.hexmind.mindshaper.common.ui.views.IconsGridItem
import pl.hexmind.mindshaper.common.validation.ValidationResult
import pl.hexmind.mindshaper.databinding.DomainEditBottomsheetBinding
import pl.hexmind.mindshaper.services.validators.DomainValidator

/**
 * Bottom sheet for editing domain name and icon.
 *
 * Caller pre-loads icon items and provides current domain data via companion show().
 * Validation is handled internally — error message shown in tv_domain_name_validation_info.
 *
 * Usage:
 *   DomainEditBottomSheet.show(
 *       fragmentManager = supportFragmentManager,
 *       items           = iconItems,
 *       domainName      = currentDomain.name,
 *       selectedIconId  = currentDomain.iconId,
 *       validator       = domainValidator
 *   ) { name, iconId -> ... }
 */
class DomainEditBottomSheet(
    private val validator: DomainValidator
) : BottomSheetDialogFragment() {

    private var _binding: DomainEditBottomsheetBinding? = null
    private val binding get() = _binding!!

    private var onConfirm: ((name: String, iconId: Int) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DomainEditBottomsheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Force showing full-height bottom sheet
        (dialog as? BottomSheetDialog)?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED

        // Clear focus and hide keyboard when tapping outside an input field
        binding.root.setOnTouchListener { _, _ ->
            binding.root.findFocus()?.clearFocus()
            hideKeyboard()
            false
        }

        val domainName = arguments?.getString(ARG_DOMAIN_NAME)
        val selectedIconId = arguments?.getInt(ARG_SELECTED_ICON_ID, -1)?.takeIf { it != -1 }

        domainName?.let { binding.etDomainName.setText(it) }

        @Suppress("DEPRECATION")
        val iconItems: List<IconsGridItem> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelableArrayList(ARG_ITEMS, IconsGridItem::class.java)
        } else {
            arguments?.getParcelableArrayList(ARG_ITEMS)
        } ?: emptyList()

        binding.igvDomainIcons.bind(
            items = iconItems,
            selectedId = selectedIconId
        )

        binding.fabConfirm.setOnClickListener {
            val name = binding.etDomainName.text?.toString()?.trim() ?: ""
            val iconId = binding.igvDomainIcons.selectedItemId

            // Validate name — icon selection is optional (keeps current if null)
            when (val result = validator.validateName(name)) {
                is ValidationResult.Valid -> {
                    onConfirm?.invoke(name, iconId ?: return@setOnClickListener)
                    dismiss()
                }
                is ValidationResult.Error -> {
                    binding.tvDomainNameValidationInfo.text = result.message
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    companion object {
        private const val TAG = "DomainEditBottomSheet"
        private const val ARG_ITEMS = "arg_items"
        private const val ARG_DOMAIN_NAME = "arg_domain_name"
        private const val ARG_SELECTED_ICON_ID = "arg_selected_icon_id"

        fun show(
            fragmentManager: FragmentManager,
            items: List<IconsGridItem>,
            domainName: String,
            selectedIconId: Int,
            validator: DomainValidator,
            onConfirm: (name: String, iconId: Int) -> Unit
        ) {
            DomainEditBottomSheet(validator).apply {
                arguments = Bundle().apply {
                    putParcelableArrayList(ARG_ITEMS, ArrayList(items))
                    putString(ARG_DOMAIN_NAME, domainName)
                    putInt(ARG_SELECTED_ICON_ID, selectedIconId)
                }
                this.onConfirm = onConfirm
            }.show(fragmentManager, TAG)
        }
    }
}
