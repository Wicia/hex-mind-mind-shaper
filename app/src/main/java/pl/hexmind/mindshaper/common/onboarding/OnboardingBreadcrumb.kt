package pl.hexmind.mindshaper.common.onboarding

/**
 * Breadcrumb waiting to be opened - opened once, never comes back until the tips are turned off (reset).
 * Every entry is covered by OnboardingManager.hasAnyProgress and resetAll.
 */
enum class OnboardingBreadcrumb(
    val settingsParamName: String
) {
    // Thoughts tasks card in the onboarding review
    THOUGHTS("onb_breadcrumb_thoughts_shown")
}
