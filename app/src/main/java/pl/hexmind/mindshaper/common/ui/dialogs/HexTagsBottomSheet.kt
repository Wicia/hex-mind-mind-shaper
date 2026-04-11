package pl.hexmind.mindshaper.common.ui.dialogs

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
import pl.hexmind.mindshaper.databinding.ZHexTagsBottomsheetBinding

/**
 * Bottom sheet for picking a hex tag icon.
 *
 * Caller provides items + optional pre-selected HexTags, receives filled HexTags on confirm.
 * Dismiss by swiping down or tapping outside — no cancel button needed.
 *
 * Usage:
 *   HexTagsBottomSheet.show(
 *       fragmentManager = parentFragmentManager,
 *       items = ...,
 *       currentTags = HexTags(person = "Jan", project = "Projekt X", domainId = 3)
 *   ) { result ->
 *       // handle result.person, result.project, result.domainId
 *   }
 */
class HexTagsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: ZHexTagsBottomsheetBinding? = null
    private val binding get() = _binding!!

    private var onConfirm: ((HexTags) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ZHexTagsBottomsheetBinding.inflate(inflater, container, false)
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

        val selectedDomainId = arguments?.getInt(ARG_SELECTED_ID, -1)?.takeIf { it != -1 }
        val currentPerson = arguments?.getString(ARG_PERSON)
        val currentProject = arguments?.getString(ARG_PROJECT)

        // Pre-fill fields with existing values
        currentPerson?.let { binding.etPerson.setText(it) }
        currentProject?.let { binding.etProject.setText(it) }

        @Suppress("DEPRECATION")
        val iconItems: List<IconsGridItem> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelableArrayList(ARG_ITEMS, IconsGridItem::class.java)
        } else {
            arguments?.getParcelableArrayList(ARG_ITEMS)
        } ?: emptyList()

        binding.igvDomains.bind(
            items = iconItems,
            selectedId = selectedDomainId
        )

        binding.fabConfirm.setOnClickListener {
            val result = HexTags(
                person = binding.etPerson.text?.toString()?.trim()?.takeIf { it.isNotEmpty() },
                project = binding.etProject.text?.toString()?.trim()?.takeIf { it.isNotEmpty() },
                domainId = binding.igvDomains.selectedItemId
            )
            onConfirm?.invoke(result)
            dismiss()
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
        private const val TAG = "HexTagsBottomSheet"
        private const val ARG_ITEMS = "arg_items"
        private const val ARG_SELECTED_ID = "arg_selected_id"
        private const val ARG_PERSON = "arg_person"
        private const val ARG_PROJECT = "arg_project"

        fun show(
            fragmentManager: FragmentManager,
            items: List<IconsGridItem>,
            currentTags: HexTags = HexTags(),
            onConfirm: (HexTags) -> Unit
        ) {
            HexTagsBottomSheet().apply {
                arguments = Bundle().apply {
                    putParcelableArrayList(ARG_ITEMS, ArrayList(items))
                    currentTags.domainId?.let { putInt(ARG_SELECTED_ID, it) }
                    currentTags.person?.let { putString(ARG_PERSON, it) }
                    currentTags.project?.let { putString(ARG_PROJECT, it) }
                }
                this.onConfirm = onConfirm
            }.show(fragmentManager, TAG)
        }
    }
}

data class HexTags(
    val person: String? = null,
    val project: String? = null,
    val domainId: Int? = null
)