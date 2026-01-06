package pl.hexmind.mindshaper.common.onboarding

import android.content.Context
import androidx.annotation.DrawableRes
import pl.hexmind.mindshaper.common.ui.CommonActionsDialog
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
    fun showsSimpleTooltip(
        context: Context,
        title: String,
        info: String,
        @DrawableRes drawableResId: Int? = null,
        step: OnboardingProgressStep
    ) {
        if (wasTooltipShown(step)) {
            return
        }

        CommonActionsDialog.Builder(context)
            .setTitle(title)
            .setDescription(info)
            .setStandardAction("Dzięki") {
                markTooltipShown(step)
            }
            .setIconResId(drawableResId)
            .show()
    }

    fun wasTooltipShown(step: OnboardingProgressStep): Boolean {
        return appSettingsStorage.wasOnboardingTooltipShown(step.settingsParamName)
    }

    fun markTooltipShown(step: OnboardingProgressStep) {
        appSettingsStorage.markOnboardingTooltipShown(step.settingsParamName)
    }
}