package pl.hexmind.mindshaper.common.onboarding

/**
 * Single step in onboarding process
 */
enum class OnboardingProgressStep(
    val settingsParamName: String,
) {
    WELCOME_TOOLTIP_SHOWN("onb_welcome_tooltip_shown"),
    HOME_TOOLTIP_SHOWN("onb_home_tooltip_shown"),
    SETTINGS_TOOLTIP_SHOWN("onb_settings_tooltip_shown"),
    DETAILS_TOOLTIP_SHOWN("onb_details_tooltip_shown"),
    CREATION_TOOLTIP_SHOWN("onb_creation_tooltip_shown"),
    CAROUSEL_TOOLTIP_SHOWN("onb_carousel_tooltip_shown");
}