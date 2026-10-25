package pl.hexmind.mindshaper.activities.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.progressindicator.CircularProgressIndicator
import dagger.hilt.android.AndroidEntryPoint
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.activities.stream.StreamActivity
import pl.hexmind.mindshaper.services.AppNewsService
import pl.hexmind.mindshaper.services.ThoughtsService
import pl.hexmind.mindshaper.services.dto.ThoughtDTO
import javax.inject.Inject

/**
 * App news, opened from the rocket on Home.
 * Entries: near-dormant thoughts (only with the dormant mode on); without any entry - the empty info.
 * Unread entries on top, read ones below the notch separator.
 * An entry counts as read once opened; its ring marks it as read / unread without opening it
 * (e.g. to come back to it later) - Home hears about it through [RESULT_NEWS_CHANGED].
 */
@AndroidEntryPoint
class HomeNewsBottomSheet : BottomSheetDialogFragment() {

    @Inject lateinit var thoughtsService: ThoughtsService
    @Inject lateinit var appNewsService: AppNewsService

    private lateinit var llUnread: LinearLayout
    private lateinit var llRead: LinearLayout
    private lateinit var vSeparator: View
    private lateinit var tvEmpty: TextView

    private var thoughts: List<ThoughtDTO> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.home_news_bottomsheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Force showing full-height bottom sheet
        (dialog as? BottomSheetDialog)?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED

        llUnread   = view.findViewById(R.id.ll_home_news_unread)
        llRead     = view.findViewById(R.id.ll_home_news_read)
        vSeparator = view.findViewById(R.id.v_home_news_separator)
        tvEmpty    = view.findViewById(R.id.tv_home_news_empty)

        thoughtsService.getAllThoughts().observe(viewLifecycleOwner) { allThoughts ->
            thoughts = allThoughts
            rebuildEntries()
        }
    }

    private fun rebuildEntries() {
        llUnread.removeAllViews()
        llRead.removeAllViews()

        addNearDormantEntry()

        // Nothing to separate unless both groups have entries
        vSeparator.isVisible = llUnread.childCount > 0 && llRead.childCount > 0
        tvEmpty.isVisible = llUnread.childCount == 0 && llRead.childCount == 0
    }

    // Without the dormant mode no thought falls asleep - no entry
    private fun addNearDormantEntry() {
        val count = appNewsService.nearDormantThoughtIds(thoughts).size
        if (count == 0) return

        val isUnread = appNewsService.isNearDormantUnread(thoughts)
        val text = resources.getQuantityString(R.plurals.home_news_near_dormant, count, count)

        addEntry(
            text = text,
            isUnread = isUnread,
            onOpen = {
                appNewsService.markNearDormantRead(thoughts)
                startActivity(
                    Intent(requireContext(), StreamActivity::class.java)
                        .putExtra(StreamActivity.P_NEAR_DORMANT_ONLY, true)
                )
                dismiss()
            },
            onToggleRead = {
                if (isUnread) appNewsService.markNearDormantRead(thoughts) else appNewsService.markNearDormantUnread()
            }
        )
    }

    private fun addEntry(text: String, isUnread: Boolean, onOpen: () -> Unit, onToggleRead: () -> Unit) {
        val container = if (isUnread) llUnread else llRead
        val row = layoutInflater.inflate(R.layout.home_news_item, container, false)

        row.findViewById<TextView>(R.id.tv_news_text).text = text
        bindReadRing(row, isUnread)
        row.setOnClickListener { onOpen() }

        // The entry moves to the other group right away; the badge on Home follows
        row.findViewById<View>(R.id.fl_news_ring).setOnClickListener {
            onToggleRead()
            rebuildEntries()
            parentFragmentManager.setFragmentResult(RESULT_NEWS_CHANGED, Bundle.EMPTY)
        }

        container.addView(row)
    }

    // Unread = empty ring + chevron; read = full ring with a tick, like a done goal step
    private fun bindReadRing(row: View, isUnread: Boolean) {
        val context = requireContext()
        val ring = row.findViewById<CircularProgressIndicator>(R.id.cpi_news_ring)

        ring.setProgressCompat(if (isUnread) 0 else 100, false)
        if (!isUnread) {
            ring.setIndicatorColor(ContextCompat.getColor(context, R.color.progress_ring_done))
            ring.trackColor = ContextCompat.getColor(context, R.color.progress_ring_done)
        }
        row.findViewById<TextView>(R.id.tv_news_ring_label).text = if (isUnread) "" else "✓"
        row.findViewById<View>(R.id.iv_news_chevron).isVisible = isUnread
        row.findViewById<View>(R.id.fl_news_ring).contentDescription =
            getString(if (isUnread) R.string.home_news_mark_read else R.string.home_news_mark_unread)
    }

    companion object {
        private const val TAG = "HomeNewsBottomSheet"

        const val RESULT_NEWS_CHANGED = "home_news_changed"

        fun show(fragmentManager: FragmentManager) {
            if (fragmentManager.findFragmentByTag(TAG) != null) return
            HomeNewsBottomSheet().show(fragmentManager, TAG)
        }
    }
}
