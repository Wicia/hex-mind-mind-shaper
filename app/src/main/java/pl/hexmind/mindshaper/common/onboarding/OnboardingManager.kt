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
            TooltipItemConfig(textRes = R.string.home_capturing_button_tooltip, R.string.home_capturing_button_title, iconRes = R.drawable.ic_capture_thought),
            TooltipItemConfig(textRes = R.string.home_navigation_button_tooltip, R.string.home_navigation_button_title)
        ),

        // == SETTINGS ==
        OnboardingProgressStep.SETTINGS_ENTRY_TOOLTIP to listOf(
            TooltipItemConfig(textRes = R.string.settings_entry_tooltip_1, titleRes =  R.string.settings_entry_title_1),
            TooltipItemConfig(textRes = R.string.settings_entry_tooltip_2, titleRes =  R.string.settings_entry_title_2)
        ),
        OnboardingProgressStep.SETTINGS_YOUR_NAME_TOOLTIP to listOf(
            TooltipItemConfig(textRes = R.string.settings_your_name_tooltip, titleRes =  R.string.settings_your_name_title)
        ),
        OnboardingProgressStep.SETTINGS_DOMAINS_TOOLTIP to listOf(
            TooltipItemConfig(textRes = R.string.settings_domains_tooltip, titleRes =  R.string.settings_domains_title),
        ),
        OnboardingProgressStep.SETTINGS_THOUGHT_VALUE_TOOLTIP to listOf(
            TooltipItemConfig(textRes = R.string.settings_thought_value_tooltip, titleRes =  R.string.settings_thought_value_title),
        ),
        OnboardingProgressStep.SETTINGS_BACKUP_TOOLTIP to listOf(
            TooltipItemConfig(textRes = R.string.settings_backup_tooltip_1, titleRes =  R.string.settings_backup_title_1),
            TooltipItemConfig(textRes = R.string.settings_backup_tooltip_2, titleRes =  R.string.settings_backup_title_2)
        ),

        // STREAM
        OnboardingProgressStep.STREAM_TOOLTIP to listOf(
            TooltipItemConfig(textRes = R.string.stream_entry_tooltip, titleRes = R.string.stream_entry_title),
            TooltipItemConfig(textRes = R.string.stream_entry_searching_tooltip, titleRes = R.string.stream_entry_searching_title),
            TooltipItemConfig(textRes = R.string.stream_deleting_tooltip, titleRes = R.string.stream_deleting_title),
            TooltipItemConfig(textRes = R.string.stream_details_tooltip, titleRes = R.string.stream_details_title)
        ),

        // DETAILS
        OnboardingProgressStep.DETAILS_TOOLTIP to listOf(
            TooltipItemConfig(textRes = R.string.details_entry_tooltip, titleRes = R.string.details_entry_title),
            TooltipItemConfig(textRes = R.string.details_fields_tooltip, titleRes = R.string.details_fields_title),
            TooltipItemConfig(textRes = R.string.details_value_tooltip, titleRes = R.string.details_value_title),
            TooltipItemConfig(textRes = R.string.details_extra_forms_tooltip, titleRes = R.string.details_extra_forms_title)
        ),

        // === CAPTURING THOUGHTS ===
        // -> ENTRY (+ BUTTONS)
        OnboardingProgressStep.CAPTURE_ENTRY_TOOLTIP to listOf(
            TooltipItemConfig(textRes = R.string.capturing_entry_tooltip, R.string.capturing_entry_title),
            TooltipItemConfig(textRes = R.string.capturing_hextags_link_tooltip, R.string.capturing_hextags_link_title)
        ),
        // -> HEXTAGS
        OnboardingProgressStep.CAPTURE_HEXTAGS_TOOLTIP to listOf(
            TooltipItemConfig(textRes = R.string.capturing_hextags_tooltip, R.string.capturing_hextags_title),
            TooltipItemConfig(textRes = R.string.capturing_hextags_example_tooltip, R.string.capturing_hextags_example_title)
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
            val title = context.getString(tooltipConfig.titleRes?: R.string.common_onb_dialog_header)
            val icon = tooltipConfig.iconRes?.let {
                ResourcesCompat.getDrawable(context.resources, tooltipConfig.iconRes, context.theme
            )}
            val text = context.getString(tooltipConfig.textRes)

            builder.addGuideScreen( text, title, icon)
        }

        builder.show()
    }

    fun wasTooltipShown(step: OnboardingProgressStep): Boolean {
        return appSettingsStorage.wasOnboardingTooltipShown(step.settingsParamName)
    }

    fun markTooltipShown(step: OnboardingProgressStep) {
        appSettingsStorage.markOnboardingTooltipShown(step.settingsParamName)
    }
}

data class TooltipItemConfig(
    @StringRes val textRes: Int,
    @StringRes val titleRes : Int? = null,
    @DrawableRes val iconRes: Int? = null
)