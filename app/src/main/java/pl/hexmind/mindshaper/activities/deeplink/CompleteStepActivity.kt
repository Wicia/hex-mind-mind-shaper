package pl.hexmind.mindshaper.activities.deeplink

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.services.GoalsService
import pl.hexmind.mindshaper.services.StepProgressResult
import javax.inject.Inject

// TODO / core / no-UI handler for the calendar's "Ukończ" link - bumps step progress then finishes, so the tap just closes the event preview
@AndroidEntryPoint
class CompleteStepActivity : AppCompatActivity() {

    @Inject
    lateinit var goalsService: GoalsService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val stepId = parseStepId(intent?.data?.pathSegments)
        if (stepId == null) {
            finish()
            return
        }

        lifecycleScope.launch {
            showFeedback(goalsService.incrementStepProgress(stepId))
            finish()
        }
    }

    private fun showFeedback(result: StepProgressResult) {
        val message = when (result) {
            is StepProgressResult.Incremented ->
                if (result.isStepFinished)
                    getString(R.string.deeplink_step_finished)
                else
                    getString(R.string.deeplink_step_counted, result.current, result.max)

            StepProgressResult.AlreadyComplete ->
                getString(R.string.deeplink_step_already_complete)

            StepProgressResult.NotFound -> return
        }

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // Expects .../step/{id}/complete
    private fun parseStepId(segments: List<String>?): Int? {
        if (segments == null || segments.size < 2) return null
        if (segments[0] != "step") return null

        return segments[1].toIntOrNull()
    }
}
