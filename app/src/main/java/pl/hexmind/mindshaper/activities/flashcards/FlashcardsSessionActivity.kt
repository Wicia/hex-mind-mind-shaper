package pl.hexmind.mindshaper.activities.flashcards

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import dagger.hilt.android.AndroidEntryPoint
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.common.ui.views.content.HexFlashcardsSessionView
import pl.hexmind.mindshaper.services.FlashcardsReview
import pl.hexmind.mindshaper.services.dto.FlashcardDTO
import pl.hexmind.mindshaper.services.dto.FlashcardRating

/**
 * Review session in landscape, full screen (HexFlashcardsSessionView).
 * Every rating is stored right away - "Przerwij" / back = break off, the next session picks up the rest.
 * ! Not a CoreActivity: no navigation bar over the session
 */
@AndroidEntryPoint
class FlashcardsSessionActivity : AppCompatActivity() {

    private val viewModel: FlashcardsSessionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.flashcards_session_activity)
        hideSystemBars()

        val sessionView = findViewById<HexFlashcardsSessionView>(R.id.flashcards_session_view)
        sessionView.setCallback(object : HexFlashcardsSessionView.SessionCallback {
            override fun onRated(flashcard: FlashcardDTO, rating: FlashcardRating) {
                viewModel.rate(flashcard, rating)
            }

            override fun onRatingUndone(flashcard: FlashcardDTO) {
                viewModel.restoreProgress(flashcard)
            }

            override fun onSessionClosed() {
                finish()
            }
        })

        viewModel.plan.observe(this) { plan ->
            // Planned again here - nothing left (e.g. reviewed meanwhile) = nothing to show
            if (plan.isEmpty) {
                finish()
                return@observe
            }
            if (!sessionView.hasQueue) sessionView.setQueue(plan.queue)
        }
    }

    /** Bars come back with a swipe from the edge and hide again on their own */
    private fun hideSystemBars() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    companion object {
        const val EXTRA_SET_ID = "setId"

        /**
         * Nothing to review = no session screen.
         * @param setId null = one session over all the flashcards (sets + loose)
         */
        fun open(context: Context, plan: FlashcardsReview.Plan, setId: Int? = null) {
            if (plan.isEmpty) return
            val intent = Intent(context, FlashcardsSessionActivity::class.java)
            setId?.let { id -> intent.putExtra(EXTRA_SET_ID, id) }
            context.startActivity(intent)
        }
    }
}
