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
            TooltipItemConfig(titleRes = R.string.home_capturing_button_title, textRes = R.string.home_capturing_button_tooltip, iconRes = R.drawable.ic_capture_thought),
            TooltipItemConfig(titleRes = R.string.home_navigation_button_title, textRes = R.string.home_navigation_button_tooltip, tracked = false)
        ),

        // == SETTINGS ==
        OnboardingProgressStep.SETTINGS_ENTRY_TOOLTIP to listOf(
            TooltipItemConfig(titleRes = R.string.settings_entry_title_1, textRes = R.string.settings_entry_tooltip_1),
            TooltipItemConfig(titleRes = R.string.settings_entry_title_2, textRes = R.string.settings_entry_tooltip_2, tracked = false)
        ),
        OnboardingProgressStep.SETTINGS_YOUR_NAME_TOOLTIP to listOf(
            TooltipItemConfig(titleRes = R.string.settings_your_name_title, textRes = R.string.settings_your_name_tooltip)
        ),
        OnboardingProgressStep.SETTINGS_DOMAINS_TOOLTIP to listOf(
            TooltipItemConfig(titleRes = R.string.settings_domains_title, textRes = R.string.settings_domains_tooltip),
        ),
        OnboardingProgressStep.SETTINGS_THOUGHT_VALUE_TOOLTIP to listOf(
            TooltipItemConfig(titleRes = R.string.settings_thought_value_title, textRes = R.string.settings_thought_value_tooltip),
        ),
        OnboardingProgressStep.SETTINGS_BACKUP_TOOLTIP to listOf(
            TooltipItemConfig(titleRes = R.string.settings_backup_title_1, textRes = R.string.settings_backup_tooltip_1),
            TooltipItemConfig(titleRes = R.string.settings_backup_title_2, textRes = R.string.settings_backup_tooltip_2, tracked = false)
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
        if (wasTooltipShown(step)) {
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
}

/**
 * Review panel grouping. Order here IS the order shown in the Workshop card.
 */
enum class OnboardingSection(
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
    val steps: List<OnboardingProgressStep>
) {
    HOME(
        R.string.workshop_onboarding_section_home,
        R.drawable.ic_activity_home,
        listOf(OnboardingProgressStep.HOME_TOOLTIP)
    ),
    SETTINGS(
        R.string.workshop_onboarding_section_settings,
        R.drawable.ic_activity_settings,
        listOf(
            OnboardingProgressStep.SETTINGS_ENTRY_TOOLTIP,
            OnboardingProgressStep.SETTINGS_YOUR_NAME_TOOLTIP,
            OnboardingProgressStep.SETTINGS_DOMAINS_TOOLTIP,
            OnboardingProgressStep.SETTINGS_THOUGHT_VALUE_TOOLTIP,
            OnboardingProgressStep.SETTINGS_BACKUP_TOOLTIP
        )
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