package pl.hexmind.mindshaper.activities.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
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
import pl.hexmind.mindshaper.common.onboarding.OnboardingTask
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
    private lateinit var llOnboardingTasks: LinearLayout

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
        llOnboardingTasks = findViewById(R.id.ll_onboarding_tasks)
        findViewById<MaterialButton>(R.id.btn_onboarding_tasks_open).setOnClickListener {
            startActivity(Intent(this, StreamActivity::class.java))
        }
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
        // Tooltips may have been seen and tasks done on another screen in the meantime
        bindTasks()
        onboardingAdapter.notifyDataSetChanged()
        refreshProgress(animate = false)
    }

    // Counts the same tasks and tips the lists show, so the bar and the rows below always agree
    // ! A task weighs as much as its required points - a 3 point task counts like 3 tips
    private fun refreshProgress(animate: Boolean) {
        val tips = if (TIP_SECTIONS_SHOWN) {
            OnboardingSection.entries.flatMap { section -> onboardingManager.getSectionTips(section) }
        } else {
            emptyList()
        }
        val tasks = OnboardingTask.entries

        val total = tips.size + tasks.sumOf { task -> task.requiredCount }
        if (total == 0) return

        val doneCount = tips.count { tip -> tip.wasSeen } +
                tasks.sumOf { task -> onboardingManager.getTaskScore(task) }
        lpiOnboardingProgress.max = total
        lpiOnboardingProgress.setProgressCompat(doneCount, animate)

        val percent = doneCount * 100 / total
        tvOnboardingProgressPercent.text = getString(R.string.workshop_onboarding_progress_percent, percent)
    }

    // Task rows are rebuilt on every refresh: there are only a few of them
    private fun bindTasks() {
        llOnboardingTasks.removeAllViews()
        val inflater = LayoutInflater.from(this)

        OnboardingTask.entries.forEach { task ->
            val row = inflater.inflate(R.layout.onboarding_task_item, llOnboardingTasks, false)
            bindTaskRow(row, task)
            llOnboardingTasks.addView(row)
        }
    }

    private fun bindTaskRow(row: View, task: OnboardingTask) {
        val cpiRing: CircularProgressIndicator = row.findViewById(R.id.cpi_task_ring)
        val tvRingLabel: TextView              = row.findViewById(R.id.tv_task_ring_label)

        val score = onboardingManager.getTaskScore(task)
        val done = score >= task.requiredCount

        cpiRing.setProgressCompat((score * 100 / task.requiredCount).coerceIn(0, 100), false)
        cpiRing.setIndicatorColor(
            ContextCompat.getColor(this, if (done) R.color.progress_ring_done else R.color._orange_lvl_3)
        )
        cpiRing.trackColor = ContextCompat.getColor(this, if (done) R.color.progress_ring_done else R.color._orange_lvl_1)

        tvRingLabel.setTextColor(
            ContextCompat.getColor(this, if (done) R.color.progress_ring_done else R.color._orange_lvl_3)
        )
        tvRingLabel.text = when {
            done                 -> "✓"
            task.requiredCount > 1 -> score.toString()
            else                 -> ""
        }

        row.findViewById<TextView>(R.id.tv_task_title).setText(task.titleRes)
        row.findViewById<TextView>(R.id.tv_task_description).setText(task.descriptionRes)
        row.findViewById<MaterialButton>(R.id.btn_task_reset).setOnClickListener { showResetTaskDialog(task) }
    }

    private fun showResetTaskDialog(task: OnboardingTask) {
        ActionsDialog.Builder(this)
            .setTitle(getString(R.string.workshop_dialog_reset_onboarding_task_title))
            .setDescription(getString(R.string.workshop_dialog_reset_onboarding_task))
            .setPrimaryAction(getString(R.string.workshop_dialog_reset_onboarding_task_yes)) {
                onboardingManager.resetTask(task)
                bindTasks()
                refreshProgress(animate = true)
            }
            .show()
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
            isVisible = TIP_SECTIONS_SHOWN
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

    companion object {
        // ! No tip is shown on the screens yet (onboarding concept) - their sections stay hidden
        // and out of the progress until the first reworked tip is back
        private const val TIP_SECTIONS_SHOWN = false
    }
}
