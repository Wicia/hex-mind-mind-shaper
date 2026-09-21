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
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.common.ui.views.HexInputField
import pl.hexmind.mindshaper.database.models.HexTagType
import pl.hexmind.mindshaper.services.HexTagsService
import javax.inject.Inject
import pl.hexmind.mindshaper.databinding.StreamSearchBottomsheetBinding

/**
 * Collects the search criteria (hex tags) for the stream thoughts
 *
 * All three fields are shown at once; the user picks whichever one they want to type in.
 * Leaving a field empty clears that criterion.
 */
@AndroidEntryPoint
class StreamSearchBottomSheet : BottomSheetDialogFragment() {

    @Inject
    lateinit var hexTagsService: HexTagsService

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
        binding.hifPerson.setText(arguments?.getString(ARG_PERSON))
        binding.hifProject.setText(arguments?.getString(ARG_PROJECT))

        setupSuggestions()

        binding.fabSearchConfirm.setOnClickListener {
            onConfirm?.invoke(
                HexTags(
                    subject  = binding.hifSubject.getText().ifBlank { null },
                    person = binding.hifPerson.getText().ifBlank { null },
                    project  = binding.hifProject.getText().ifBlank { null }
                )
            )
            dismiss()
        }
    }

    // Only tags get suggestions - the subject is a headline of one thought, never reused
    private fun setupSuggestions() {
        bindSuggestions(
            field          = binding.hifPerson,
            scrollView     = binding.hsvSuggestionsPerson,
            chipsContainer = binding.llSuggestionsPerson,
            tagType        = HexTagType.PERSON
        )

        bindSuggestions(
            field          = binding.hifProject,
            scrollView     = binding.hsvSuggestionsProject,
            chipsContainer = binding.llSuggestionsProject,
            tagType        = HexTagType.PROJECT
        )
    }

    private fun bindSuggestions(
        field         : HexInputField,
        scrollView    : View,
        chipsContainer: LinearLayout,
        tagType       : HexTagType
    ) {
        field.setOnFocusChangeListener { hasFocus ->
            if (hasFocus) {
                refreshSuggestions(field, scrollView, chipsContainer, tagType)
            }
            else {
                scrollView.isVisible = false
            }
        }

        field.addTextChangedListener {
            if (field.hasFocus()) {
                refreshSuggestions(field, scrollView, chipsContainer, tagType)
            }
        }
    }

    private fun refreshSuggestions(
        field         : HexInputField,
        scrollView    : View,
        chipsContainer: LinearLayout,
        tagType       : HexTagType
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            val names = hexTagsService.getSuggestions(tagType, field.getRawText())

            chipsContainer.removeAllViews()
            names.forEach { name -> chipsContainer.addView(buildChip(name, field, scrollView, chipsContainer)) }
            scrollView.isVisible = names.isNotEmpty()
        }
    }

    private fun buildChip(
        name          : String,
        field         : HexInputField,
        scrollView    : View,
        chipsContainer: LinearLayout
    ): TextView {
        val chip = LayoutInflater.from(requireContext())
            .inflate(R.layout.common_hex_tags_suggestion, chipsContainer, false) as TextView

        chip.text = name

        // ! Do not hide the row here - setText triggers a refresh that shows it again, and the
        // sheet visibly jumps as its height collapses and expands within one frame
        chip.setOnClickListener { appendChosenTag(field, name) }

        return chip
    }

    /**
     * Several criteria of one type narrow the stream further ("maciek kamil" = thoughts about both),
     * so a chip completes the tag being typed and leaves room for the next one.
     */
    private fun appendChosenTag(field: HexInputField, chosenName: String) {
        val rawText      = field.getRawText()
        val finishedTags = rawText.dropLast(typedPart(rawText).length).trim()

        field.setText(
            if (finishedTags.isEmpty()) "$chosenName " else "$finishedTags $chosenName "
        )
    }

    /** The unfinished word after the last separator - a tag ends at a space or a comma. */
    private fun typedPart(text: String): String =
        text.takeLastWhile { character -> !character.isWhitespace() && character != ',' }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {

        const val TAG = "StreamSearchBottomSheet"

        private const val ARG_SUBJECT    = "arg_subject"
        private const val ARG_PERSON  = "arg_person"
        private const val ARG_PROJECT   = "arg_project"

        fun show(
            fragmentManager: FragmentManager,
            currentTags    : HexTags,
            onConfirm      : (HexTags) -> Unit
        ) {
            StreamSearchBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_SUBJECT, currentTags.subject)
                    putString(ARG_PERSON, currentTags.person)
                    putString(ARG_PROJECT, currentTags.project)
                }
                this.onConfirm = onConfirm
            }.show(fragmentManager, TAG)
        }
    }
}
