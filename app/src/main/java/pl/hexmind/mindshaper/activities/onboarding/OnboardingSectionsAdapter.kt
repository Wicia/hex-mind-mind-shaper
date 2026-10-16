package pl.hexmind.mindshaper.activities.onboarding

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.common.onboarding.OnboardingSection
import pl.hexmind.mindshaper.common.onboarding.OnboardingTipStatus

class OnboardingSectionsAdapter(
    private val cards: List<List<OnboardingSection>>,
    private val onOpenScreen: (OnboardingSection) -> Unit,
    private val onResetSection: (OnboardingSection) -> Unit,
    private val tipsProvider: (OnboardingSection) -> List<OnboardingTipStatus>
) : RecyclerView.Adapter<OnboardingSectionsAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val llSections: LinearLayout = itemView.findViewById(R.id.ll_card_sections)
    }

    private class SectionViews(sectionView: View) {
        val ivIcon: ImageView        = sectionView.findViewById(R.id.iv_section_icon)
        val tvLabel: TextView        = sectionView.findViewById(R.id.tv_section_label)
        val tvCounter: TextView      = sectionView.findViewById(R.id.tv_section_counter)
        val btnOpen: MaterialButton  = sectionView.findViewById(R.id.btn_section_open)
        val btnReset: MaterialButton = sectionView.findViewById(R.id.btn_section_reset)
        val llTips: LinearLayout     = sectionView.findViewById(R.id.ll_section_tips)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.onboarding_card_item, parent, false)

        return ViewHolder(view)
    }

    override fun getItemCount(): Int = cards.size

    // Section items are rebuilt on every bind: a card holds at most a few of them
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.llSections.removeAllViews()
        val inflater = LayoutInflater.from(holder.itemView.context)

        cards[position].forEach { section ->
            val sectionView = inflater.inflate(R.layout.onboarding_section_item, holder.llSections, false)
            bindSection(SectionViews(sectionView), section)
            holder.llSections.addView(sectionView)
        }
    }

    private fun bindSection(views: SectionViews, section: OnboardingSection) {
        val context = views.tvCounter.context
        val tips = tipsProvider(section)

        views.ivIcon.setImageResource(section.iconRes)
        views.tvLabel.setText(section.labelRes)
        views.tvCounter.text = context.getString(
            R.string.workshop_onboarding_section_counter,
            tips.count { tip -> tip.wasSeen },
            tips.size
        )

        bindTips(views.llTips, tips)

        views.btnOpen.setOnClickListener { onOpenScreen(section) }
        views.btnReset.setOnClickListener { onResetSection(section) }
    }

    private fun bindTips(llTips: LinearLayout, tips: List<OnboardingTipStatus>) {
        llTips.removeAllViews()
        val inflater = LayoutInflater.from(llTips.context)

        tips.forEach { tip ->
            val row = inflater.inflate(R.layout.onboarding_tip_item, llTips, false)
            bindTipRow(row, tip)
            llTips.addView(row)
        }
    }

    private fun bindTipRow(row: View, tip: OnboardingTipStatus) {
        val context = row.context
        val cpiRing: CircularProgressIndicator = row.findViewById(R.id.cpi_tip_ring)
        val tvRingLabel: TextView              = row.findViewById(R.id.tv_tip_ring_label)
        val tvTitle: TextView                  = row.findViewById(R.id.tv_tip_title)

        // Ring is always full - colour alone carries the seen/not-seen meaning
        cpiRing.setProgressCompat(100, false)

        val ringColor = ContextCompat.getColor(
            context, if (tip.wasSeen) R.color.importance_low else R.color._orange_lvl_2
        )
        cpiRing.setIndicatorColor(ringColor)
        cpiRing.trackColor = ringColor

        tvRingLabel.text = if (tip.wasSeen) "\u2713" else ""
        tvRingLabel.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))

        tvTitle.setText(tip.titleRes)
        tvTitle.setTextColor(
            ContextCompat.getColor(
                context, if (tip.wasSeen) R.color.text_primary else R.color.text_secondary
            )
        )
    }
}
