package pl.hexmind.mindshaper.common.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.common.regex.HexTags

/**
 * Graphical replacement for the text search bar.
 *
 * Left: the button that opens the search sheet. Right: the picked criteria as removable pills.
 * The view owns no editing UI - it only reports that the host should open the sheet.
 */
class HexTagsSearcher @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val criteriaContainer: LinearLayout
    private val scrollView: View
    private val hintView: TextView

    private var currentTags = HexTags()

    var onOpenRequested: (() -> Unit)? = null
    var onTagsChanged: ((HexTags) -> Unit)? = null

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL

        LayoutInflater.from(context).inflate(R.layout.common_hex_tags_searcher, this, true)
        criteriaContainer = findViewById(R.id.ll_criteria)
        scrollView        = findViewById(R.id.hsv_criteria)
        hintView          = findViewById(R.id.tv_searcher_hint)

        // The whole bar opens the sheet, not just the icon - the hint text reads like an invitation to tap
        val openSearch = OnClickListener { onOpenRequested?.invoke() }
        findViewById<View>(R.id.btn_open_search).setOnClickListener(openSearch)
        hintView.setOnClickListener(openSearch)
        setOnClickListener(openSearch)

        renderCriteria()
    }

    fun getTags(): HexTags = currentTags

    fun setTags(tags: HexTags) {
        currentTags = tags
        renderCriteria()
        onTagsChanged?.invoke(currentTags)
    }

    private fun removeTag(clearedTags: HexTags) {
        currentTags = clearedTags
        renderCriteria()
        onTagsChanged?.invoke(currentTags)
    }

    private fun renderCriteria() {
        criteriaContainer.removeAllViews()

        addCriterionIfPresent(currentTags.subject,  R.drawable.ic_hextags_subject)   { currentTags.copy(subject = null) }
        addCriterionIfPresent(currentTags.soulMate, R.drawable.ic_hextags_soul_mates) { currentTags.copy(soulMate = null) }
        addCriterionIfPresent(currentTags.project,  R.drawable.ic_hextags_project)    { currentTags.copy(project = null) }

        val hasCriteria = criteriaContainer.childCount > 0
        scrollView.isVisible = hasCriteria
        hintView.isVisible   = !hasCriteria
    }

    private fun addCriterionIfPresent(value: String?, iconRes: Int, withoutThisTag: () -> HexTags) {
        if (value.isNullOrBlank()) return

        val pill = LayoutInflater.from(context)
            .inflate(R.layout.common_hex_tags_criterion, criteriaContainer, false)

        pill.findViewById<ImageView>(R.id.iv_criterion_icon).setImageResource(iconRes)
        pill.findViewById<TextView>(R.id.tv_criterion_value).text = value
        pill.findViewById<ImageView>(R.id.iv_criterion_remove).setOnClickListener { removeTag(withoutThisTag()) }

        criteriaContainer.addView(pill)
    }
}
