package pl.hexmind.mindshaper.common.onboarding

/**
 * Single step in onboarding process
 */
enum class OnboardingProgressStep(
    val settingsParamName: String,
) {
    WELCOME_TOOLTIP("onb_welcome_tooltip_shown"),
    HOME_TOOLTIP("onb_home_tooltip_shown"),
    SETTINGS_TOOLTIP("onb_settings_tooltip_shown"),
    DETAILS_TOOLTIP("onb_details_tooltip_shown"),
    CAPTURE_TOOLTIP("onb_creation_tooltip_shown"),
    STREAM_TOOLTIP("onb_stream_tooltip_shown");
}