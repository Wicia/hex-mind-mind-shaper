package pl.hexmind.mindshaper.common.onboarding

/**
 * Single step in onboarding process
 */
enum class OnboardingProgressStep(
    val settingsParamName: String,
) {
    HOME_TOOLTIP("onb_home_tooltip_shown"),

    // == SETTINGS ==
    SETTINGS_ENTRY_TOOLTIP("onb_settings_entry_tooltip_shown"),
    SETTINGS_YOUR_NAME_TOOLTIP("onb_settings_your_name_tooltip_shown"),
    SETTINGS_THOUGHT_VALUE_TOOLTIP("onb_settings_thought_value_tooltip_shown"),
    SETTINGS_DOMAINS_TOOLTIP("onb_settings_domains_tooltip_shown"),
    SETTINGS_BACKUP_TOOLTIP("onb_settings_backup_tooltip_shown"),

    DETAILS_TOOLTIP("onb_details_tooltip_shown"),

    // == CAPTURING ==
    CAPTURE_ENTRY_TOOLTIP("onb_creation_entry_tooltip_shown"),
    CAPTURE_HEXTAGS_TOOLTIP("onb_creation_hextags_tooltip_shown"),

    STREAM_TOOLTIP("onb_stream_tooltip_shown");
}