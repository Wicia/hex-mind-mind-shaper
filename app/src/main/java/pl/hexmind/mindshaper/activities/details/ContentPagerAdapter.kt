package pl.hexmind.mindshaper.activities.details

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import pl.hexmind.mindshaper.R

/**
 * Adapter for ViewPager2 that manages different content type pages
 */
class ContentPagerAdapter : RecyclerView.Adapter<ContentPagerAdapter.PageViewHolder>() {

    private val layouts = listOf(
        R.layout.page_rich_text,
        R.layout.page_drawing,
        R.layout.page_recording,
        R.layout.page_photo
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
        return PageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        // Binding logic will be added when actual content is implemented
    }

    override fun getItemCount(): Int = layouts.size

    override fun getItemViewType(position: Int): Int = layouts[position]

    class PageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}