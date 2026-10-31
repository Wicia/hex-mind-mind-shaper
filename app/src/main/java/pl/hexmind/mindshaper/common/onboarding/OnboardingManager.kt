package pl.hexmind.mindshaper.common.onboarding

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.res.ResourcesCompat
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.common.ui.dialogs.GuideDialog
import pl.hexmind.mindshaper.services.AppSettingsStorage
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Responsible for onboarding process for user
 */
@Singleton
class OnboardingManager @Inject constructor(
    private val appSettingsStorage: AppSettingsStorage
) {
    val tooltipsConfig: Map<OnboardingProgressStep, List<TooltipItemConfig>> = mapOf(

        // HOME
        OnboardingProgressStep.HOME_TOOLTIP to listOf(
            TooltipItemConfig(titleRes = R.string.home_capturing_button_title, textRes = R.string.home_capturing_button_tooltip, iconRes = R.drawable.ic_capture_thought)
        ),

        // == SETTINGS ==
        OnboardingProgressStep.SETTINGS_ENTRY_TOOLTIP to listOf(
            TooltipItemConfig(titleRes = R.string.settings_entry_title_1, textRes = R.string.settings_entry_tooltip_1),
            TooltipItemConfig(titleRes = R.string.settings_entry_title_2, textRes = R.string.settings_entry_tooltip_2, tracked = false)
        ),
        OnboardingProgressStep.SETTINGS_YOUR_NAME_TOOLTIP to listOf(
            TooltipItemConfig(titleRes = R.string.settings_your_name_title, textRes = R.string.settings_your_name_tooltip)
        ),
        OnboardingProgressStep.SETTINGS_THOUGHT_VALUE_TOOLTIP to listOf(
            TooltipItemConfig(titleRes = R.string.settings_thought_value_title, textRes = R.string.settings_thought_value_tooltip),
        ),
        OnboardingProgressStep.SETTINGS_BACKUP_TOOLTIP to listOf(
            TooltipItemConfig(titleRes = R.string.settings_backup_title_1, textRes = R.string.settings_backup_tooltip_1),
            TooltipItemConfig(titleRes = R.string.settings_backup_title_2, textRes = R.string.settings_backup_tooltip_2, tracked = false)
        ),

        // == METADATA ==
        OnboardingProgressStep.METADATA_DOMAINS_TOOLTIP to listOf(
            TooltipItemConfig(titleRes = R.string.metadata_domains_title, textRes = R.string.metadata_domains_tooltip),
        ),

        // STREAM
        OnboardingProgressStep.STREAM_TOOLTIP to listOf(
            TooltipItemConfig(titleRes = R.string.stream_entry_title, textRes = R.string.stream_entry_tooltip),
            TooltipItemConfig(titleRes = R.string.stream_entry_searching_title, textRes = R.string.stream_entry_searching_tooltip),
            TooltipItemConfig(titleRes = R.string.stream_deleting_title, textRes = R.string.stream_deleting_tooltip),
            TooltipItemConfig(titleRes = R.string.stream_details_title, textRes = R.string.stream_details_tooltip)
        ),

        // DETAILS
        OnboardingProgressStep.DETAILS_TOOLTIP to listOf(
            TooltipItemConfig(titleRes = R.string.details_entry_title, textRes = R.string.details_entry_tooltip),
            TooltipItemConfig(titleRes = R.string.details_fields_title, textRes = R.string.details_fields_tooltip),
            TooltipItemConfig(titleRes = R.string.details_value_title, textRes = R.string.details_value_tooltip),
            TooltipItemConfig(titleRes = R.string.details_extra_forms_title, textRes = R.string.details_extra_forms_tooltip)
        ),

        // === CAPTURING THOUGHTS ===
        // -> ENTRY (+ BUTTONS)
        OnboardingProgressStep.CAPTURE_ENTRY_TOOLTIP to listOf(
            TooltipItemConfig(titleRes = R.string.capture_entry_title, textRes = R.string.capture_entry_tooltip),
            TooltipItemConfig(titleRes = R.string.capture_hex_tags_link_title, textRes = R.string.capture_hex_tags_link_tooltip)
        ),
        // -> HEXTAGS
        OnboardingProgressStep.CAPTURE_HEXTAGS_TOOLTIP to listOf(
            TooltipItemConfig(titleRes = R.string.capture_hex_tags_title, textRes = R.string.capture_hex_tags_onb_tooltip),
            TooltipItemConfig(titleRes = R.string.capture_hex_tags_example_title, textRes = R.string.capture_hex_tags_example_tooltip)
        )
    )

    fun showTooltipForStep(step: OnboardingProgressStep, context: Context){
        // Tips are off unless turned on - in the welcome or behind the flask
        if (!appSettingsStorage.isOnboardingEnabled() || wasTooltipShown(step)) {
            return
        }

        val tooltipConfig = tooltipsConfig[step]

        val builder = GuideDialog.Builder(context)
            .setOnDismissAction {
                markTooltipShown(step)
            }

        tooltipConfig?.forEach { tooltipConfig ->
            val title = context.getString(tooltipConfig.titleRes)
            val icon = tooltipConfig.iconRes?.let {
                ResourcesCompat.getDrawable(context.resources, tooltipConfig.iconRes, context.theme
            )}
            val text = context.getString(tooltipConfig.textRes)

            builder.addGuideScreen( text, title, icon)
        }

        builder.show()
    }

    /**
     * Flattens a section into 2 level review rows (section/activity → onb steps + tooltips).
     * Seen state lives per step, not per tooltip, so every tooltip of one-step shares the same marker.
     */
    fun getSectionTips(section: OnboardingSection): List<OnboardingTipStatus> {
        return section.steps.flatMap { step ->
            val seen = wasTooltipShown(step)
            tooltipsConfig[step].orEmpty()
                .filter {
                    tooltipConfig -> tooltipConfig.tracked
                }
                .map {
                    tooltipConfig -> OnboardingTipStatus(
                        titleRes = tooltipConfig.titleRes,
                        wasSeen  = seen
                    )
                }
        }
    }

    fun resetSection(section: OnboardingSection) {
        section.steps.forEach { step ->
            appSettingsStorage.clearOnboardingTooltipShown(step.settingsParamName)
        }
    }

    fun wasTooltipShown(step: OnboardingProgressStep): Boolean {
        return appSettingsStorage.wasOnboardingTooltipShown(step.settingsParamName)
    }

    fun markTooltipShown(step: OnboardingProgressStep) {
        appSettingsStorage.markOnboardingTooltipShown(step.settingsParamName)
    }

    fun getTaskScore(task: OnboardingTask): Int {
        val earnedKeys = appSettingsStorage.getOnboardingTaskPoints(task.settingsParamName)
        return task.points.count { point -> point.key in earnedKeys }.coerceAtMost(task.requiredCount)
    }

    // Scored only while the tips are on - actions from before are never counted back
    fun recordTaskPoints(task: OnboardingTask, points: Set<OnboardingTaskPoint>) {
        if (!appSettingsStorage.isOnboardingEnabled()) return

        val newKeys = points.filter { point -> point in task.points }.map { point -> point.key }
        if (newKeys.isEmpty()) return

        val earnedKeys = appSettingsStorage.getOnboardingTaskPoints(task.settingsParamName)
        appSettingsStorage.setOnboardingTaskPoints(task.settingsParamName, earnedKeys + newKeys)

        if (getActiveTask() != task) return

        // A run counts on its own - a task done before can be run again, the overall progress stays as it was
        val runKeys = appSettingsStorage.getOnboardingTaskPoints(runParamName(task)) + newKeys
        appSettingsStorage.setOnboardingTaskPoints(runParamName(task), runKeys)

        // Run done = nothing more to guide; the screen the task leads back to congratulates once
        if (task.points.count { point -> point.key in runKeys } >= task.requiredCount) {
            deactivateTask()
            completedActiveTask = task
        }
    }

    private fun runParamName(task: OnboardingTask): String = task.settingsParamName + RUN_PARAM_SUFFIX

    private fun runGuidesParamName(task: OnboardingTask): String = task.settingsParamName + RUN_GUIDES_PARAM_SUFFIX

    // A run starts from scratch - its points and the guides already seen
    private fun clearRun(task: OnboardingTask) {
        appSettingsStorage.clearOnboardingTaskPoints(runParamName(task))
        appSettingsStorage.clearOnboardingTaskPoints(runGuidesParamName(task))
    }

    // In memory only - lost with the process, the task stays done anyway
    private var completedActiveTask: OnboardingTask? = null

    // Active task done since the last call - taken once
    fun takeCompletedActiveTask(): OnboardingTask? {
        return completedActiveTask.also { completedActiveTask = null }
    }

    fun isTaskDone(task: OnboardingTask): Boolean {
        return getTaskScore(task) >= task.requiredCount
    }

    fun resetTask(task: OnboardingTask) {
        appSettingsStorage.clearOnboardingTaskPoints(task.settingsParamName)
    }

    fun getActiveTask(): OnboardingTask? {
        val taskKey = appSettingsStorage.getOnboardingActiveTask() ?: return null
        return OnboardingTask.entries.firstOrNull { task -> task.settingsParamName == taskKey }
    }

    // One active task at a time - starting another one replaces it; every start is a new run from scratch
    fun activateTask(task: OnboardingTask) {
        getActiveTask()?.let { activeTask -> clearRun(activeTask) }
        clearRun(task)
        appSettingsStorage.setOnboardingActiveTask(task.settingsParamName)
    }

    fun deactivateTask() {
        getActiveTask()?.let { activeTask -> clearRun(activeTask) }
        appSettingsStorage.setOnboardingActiveTask(null)
    }

    // Screens show the task's breadcrumbs only while it is active and the tips are on
    fun isGuiding(task: OnboardingTask): Boolean {
        return appSettingsStorage.isOnboardingEnabled() && getActiveTask() == task
    }

    // Each breadcrumb of a run is seen once - not again on a return to its screen
    fun shouldShowTaskGuide(task: OnboardingTask, guide: OnboardingTaskGuide): Boolean {
        return isGuiding(task) && guide.key !in appSettingsStorage.getOnboardingTaskPoints(runGuidesParamName(task))
    }

    fun markTaskGuideShown(task: OnboardingTask, guide: OnboardingTaskGuide) {
        val shownKeys = appSettingsStorage.getOnboardingTaskPoints(runGuidesParamName(task)) + guide.key
        appSettingsStorage.setOnboardingTaskPoints(runGuidesParamName(task), shownKeys)
    }

    // Welcome is not a step - it stays out of the review panel and its section resets
    // ! Users from before the welcome already saw the Home tooltip - they skip it
    fun isWelcomePending(): Boolean {
        return !appSettingsStorage.wasOnboardingTooltipShown(WELCOME_SHOWN_KEY)
                && !wasTooltipShown(OnboardingProgressStep.HOME_TOOLTIP)
    }

    fun markWelcomeShown() {
        appSettingsStorage.markOnboardingTooltipShown(WELCOME_SHOWN_KEY)
    }

    fun isBreadcrumbPending(breadcrumb: OnboardingBreadcrumb): Boolean {
        return !appSettingsStorage.wasOnboardingTooltipShown(breadcrumb.settingsParamName)
    }

    fun markBreadcrumbShown(breadcrumb: OnboardingBreadcrumb) {
        appSettingsStorage.markOnboardingTooltipShown(breadcrumb.settingsParamName)
    }

    // Anything to lose by turning the tips off - seen tips, task points or opened breadcrumbs
    fun hasAnyProgress(): Boolean {
        return OnboardingProgressStep.entries.any { step -> wasTooltipShown(step) } ||
                OnboardingTask.entries.any { task -> getTaskScore(task) > 0 } ||
                OnboardingBreadcrumb.entries.any { breadcrumb -> !isBreadcrumbPending(breadcrumb) }
    }

    // Turning the tips off starts them over - the welcome stays seen, it is the choice made at the start
    fun resetAll() {
        OnboardingProgressStep.entries.forEach { step ->
            appSettingsStorage.clearOnboardingTooltipShown(step.settingsParamName)
        }
        OnboardingTask.entries.forEach { task -> resetTask(task) }
        deactivateTask()
        OnboardingBreadcrumb.entries.forEach { breadcrumb ->
            appSettingsStorage.clearOnboardingTooltipShown(breadcrumb.settingsParamName)
        }
    }

    companion object {
        private const val WELCOME_SHOWN_KEY = "onb_welcome_shown"
        private const val RUN_PARAM_SUFFIX = "_run"
        private const val RUN_GUIDES_PARAM_SUFFIX = "_run_guides"
    }
}

