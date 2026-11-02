package pl.hexmind.mindshaper.activities.metadata

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.database.models.HexTagUsage

class MetadataTagRowAdapter(
    private val onTagTap: (tagName: String) -> Unit = {},
    private val onTagLongPress: (tagName: String) -> Unit = {},
    private val onTagSearch: (tagName: String) -> Unit = {},
    private val onTagMergePick: (tagName: String) -> Unit = {}
) : RecyclerView.Adapter<MetadataTagRowAdapter.RowViewHolder>() {

    private var rows: List<List<HexTagUsage>> = emptyList()

    // Merge mode: the whole pill picks the tag - edit, search and delete step aside
    private var isMergeMode = false
    private var mergeTargetName: String? = null

    /** [targetName] = the tag picked first, the one that stays - marked on its pill */
    fun setMergeMode(isEnabled: Boolean, targetName: String? = null) {
        isMergeMode = isEnabled
        mergeTargetName = if (isEnabled) targetName else null
        notifyDataSetChanged()
    }

    fun submitRows(newRows: List<List<HexTagUsage>>) {
        rows = newRows
        notifyDataSetChanged()
    }

    class RowViewHolder(val rowContainer: LinearLayout) : RecyclerView.ViewHolder(rowContainer)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowViewHolder {
        val rowContainer = LayoutInflater.from(parent.context)
            .inflate(R.layout.metadata_hextag_row, parent, false) as LinearLayout
        return RowViewHolder(rowContainer)
    }

    override fun onBindViewHolder(holder: RowViewHolder, position: Int) {
        val rowContainer = holder.rowContainer
        // Reused rows may still hold pills from a previous binding
        rowContainer.removeAllViews()

        rows[position].forEach { hexTag ->
            val pill = LayoutInflater.from(rowContainer.context)
                .inflate(R.layout.common_hex_tag_pill, rowContainer, false)

            // Two parts, two actions: the name edits the tag, the count searches the Stream by it
            val tvName = pill.findViewById<TextView>(R.id.tv_pill_name)
            tvName.text = hexTag.name
            pill.findViewById<TextView>(R.id.tv_pill_count).text = hexTag.usageCount.toString()

            if (isMergeMode) {
                bindMergePill(pill, hexTag.name)
                rowContainer.addView(pill)
                return@forEach
            }

            tvName.setOnClickListener { onTagTap(hexTag.name) }
            pill.findViewById<View>(R.id.ll_pill_search).setOnClickListener { onTagSearch(hexTag.name) }

            // Deletion stays on the whole pill, whichever part is held
            val onLongPress = View.OnLongClickListener {
                onTagLongPress(hexTag.name)
                true
            }
            tvName.setOnLongClickListener(onLongPress)
            pill.findViewById<View>(R.id.ll_pill_search).setOnLongClickListener(onLongPress)

            rowContainer.addView(pill)
        }
    }

    private fun bindMergePill(pill: View, tagName: String) {
        if (tagName == mergeTargetName) {
            pill.setBackgroundResource(R.drawable.shape_hex_chip_merge_target)
        }

        val onPick = View.OnClickListener { onTagMergePick(tagName) }
        listOf(pill.findViewById<View>(R.id.tv_pill_name), pill.findViewById<View>(R.id.ll_pill_search))
            .forEach { pillPart ->
                pillPart.setOnClickListener(onPick)
                pillPart.setOnLongClickListener(null)
                pillPart.isLongClickable = false
            }
    }

    override fun getItemCount(): Int = rows.size
}
