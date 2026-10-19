package pl.hexmind.mindshaper.activities.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.LinearProgressIndicator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.activities.CoreActivity
import pl.hexmind.mindshaper.activities.capture.CaptureActivity
import pl.hexmind.mindshaper.activities.details.DetailsActivity
import pl.hexmind.mindshaper.activities.home.HomeActivity
import pl.hexmind.mindshaper.activities.metadata.MetadataActivity
import pl.hexmind.mindshaper.activities.settings.SettingsActivity
import pl.hexmind.mindshaper.activities.stream.StreamActivity
import pl.hexmind.mindshaper.common.onboarding.OnboardingSection
import pl.hexmind.mindshaper.common.ui.dialogs.ActionsDialog
import pl.hexmind.mindshaper.common.ui.views.HexSwitchRow
import pl.hexmind.mindshaper.services.ThoughtsService
import javax.inject.Inject

/**
 * Onboarding review: per screen, which tips were seen, with a way back to the screen and a reset.
 */
@AndroidEntryPoint
class OnboardingActivity : CoreActivity() {

    @Inject
    lateinit var thoughtsService: ThoughtsService

    private lateinit var rvOnboardingSections: RecyclerView
    private lateinit var onboardingAdapter: OnboardingSectionsAdapter
    private lateinit var lpiOnboardingProgress: LinearProgressIndicator
    private lateinit var tvOnboardingProgressPercent: TextView
    private lateinit var switchOnboardingTips: HexSwitchRow
    private lateinit var tvOnboardingTipsHint: View
    private lateinit var llOnboardingOptions: View

    // One card per functionality: Stream and Details are the same one
    private val onboardingCards = listOf(
        listOf(OnboardingSection.HOME),
        listOf(OnboardingSection.SETTINGS),
        listOf(OnboardingSection.METADATA),
        listOf(OnboardingSection.CAPTURE),
        listOf(OnboardingSection.STREAM, OnboardingSection.DETAILS)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.onboarding_activity)

        setupHeader(R.drawable.ic_activity_onboarding, R.string.workshop_section_onboarding)
        lpiOnboardingProgress = findViewById(R.id.lpi_onboarding_progress)
        tvOnboardingProgressPercent = findViewById(R.id.tv_onboarding_progress_percent)
        setupTipsToggle()
        setupOnboardingList()
    }

    // The same setting as the welcome choice - the screen stays reachable, so tips can be turned back on
    private fun setupTipsToggle() {
        switchOnboardingTips = findViewById(R.id.switch_onboarding_tips)
        tvOnboardingTipsHint = findViewById(R.id.tv_onboarding_tips_hint)
        llOnboardingOptions = findViewById(R.id.ll_onboarding_options)

        switchOnboardingTips.isChecked = appSettingsStorage.isOnboardingEnabled()
        switchOnboardingTips.setOnCheckedChangeListener { _, isChecked ->
            appSettingsStorage.setOnboardingEnabled(isChecked)
            syncTipsOptionsVisibility()
        }

        syncTipsOptionsVisibility()
    }

    // Off = progress and sections hidden, a hint about the tips in their place
    private fun syncTipsOptionsVisibility() {
        val enabled = switchOnboardingTips.isChecked
        llOnboardingOptions.isVisible = enabled
        tvOnboardingTipsHint.isVisible = !enabled
    }

    override fun onResume() {
        super.onResume()
        // Tooltips may have been seen on another screen in the meantime
        onboardingAdapter.notifyDataSetChanged()
        refreshProgress(animate = false)
    }

    // Counts the same tips the list shows, so the bar and the rows below always agree
    private fun refreshProgress(animate: Boolean) {
        val tips = OnboardingSection.entries.flatMap { section -> onboardingManager.getSectionTips(section) }
        if (tips.isEmpty()) return

        val seenCount = tips.count { tip -> tip.wasSeen }
        lpiOnboardingProgress.max = tips.size
        lpiOnboardingProgress.setProgressCompat(seenCount, animate)

        val percent = seenCount * 100 / tips.size
        tvOnboardingProgressPercent.text = getString(R.string.workshop_onboarding_progress_percent, percent)
    }

    private fun setupOnboardingList() {
        rvOnboardingSections = findViewById(R.id.rv_onboarding_sections)

        onboardingAdapter = OnboardingSectionsAdapter(
            cards          = onboardingCards,
            onOpenScreen   = { section -> openSectionScreen(section) },
            onResetSection = { section -> showResetOnboardingDialog(section) },
            tipsProvider   = { section -> onboardingManager.getSectionTips(section) }
        )

        rvOnboardingSections.apply {
            layoutManager = LinearLayoutManager(this@OnboardingActivity)
            adapter = onboardingAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun showResetOnboardingDialog(section: OnboardingSection) {
        ActionsDialog.Builder(this)
            .setTitle(getString(R.string.workshop_dialog_reset_onboarding_title))
            .setDescription(getString(R.string.workshop_dialog_reset_onboarding))
            .setPrimaryAction(getString(R.string.workshop_dialog_reset_onboarding_yes)) {
                onboardingManager.resetSection(section)
                onboardingAdapter.notifyDataSetChanged()
                refreshProgress(animate = true)
            }
            .show()
    }

    private fun openSectionScreen(section: OnboardingSection) {
        val intent = when (section) {
            OnboardingSection.HOME     -> Intent(this, HomeActivity::class.java)
            OnboardingSection.SETTINGS -> Intent(this, SettingsActivity::class.java)
            OnboardingSection.METADATA -> Intent(this, MetadataActivity::class.java)
            OnboardingSection.CAPTURE  -> Intent(this, CaptureActivity::class.java)
            OnboardingSection.STREAM   -> Intent(this, StreamActivity::class.java)
            OnboardingSection.DETAILS  -> return openNewestThought() // "Go to Details" needs a thought to open
        }

        startActivity(intent)
    }

    private fun openNewestThought() {
        lifecycleScope.launch {
            val thoughtId = thoughtsService.getNewestThoughtId()

            if (thoughtId == null)
                showShortToast(R.string.workshop_onboarding_no_thoughts)
            else {
                val intent = Intent(this@OnboardingActivity, DetailsActivity::class.java)
                    .putExtra(DetailsActivity.P_SELECTED_THOUGHT_ID, thoughtId)

                startActivity(intent)
            }
        }
    }
}
