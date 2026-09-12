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
import pl.hexmind.mindshaper.common.validation.ValidatedProperty
import pl.hexmind.mindshaper.common.validation.ValidationResult
import pl.hexmind.mindshaper.common.validation.resolveMessage
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
import pl.hexmind.mindshaper.databinding.CommonHexTagsBottomsheetBinding

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
@AndroidEntryPoint
class HexTagsBottomSheet : BottomSheetDialogFragment() {

    @Inject
    lateinit var hexTagsService: HexTagsService

    private var _binding: CommonHexTagsBottomsheetBinding? = null
    private val binding get() = _binding!!

    private var onValidate: ((HexTags) -> ValidationResult)? = null
    private var onConfirm: ((HexTags) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = CommonHexTagsBottomsheetBinding.inflate(inflater, container, false)
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

        val selectedDomainId = arguments?.getInt(ARG_SELECTED_DOMAIN_ID, -1)?.takeIf { it != -1 }
        val currentPerson = arguments?.getString(ARG_PERSON)
        val currentProject = arguments?.getString(ARG_PROJECT)

        // Pre-fill fields with existing values
        currentPerson?.let { binding.hifPerson.setText(it) }
        currentProject?.let { binding.hifProject.setText(it) }

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

        setupSuggestions()

        binding.fabConfirm.setOnClickListener {
            val result = HexTags(
                person = binding.hifPerson.getText().takeIf { it.isNotEmpty() },
                project = binding.hifProject.getText().takeIf { it.isNotEmpty() },
                domainId = binding.igvDomains.selectedItemId
            )

            val validationResult = onValidate?.invoke(result)
            if (validationResult is ValidationResult.Error) {
                showExternalError(validationResult)
            } else {
                confirmUnlessSimilarTagExists(result)
            }
        }
    }

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

    /** Chips appear on focus and follow what is typed, so the sheet stays compact until it is needed. */
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
            // Only the tag being typed matters, not the ones already finished before a separator
            val names = hexTagsService.getSuggestions(tagType, field.getRawText())

            chipsContainer.removeAllViews()
            names.forEach { name -> chipsContainer.addView(buildChip(name, field, chipsContainer)) }
            scrollView.isVisible = names.isNotEmpty()
        }
    }

    private fun buildChip(name: String, field: HexInputField, chipsContainer: LinearLayout): TextView {
        val chip = LayoutInflater.from(requireContext())
            .inflate(R.layout.common_hex_tags_suggestion, chipsContainer, false) as TextView

        chip.text = name
        chip.setOnClickListener { replaceTypedTag(field, name) }
        return chip
    }

    // Swaps only the tag under the cursor, so picking a chip never wipes the earlier ones
    private fun replaceTypedTag(field: HexInputField, chosenName: String) {
        val finishedTags = field.getRawText().dropLast(typedPart(field.getRawText()).length).trim()

        field.setText(
            if (finishedTags.isEmpty()) "$chosenName " else "$finishedTags $chosenName "
        )
    }

    /** The unfinished word after the last separator - a tag ends at a space or a comma. */
    private fun typedPart(text: String): String =
        text.takeLastWhile { character -> !character.isWhitespace() && character != ',' }

    /**
     * A tag spelled almost like an existing one is usually a slip, so the user gets a choice instead
     * of a silent second entry in the dictionary.
     */
    private fun confirmUnlessSimilarTagExists(result: HexTags) {
        viewLifecycleOwner.lifecycleScope.launch {
            val similarTag = firstSimilarTag(result)

            if (similarTag == null) {
                dismiss()
                onConfirm?.invoke(result)
                return@launch
            }

            ActionsDialog.Builder(requireContext())
                .setTitle(getString(R.string.common_hex_tags_similar_title))
                .setDescription(
                    getString(R.string.common_hex_tags_similar_description, similarTag.suggestion)
                )
                .setStandardAction(
                    getString(R.string.common_hex_tags_similar_use, similarTag.suggestion)
                ) {
                    applySimilarTag(result, similarTag)
                }
                .setDismissText(getString(R.string.common_hex_tags_similar_keep_mine))
                .setDismissAction {
                    dismiss()
                    onConfirm?.invoke(result)
                }
                .show()
        }
    }

    /**
     * ! Checked tag by tag, not on the whole field - "ja powiesc" holds two tags, and normalizing
     * the field as one string would find nothing and then overwrite both with a single name.
     */
    private suspend fun firstSimilarTag(result: HexTags): SimilarTag? =
        firstSimilarTagOfType(HexTagType.PERSON, result.person)
            ?: firstSimilarTagOfType(HexTagType.PROJECT, result.project)

    private suspend fun firstSimilarTagOfType(tagType: HexTagType, fieldText: String?): SimilarTag? {
        splitTags(fieldText).forEach { typedTag ->
            val suggestion = hexTagsService.findSimilarTagNames(tagType, typedTag).firstOrNull()
            if (suggestion != null) return SimilarTag(tagType, typedTag, suggestion)
        }

        return null
    }

    /** Swaps one tag inside the field, leaving the others as they were. */
    private fun applySimilarTag(result: HexTags, similarTag: SimilarTag) {
        val corrected = when (similarTag.tagType) {
            HexTagType.PERSON  -> result.copy(person = replaceTag(result.person, similarTag))
            HexTagType.PROJECT -> result.copy(project = replaceTag(result.project, similarTag))
        }

        dismiss()
        onConfirm?.invoke(corrected)
    }

    private fun replaceTag(fieldText: String?, similarTag: SimilarTag): String =
        splitTags(fieldText)
            .map { tagName -> if (tagName == similarTag.typedTag) similarTag.suggestion else tagName }
            .joinToString(" ")

    private fun splitTags(fieldText: String?): List<String> =
        fieldText?.split(TAG_SEPARATOR_PATTERN)
            ?.map { tagName -> tagName.trim().lowercase() }
            ?.filter { tagName -> tagName.isNotEmpty() }
            .orEmpty()

    private data class SimilarTag(
        val tagType   : HexTagType,
        val typedTag  : String,
        val suggestion: String
    )

    private fun showExternalError(error: ValidationResult.Error) {
        val errorMessage : String = error.resolveMessage(requireContext())
        when (error.refProperty) {
            ValidatedProperty.T_PEOPLE -> binding.hifPerson.showError(errorMessage)
            ValidatedProperty.T_PROJECT    -> binding.hifProject.showError(errorMessage)
            else -> { }
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

    private val TAG_SEPARATOR_PATTERN = Regex("[,\\s]+")

    companion object {
        private const val TAG = "HexTagsBottomSheet"
        private const val ARG_ITEMS = "arg_items"
        private const val ARG_SELECTED_DOMAIN_ID = "arg_selected_domain_id"
        private const val ARG_PERSON = "arg_person"
        private const val ARG_PROJECT = "arg_project"

        fun show(
            fragmentManager: FragmentManager,
            items: List<IconsGridItem>,
            currentTags: HexTags = HexTags(),
            onValidate: (HexTags) -> ValidationResult,
            onConfirm: (HexTags) -> Unit
        ) {
            HexTagsBottomSheet().apply {
                arguments = Bundle().apply {
                    putParcelableArrayList(ARG_ITEMS, ArrayList(items))
                    currentTags.domainId?.let { putInt(ARG_SELECTED_DOMAIN_ID, it) }
                    currentTags.person?.let { putString(ARG_PERSON, it) }
                    currentTags.project?.let { putString(ARG_PROJECT, it) }
                }
                this.onValidate = onValidate
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