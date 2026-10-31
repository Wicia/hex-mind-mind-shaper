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
    val requiredCount: Int = points.size
) {
    // Scored by new thoughts only - editing a thought does not count
    // Guided while active: Stream fab -> bullet button in the note dialog -> subject and Save
    CAPTURE_THOUGHT(
        "onb_task_capture_thought_points",
        R.string.workshop_onboarding_task_capture_title,
        R.string.workshop_onboarding_task_capture_desc,
        listOf(OnboardingTaskPoint.SUBJECT, OnboardingTaskPoint.NOTE)
    );
}

/**
 * Feature whose use scores a task point; the key is what gets stored
 */
enum class OnboardingTaskPoint(val key: String) {
    SUBJECT("subject"),
    NOTE("note");
}

/**
 * Breadcrumb (spotlight) of an active task - each seen once per run; the key is what gets stored
 */
enum class OnboardingTaskGuide(val key: String) {
    STREAM_CAPTURE("stream_capture"),       // Stream fab
    NOTE_BULLET("note_bullet"),             // bullet button in the note dialog
    SUBJECT_AND_SAVE("subject_and_save");   // subject, then Save - one guide in two steps
}
