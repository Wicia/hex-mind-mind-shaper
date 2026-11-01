package pl.hexmind.mindshaper.common.onboarding

import androidx.annotation.StringRes
import pl.hexmind.mindshaper.R

/**
 * Short flow to try in the app - independent of other tasks.
 * Started on purpose (one active at a time) - then its screens show breadcrumbs, step by step.
 * One point per feature used, each scored once and by the app after the action.
 * Done after requiredCount points - fewer than all when any of them will do
 */
enum class OnboardingTask(
    val settingsParamName: String,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val points: List<OnboardingTaskPoint>,
    val requiredCount: Int = points.size,
    val needsThought: Boolean = false   // started on an existing thought - none yet = not started
) {
    // Scored by new thoughts only - editing a thought does not count
    // Guided while active: Stream fab -> bullet button in the note dialog -> subject and Save
    CAPTURE_THOUGHT(
        "onb_task_capture_thought_points",
        R.string.workshop_onboarding_task_capture_title,
        R.string.workshop_onboarding_task_capture_desc,
        listOf(OnboardingTaskPoint.SUBJECT, OnboardingTaskPoint.NOTE)
    ),

    // Scored by a project tag added in Details and a search by tag from Metadata - the tag name is only an example
    // Guided while active: thought in Stream -> tags button in Details -> project field and Save in the tags sheet
    //   -> navigation panel -> domains tab and tag search
    TAG_THOUGHT(
        "onb_task_tag_thought_points",
        R.string.workshop_onboarding_task_tag_title,
        R.string.workshop_onboarding_task_tag_desc,
        listOf(OnboardingTaskPoint.PROJECT_TAG, OnboardingTaskPoint.TAG_SEARCH),
        needsThought = true
    );
}

/**
 * Feature whose use scores a task point; the key is what gets stored
 */
enum class OnboardingTaskPoint(val key: String) {
    SUBJECT("subject"),
    NOTE("note"),
    PROJECT_TAG("project_tag"),
    TAG_SEARCH("tag_search");
}

/**
 * Breadcrumb (spotlight) of an active task - each seen once per run; the key is what gets stored
 */
enum class OnboardingTaskGuide(val key: String) {
    STREAM_CAPTURE("stream_capture"),       // Stream fab
    NOTE_BULLET("note_bullet"),             // bullet button in the note dialog
    SUBJECT_AND_SAVE("subject_and_save"),   // subject, then Save - one guide in two steps
    STREAM_THOUGHT("stream_thought"),       // current thought card in Stream
    DETAILS_TAGS("details_tags"),           // tags button in Details
    TAGS_SHEET("tags_sheet"),               // project field, then Save in the tags sheet
    NAV_METADATA("nav_metadata"),           // navigation panel toggle, then its Metadata item
    METADATA_SEARCH("metadata_search");     // Domains tab, Persons and Projects tabs, then the search part of a project pill
}
