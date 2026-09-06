package pl.hexmind.mindshaper.activities.stream

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import pl.hexmind.mindshaper.common.regex.HexTags
import pl.hexmind.mindshaper.databinding.StreamSearchBottomsheetBinding

/**
 * Collects the search criteria (hex tags) for the stream thoughts
 *
 * All three fields are shown at once; the user picks whichever one they want to type in.
 * Leaving a field empty clears that criterion.
 */
class StreamSearchBottomSheet : BottomSheetDialogFragment() {

    private var _binding: StreamSearchBottomsheetBinding? = null
    private val binding get() = _binding!!

    private var onConfirm: ((HexTags) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = StreamSearchBottomsheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()

        // ! StreamActivity runs with adjustNothing, which the dialog window inherits - without this
        // the keyboard covers the lowest field instead of pushing the sheet up
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.hifSubject.setText(arguments?.getString(ARG_SUBJECT))
        binding.hifSoulMate.setText(arguments?.getString(ARG_SOUL_MATE))
        binding.hifProject.setText(arguments?.getString(ARG_PROJECT))

        binding.fabSearchConfirm.setOnClickListener {
            onConfirm?.invoke(
                HexTags(
                    subject  = binding.hifSubject.getText().ifBlank { null },
                    soulMate = binding.hifSoulMate.getText().ifBlank { null },
                    project  = binding.hifProject.getText().ifBlank { null }
                )
            )
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {

        const val TAG = "StreamSearchBottomSheet"

        private const val ARG_SUBJECT    = "arg_subject"
        private const val ARG_SOUL_MATE  = "arg_soul_mate"
        private const val ARG_PROJECT   = "arg_project"

        fun show(
            fragmentManager: FragmentManager,
            currentTags    : HexTags,
            onConfirm      : (HexTags) -> Unit
        ) {
            StreamSearchBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_SUBJECT, currentTags.subject)
                    putString(ARG_SOUL_MATE, currentTags.soulMate)
                    putString(ARG_PROJECT, currentTags.project)
                }
                this.onConfirm = onConfirm
            }.show(fragmentManager, TAG)
        }
    }
}
