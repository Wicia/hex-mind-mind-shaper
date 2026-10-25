package pl.hexmind.mindshaper.activities.stream

import android.os.Build
import android.os.Bundle
import androidx.annotation.StringRes
import pl.hexmind.mindshaper.R
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import pl.hexmind.mindshaper.common.ui.views.IconsGridItem
import pl.hexmind.mindshaper.databinding.StreamFilterBottomsheetBinding

/**
 * Bottom sheet for filtering thoughts stream.
 *
 * Result is delivered via FragmentResult on FAB tap.
 */
class StreamFilterBottomSheet : BottomSheetDialogFragment() {

    private var _binding: StreamFilterBottomsheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = StreamFilterBottomsheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (dialog as? BottomSheetDialog)?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED

        val showActive       = arguments?.getBoolean(ARG_SHOW_ACTIVE, true) ?: true
        val showNearDormant  = arguments?.getBoolean(ARG_SHOW_NEAR_DORMANT, true) ?: true
        val showDormant      = arguments?.getBoolean(ARG_SHOW_DORMANT, false) ?: false
        val selectedDomainId = arguments?.getInt(ARG_SELECTED_DOMAIN_ID, -1)?.takeIf { it != -1 }
        val isDormantEnabled = arguments?.getBoolean(ARG_DORMANT_ENABLED, false) ?: false
        val activeCount      = arguments?.getInt(ARG_ACTIVE_COUNT, 0) ?: 0
        val nearDormantCount = arguments?.getInt(ARG_NEAR_DORMANT_COUNT, 0) ?: 0
        val dormantCount     = arguments?.getInt(ARG_DORMANT_COUNT, 0) ?: 0
        val createdPeriod    = arguments?.getString(ARG_CREATED_PERIOD)?.let { name -> CreatedPeriod.valueOf(name) }

