package pl.hexmind.mindshaper.common.onboarding

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.res.ResourcesCompat
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.common.ui.DialogTooltips
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
            TooltipItemConfig(
                textRes = R.string.home_catching_tooltips_1,
                iconRes = R.drawable.ic_catching_thought
            )
        ),

        // SETTINGS
        OnboardingProgressStep.SETTINGS_TOOLTIP to listOf(
            TooltipItemConfig(
                textRes = R.string.settings_possibilities_tooltips_1,
            ),
            TooltipItemConfig(
                textRes = R.string.settings_possibilities_tooltips_2
            ),
            TooltipItemConfig(
                textRes = R.string.settings_possibilities_tooltips_3
            )
        ),

        // CAROUSEL
        OnboardingProgressStep.CAROUSEL_TOOLTIP to listOf(
            TooltipItemConfig(
                textRes = R.string.carousel_possibilities_tooltips_1,
            ),
            TooltipItemConfig(
                textRes = R.string.carousel_possibilities_tooltips_2
            ),
            TooltipItemConfig(
                textRes = R.string.carousel_possibilities_tooltips_3
            ),
            TooltipItemConfig(
                textRes = R.string.carousel_possibilities_tooltips_4
            )
        ),

        // DETAILS
        OnboardingProgressStep.DETAILS_TOOLTIP to listOf(
            TooltipItemConfig(
                textRes = R.string.details_possibilities_tooltips_1,
            ),
            TooltipItemConfig(
                textRes = R.string.details_possibilities_tooltips_2
            ),
            TooltipItemConfig(
                textRes = R.string.details_possibilities_tooltips_3,
            ),
            TooltipItemConfig(
                textRes = R.string.details_possibilities_tooltips_4
            )
        ),

        // CAPTURING THOUGHTS
        OnboardingProgressStep.CAPTURE_TOOLTIP to listOf(
            TooltipItemConfig(
                textRes = R.string.catching_possibilities_tooltips_1,
            ),
            TooltipItemConfig(
                textRes = R.string.catching_possibilities_tooltips_2,
                iconRes = R.drawable.ic_hex_tags
            ),
            TooltipItemConfig(
                textRes = R.string.catching_possibilities_tooltips_3
            )
        )
    )

    fun showTooltipForStep(step: OnboardingProgressStep, context: Context){
        if (wasTooltipShown(step)) {
            return
        }

        val tooltipConfig = tooltipsConfig[step]

        val builder = DialogTooltips.Builder(context)
            .setTitle("Jak korzystać z aplikacji?")
            .setOnDismissAction {
                markTooltipShown(step)
            }

        tooltipConfig?.forEach { entry ->
            val icon = entry.iconRes?.let { ResourcesCompat.getDrawable(
                context.resources,
                entry.iconRes,
                context.theme
            )}
            val text = context.getString(entry.textRes)

            builder.addTooltip(text, icon)
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

data class TooltipItemConfig(@StringRes val textRes: Int, @DrawableRes val iconRes: Int? = null )