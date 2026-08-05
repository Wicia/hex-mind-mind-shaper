package pl.hexmind.mindshaper.activities.workshop

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
    private val onOpenScreen: (OnboardingSection) -> Unit,
    private val onResetSection: (OnboardingSection) -> Unit,
    private val tipsProvider: (OnboardingSection) -> List<OnboardingTipStatus>
) : RecyclerView.Adapter<OnboardingSectionsAdapter.ViewHolder>() {

    private val sections = OnboardingSection.entries

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivIcon: ImageView        = itemView.findViewById(R.id.iv_section_icon)
        val tvLabel: TextView        = itemView.findViewById(R.id.tv_section_label)
        val tvCounter: TextView      = itemView.findViewById(R.id.tv_section_counter)
        val btnOpen: MaterialButton  = itemView.findViewById(R.id.btn_section_open)
        val btnReset: MaterialButton = itemView.findViewById(R.id.btn_section_reset)
        val llTips: LinearLayout     = itemView.findViewById(R.id.ll_section_tips)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.onboarding_section_item, parent, false)

        return ViewHolder(view)
    }

    override fun getItemCount(): Int = sections.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val section = sections[position]
        val context = holder.itemView.context
        val tips = tipsProvider(section)

        holder.ivIcon.setImageResource(section.iconRes)
        holder.tvLabel.setText(section.labelRes)
        holder.tvCounter.text = context.getString(
            R.string.workshop_onboarding_section_counter,
            tips.count { tip -> tip.wasSeen },
            tips.size
        )

        bindTips(holder, tips)

        holder.btnOpen.setOnClickListener { onOpenScreen(section) }
        holder.btnReset.setOnClickListener { onResetSection(section) }
    }

    // Rows are rebuilt on every bind: the list is short and its length changes only on reset
    private fun bindTips(holder: ViewHolder, tips: List<OnboardingTipStatus>) {
        holder.llTips.removeAllViews()
        val inflater = LayoutInflater.from(holder.itemView.context)

        tips.forEach { tip ->
            val row = inflater.inflate(R.layout.onboarding_tip_item, holder.llTips, false)
            bindTipRow(row, tip)
            holder.llTips.addView(row)
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