        @Suppress("DEPRECATION")
        val iconItems: List<IconsGridItem> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelableArrayList(ARG_DOMAIN_ITEMS, IconsGridItem::class.java)
        } else {
            arguments?.getParcelableArrayList(ARG_DOMAIN_ITEMS)
        } ?: emptyList()

        // States section — hide entirely when dormant mode is off (no distinction between states)
        val statesVisibility = if (isDormantEnabled) View.VISIBLE else View.GONE
        binding.tvStatesLabel.visibility     = statesVisibility
        binding.cbShowActive.visibility      = statesVisibility
        binding.cbShowNearDormant.visibility = statesVisibility
        binding.cbShowDormant.visibility     = statesVisibility

        if (isDormantEnabled) {
            binding.cbShowActive.isChecked      = showActive
            binding.cbShowNearDormant.isChecked = showNearDormant
            binding.cbShowDormant.isChecked     = showDormant

            binding.cbShowActive.text      = getThoughtCountLabel(R.string.stream_filter_show_active, activeCount)
            binding.cbShowNearDormant.text = getThoughtCountLabel(R.string.stream_filter_show_near_dormant, nearDormantCount)
            binding.cbShowDormant.text     = getThoughtCountLabel(R.string.stream_filter_show_dormant, dormantCount)
        }

        // Domain grid — track selection manually to support deselect
        var currentSelectedId: Int? = selectedDomainId
        binding.igvDomains.bind(
            items      = iconItems,
            selectedId = selectedDomainId
        ) { clickedItem ->
            currentSelectedId = if (clickedItem.id == currentSelectedId) {
                binding.igvDomains.setSelectedItem(null)
                null
            } else {
                clickedItem.id
            }
        }

        // Created period chips - one at most; tapping the selected one again clears it (= any time)
        val createdChips = listOf(
            binding.chipCreatedToday          to CreatedPeriod.TODAY,
            binding.chipCreatedSinceYesterday to CreatedPeriod.SINCE_YESTERDAY,
            binding.chipCreatedLastWeek       to CreatedPeriod.LAST_WEEK,
            binding.chipCreatedLastMonth      to CreatedPeriod.LAST_MONTH
        )
        var currentCreatedPeriod: CreatedPeriod? = createdPeriod
        fun syncCreatedChips() {
            createdChips.forEach { (chip, period) -> chip.isSelected = period == currentCreatedPeriod }
        }
        createdChips.forEach { (chip, period) ->
            chip.setOnClickListener {
                currentCreatedPeriod = if (period == currentCreatedPeriod) null else period
                syncCreatedChips()
            }
        }
        syncCreatedChips()

        // FAB: apply filters and return result to StreamActivity
        binding.fabApply.setOnClickListener {
            parentFragmentManager.setFragmentResult(
                REQUEST_KEY,
                Bundle().apply {
                    putBoolean(RESULT_SHOW_ACTIVE,       if (isDormantEnabled) binding.cbShowActive.isChecked else true)
                    putBoolean(RESULT_SHOW_NEAR_DORMANT, if (isDormantEnabled) binding.cbShowNearDormant.isChecked else true)
                    putBoolean(RESULT_SHOW_DORMANT,      if (isDormantEnabled) binding.cbShowDormant.isChecked else true)
                    putInt(RESULT_DOMAIN_ID, currentSelectedId ?: -1)
                    putString(RESULT_CREATED_PERIOD, currentCreatedPeriod?.name)
                }
            )
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // To keep the wording in one string only
    private fun getThoughtCountLabel(@StringRes labelRes: Int, count: Int): String =
        getString(R.string.stream_filter_label_with_count, getString(labelRes), count)

    companion object {
        private const val TAG                    = "StreamFilterBottomSheet"
        private const val ARG_SHOW_ACTIVE        = "arg_show_active"
        private const val ARG_SHOW_NEAR_DORMANT  = "arg_show_near_dormant"
        private const val ARG_SHOW_DORMANT       = "arg_show_dormant"
        private const val ARG_SELECTED_DOMAIN_ID = "arg_selected_domain_id"
        private const val ARG_DOMAIN_ITEMS       = "arg_domain_items"
        private const val ARG_DORMANT_ENABLED    = "arg_dormant_enabled"
        private const val ARG_ACTIVE_COUNT       = "arg_active_count"
        private const val ARG_NEAR_DORMANT_COUNT = "arg_near_dormant_count"
        private const val ARG_DORMANT_COUNT      = "arg_dormant_count"
        private const val ARG_CREATED_PERIOD     = "arg_created_period"

        const val REQUEST_KEY              = "stream_filter_request"
        const val RESULT_SHOW_ACTIVE       = "result_show_active"
        const val RESULT_SHOW_NEAR_DORMANT = "result_show_near_dormant"
        const val RESULT_SHOW_DORMANT      = "result_show_dormant"
        const val RESULT_DOMAIN_ID         = "result_domain_id"
        const val RESULT_CREATED_PERIOD    = "result_created_period"

        fun show(
            fragmentManager      : FragmentManager,
            showActive           : Boolean,
            showNearDormant      : Boolean,
            showDormant          : Boolean,
            selectedDomainId     : Int?,
            domainItems          : List<IconsGridItem>,
            isDormantModeEnabled : Boolean,
            activeCount          : Int,
            nearDormantCount     : Int,
            dormantCount         : Int,
            createdPeriod        : CreatedPeriod?
        ) {
            StreamFilterBottomSheet().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_SHOW_ACTIVE,       showActive)
                    putBoolean(ARG_SHOW_NEAR_DORMANT, showNearDormant)
                    putBoolean(ARG_SHOW_DORMANT,      showDormant)
                    putInt(ARG_SELECTED_DOMAIN_ID, selectedDomainId ?: -1)
                    putParcelableArrayList(ARG_DOMAIN_ITEMS, ArrayList(domainItems))
                    putBoolean(ARG_DORMANT_ENABLED, isDormantModeEnabled)
                    putInt(ARG_ACTIVE_COUNT,       activeCount)
                    putInt(ARG_NEAR_DORMANT_COUNT, nearDormantCount)
                    putInt(ARG_DORMANT_COUNT,      dormantCount)
                    putString(ARG_CREATED_PERIOD, createdPeriod?.name)
                }
            }.show(fragmentManager, TAG)
        }
    }
}