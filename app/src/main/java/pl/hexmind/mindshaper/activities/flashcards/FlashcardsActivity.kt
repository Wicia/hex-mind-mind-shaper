package pl.hexmind.mindshaper.activities.flashcards

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.activities.CoreActivity
import pl.hexmind.mindshaper.common.ui.dialogs.FlashcardEditDialog
import pl.hexmind.mindshaper.common.ui.dialogs.FlashcardsSessionDialog
import pl.hexmind.mindshaper.common.ui.views.HexWavyFrameView
import pl.hexmind.mindshaper.services.dto.FlashcardSetDTO

/**
 * Flashcards screen - today's session widget (one session over all the sets) + the flashcard sets below.
 * Set: tap = preview, rocket = review of just this set, long press = delete.
 * Opened from a reminder: the session starts as soon as the sets are loaded.
 * Opened from the launcher shortcut: the new-set dialog opens right away.
 */
@AndroidEntryPoint
class FlashcardsActivity : CoreActivity() {

    private val viewModel: FlashcardsViewModel by viewModels()

    private lateinit var rvSets: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var tvSetsLabel: TextView
    private lateinit var reviewToday: View
    private lateinit var tvReviewTodayStatus: TextView
    private lateinit var btnReviewToday: MaterialButton
    private lateinit var frameReviewToday: HexWavyFrameView

    // Last sets from the database - due reviews also change with the time, so onResume renders them again
    private var sets: List<FlashcardSetDTO> = emptyList()

    // Reminder's session waiting for the first sets from the database
    private var pendingSession = false

    private val setsAdapter = FlashcardSetsAdapter(
        onSetTap = { set ->
            set.id?.let { setId -> startActivity(FlashcardSetActivity.newIntent(this, setId)) }
        },
        onSetLongPress = { set ->
            set.id?.let { setId -> FlashcardsDeletion.confirmSet(this) { viewModel.deleteSet(setId) } }
        },
        onReviewTap = { set ->
            FlashcardsSessionDialog.open(this, viewModel.planReview(sets, set.id), onRated = viewModel::rate)
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.flashcards_activity)

        // Only on the first creation - a rotation must not start the session again
        if (savedInstanceState == null) {
            pendingSession = intent.getBooleanExtra(EXTRA_START_SESSION, false)
        }

        // Nav overlay + current-screen highlight are added by CoreActivity (onContentChanged / onResume)
        setupHeader(R.drawable.ic_activity_flashcards, R.string.flashcards_title)

        rvSets = findViewById(R.id.rv_sets)
        tvEmpty = findViewById(R.id.tv_sets_empty)

        tvSetsLabel = findViewById(R.id.tv_sets_label)

        reviewToday = findViewById(R.id.inc_review_today)
        tvReviewTodayStatus = reviewToday.findViewById(R.id.tv_review_today_status)
        btnReviewToday = reviewToday.findViewById(R.id.btn_review_today)
        frameReviewToday = reviewToday.findViewById(R.id.frame_review_today)

        rvSets.layoutManager = LinearLayoutManager(this)
        rvSets.adapter = setsAdapter

        btnReviewToday.setOnClickListener {
            FlashcardsSessionDialog.open(this, viewModel.planReview(sets), onRated = viewModel::rate)
        }

        findViewById<FloatingActionButton>(R.id.fab_add_set).setOnClickListener { openNewSet() }

        viewModel.sets.observe(this) { newSets ->
            sets = newSets
            render()
            startPendingSession()
        }

        // Launcher shortcut - only on the first creation, a rotation must not open the dialog again
        if (savedInstanceState == null && intent.getBooleanExtra(EXTRA_NEW_SET, false)) {
            openNewSet()
        }
    }

    // One dialog: name -> the set is created -> its flashcards in a series -> Finish opens the set
    private fun openNewSet() {
        var newSetId: Int? = null
        FlashcardEditDialog.newSet(
            context = this,
            onCreateSet = { name, onCreated ->
                viewModel.addSet(name) { setId ->
                    newSetId = setId
                    onCreated()
                }
            },
            onAdd = { flashcard -> newSetId?.let { setId -> viewModel.addFlashcard(setId, flashcard) } },
            onFinish = { newSetId?.let { setId -> startActivity(FlashcardSetActivity.newIntent(this, setId)) } }
        ).show()
    }

    private fun startPendingSession() {
        if (!pendingSession) return
        pendingSession = false

        FlashcardsSessionDialog.open(this, viewModel.planReview(sets), onRated = viewModel::rate)
    }

    override fun onResume() {
        super.onResume()
        // A new day may have started while away
        render()
    }

    private fun render() {
        setsAdapter.submitList(sets.map { set ->
            FlashcardSetItem(
                set       = set,
                stats     = viewModel.statsOf(set),
                canReview = !viewModel.planReview(sets, set.id).isEmpty
            )
        })

        val hasSets = sets.isNotEmpty()
        rvSets.visibility = if (hasSets) View.VISIBLE else View.GONE
        tvSetsLabel.visibility = if (hasSets) View.VISIBLE else View.GONE
        tvEmpty.visibility = if (hasSets) View.GONE else View.VISIBLE

        renderReviewToday()
    }

    private fun renderReviewToday() {
        // No flashcards at all - nothing to say about the review
        val hasFlashcards = sets.any { set -> set.flashcards.isNotEmpty() }
        reviewToday.visibility = if (hasFlashcards) View.VISIBLE else View.GONE
        if (!hasFlashcards) return

        val plan = viewModel.planReview(sets)
        // The total is the whole session - the new ones are a part of it
        val counts = resources.getQuantityString(R.plurals.flashcards_review_today_counts, plan.queue.size, plan.queue.size)
        val status = when {
            plan.isEmpty      -> getString(R.string.flashcards_review_today_done)
            plan.newCount > 0 -> counts + " " + resources.getQuantityString(R.plurals.flashcards_review_today_new, plan.newCount, plan.newCount)
            else              -> counts
        }
        tvReviewTodayStatus.text =
            if (plan.newPausedBacklog > 0) status + "\n" + getString(R.string.flashcards_new_paused, plan.newPausedBacklog)
            else status

        btnReviewToday.visibility = if (plan.isEmpty) View.INVISIBLE else View.VISIBLE
        // Rippling frame = "something to do" - smooths out once everything is reviewed
        frameReviewToday.isWaving = !plan.isEmpty
    }

    companion object {
        /** true = the review session starts right away (reminder) */
        const val EXTRA_START_SESSION = "extra_start_session"

        /** true = the new-set dialog opens right away (launcher shortcut) */
        const val EXTRA_NEW_SET = "extra_new_set"
    }
}