/**
 * Review panel grouping: tips reviewed together. Card order and which sections share a card are set in OnboardingActivity.
 */
enum class OnboardingSection(
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
    val steps: List<OnboardingProgressStep>
) {
    HOME(
        R.string.workshop_onboarding_section_home,
        R.drawable.ic_owl,
        listOf(OnboardingProgressStep.HOME_TOOLTIP)
    ),
    SETTINGS(
        R.string.workshop_onboarding_section_settings,
        R.drawable.ic_activity_settings,
        listOf(
            OnboardingProgressStep.SETTINGS_ENTRY_TOOLTIP,
            OnboardingProgressStep.SETTINGS_YOUR_NAME_TOOLTIP,
            OnboardingProgressStep.SETTINGS_THOUGHT_VALUE_TOOLTIP,
            OnboardingProgressStep.SETTINGS_BACKUP_TOOLTIP
        )
    ),
    METADATA(
        R.string.workshop_onboarding_section_metadata,
        R.drawable.ic_activity_metadata,
        listOf(OnboardingProgressStep.METADATA_DOMAINS_TOOLTIP)
    ),
    CAPTURE(
        R.string.workshop_onboarding_section_capture,
        R.drawable.ic_capture_thought,
        listOf(
            OnboardingProgressStep.CAPTURE_ENTRY_TOOLTIP,
            OnboardingProgressStep.CAPTURE_HEXTAGS_TOOLTIP
        )
    ),
    STREAM(
        R.string.workshop_onboarding_section_stream,
        R.drawable.ic_activity_stream,
        listOf(OnboardingProgressStep.STREAM_TOOLTIP)
    ),
    DETAILS(
        R.string.workshop_onboarding_section_details,
        R.drawable.ic_action_thought_link,
        listOf(OnboardingProgressStep.DETAILS_TOOLTIP)
    )
}

data class OnboardingTipStatus(
    @StringRes val titleRes: Int,
    val wasSeen: Boolean
)

data class TooltipItemConfig(
    @StringRes val titleRes: Int,
    @StringRes val textRes: Int,
    @DrawableRes val iconRes: Int? = null,
    val tracked: Boolean = true // for skipping showing tooltips in onb review panel
)