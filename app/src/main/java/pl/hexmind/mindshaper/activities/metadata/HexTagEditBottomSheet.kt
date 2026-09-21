package pl.hexmind.mindshaper.activities.metadata

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.databinding.HexTagEditBottomsheetBinding

/**
 * Bottom sheet for editing a hex tag's name. Caller passes the current name via show() and gets the
 * new one back through onConfirm; persistence and clash handling stay with the caller.
 */
class HexTagEditBottomSheet : BottomSheetDialogFragment() {

    private var _binding: HexTagEditBottomsheetBinding? = null
    private val binding get() = _binding!!

    private var onConfirm: ((newName: String) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = HexTagEditBottomsheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (dialog as? BottomSheetDialog)?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED

        // Tap outside a field drops focus + keyboard
        binding.root.setOnTouchListener { _, _ ->
            binding.root.findFocus()?.clearFocus()
            hideKeyboard()
            false
        }

        arguments?.getString(ARG_TAG_NAME)?.let { tagName -> binding.hifTagName.setText(tagName) }

        binding.fabConfirm.setOnClickListener {
            val newName = binding.hifTagName.getText()
            if (newName.isEmpty()) {
                binding.hifTagName.showError(getString(R.string.metadata_tag_edit_empty))
                return@setOnClickListener
            }

            binding.hifTagName.clearError()
            onConfirm?.invoke(newName)
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
        private const val TAG = "HexTagEditBottomSheet"
        private const val ARG_TAG_NAME = "arg_tag_name"

        fun show(
            fragmentManager: FragmentManager,
            tagName: String,
            onConfirm: (newName: String) -> Unit
        ) {
            HexTagEditBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_TAG_NAME, tagName)
                }
                this.onConfirm = onConfirm
            }.show(fragmentManager, TAG)
        }
    }
}
