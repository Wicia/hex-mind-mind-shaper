package pl.hexmind.mindshaper.common.onboarding

import androidx.annotation.StringRes
import pl.hexmind.mindshaper.R

/**
 * Short flow to try in the app - independent of other tasks.
 * One point per feature used, each scored once and by the app after the action.
 * Done after requiredCount points - fewer than all when any of them will do
 */
enum class OnboardingTask(
    val settingsParamName: String,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val points: List<OnboardingTaskPoint>,
    val requiredCount: Int = points.size
) {
    // Scored by new thoughts only - editing a thought does not count
    CAPTURE_THOUGHT(
        "onb_task_capture_thought_points",
        R.string.workshop_onboarding_task_capture_title,
        R.string.workshop_onboarding_task_capture_desc,
        listOf(OnboardingTaskPoint.SUBJECT, OnboardingTaskPoint.HEX_TAG, OnboardingTaskPoint.NOTE)
    ),

    // Scored by every sort chosen in the dialog, the default one too - direction alone does not count
    SORT_STREAM(
        "onb_task_sort_stream_points",
        R.string.workshop_onboarding_task_sort_title,
        R.string.workshop_onboarding_task_sort_desc,
        listOf(
            OnboardingTaskPoint.SORT_VALUE, OnboardingTaskPoint.SORT_CREATED_AT, OnboardingTaskPoint.SORT_UPDATED_AT,
            OnboardingTaskPoint.SORT_SUBJECT, OnboardingTaskPoint.SORT_PERSON, OnboardingTaskPoint.SORT_PROJECT
        ),
        requiredCount = 3
    ),

    // Scored by searches confirmed in the search sheet - removing a criterion or arriving with one does not count
    SEARCH_STREAM(
        "onb_task_search_stream_points",
        R.string.workshop_onboarding_task_search_title,
        R.string.workshop_onboarding_task_search_desc,
        listOf(OnboardingTaskPoint.SEARCH_SUBJECT, OnboardingTaskPoint.SEARCH_HEX_TAG)
    ),

    // Scored by filters applied in the filter sheet - thought states alone do not count
    FILTER_STREAM(
        "onb_task_filter_stream_points",
        R.string.workshop_onboarding_task_filter_title,
        R.string.workshop_onboarding_task_filter_desc,
        listOf(OnboardingTaskPoint.FILTER_DOMAIN, OnboardingTaskPoint.FILTER_CREATED_PERIOD)
    );
}

/**
 * Feature whose use scores a task point; the key is what gets stored
 */
enum class OnboardingTaskPoint(val key: String) {
    SUBJECT("subject"),
    HEX_TAG("hex_tag"),   // person or project
    NOTE("note"),
    SORT_VALUE("sort_value"),
    SORT_CREATED_AT("sort_created_at"),
    SORT_UPDATED_AT("sort_updated_at"),
    SORT_SUBJECT("sort_subject"),
    SORT_PERSON("sort_person"),
    SORT_PROJECT("sort_project"),
    SEARCH_SUBJECT("search_subject"),
    SEARCH_HEX_TAG("search_hex_tag"),   // person or project
    FILTER_DOMAIN("filter_domain"),
    FILTER_CREATED_PERIOD("filter_created_period");
}
