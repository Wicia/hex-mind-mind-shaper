package pl.hexmind.mindshaper.activities.metadata

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.database.models.HexTagUsage

class MetadataTagRowAdapter(
    private val onTagTap: (tagName: String) -> Unit = {}
) : RecyclerView.Adapter<MetadataTagRowAdapter.RowViewHolder>() {

    private var rows: List<List<HexTagUsage>> = emptyList()

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

        rows[position].forEach { personTag ->
            val pill = LayoutInflater.from(rowContainer.context)
                .inflate(R.layout.common_hex_tag_pill, rowContainer, false) as TextView

            pill.text = "${personTag.name}$USAGE_SEPARATOR${personTag.usageCount}"
            pill.setOnClickListener { onTagTap(personTag.name) }
            rowContainer.addView(pill)
        }
    }

    override fun getItemCount(): Int = rows.size

    private companion object {
        const val USAGE_SEPARATOR = " • "
    }
}
