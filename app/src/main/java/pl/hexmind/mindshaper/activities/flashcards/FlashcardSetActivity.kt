package pl.hexmind.mindshaper.activities.flashcards

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.view.updateLayoutParams
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.activities.CoreActivity
import pl.hexmind.mindshaper.common.ui.dialogs.FlashcardEditDialog
import pl.hexmind.mindshaper.common.ui.dialogs.FlashcardsEditDialog
import pl.hexmind.mindshaper.common.ui.dialogs.FlashcardsSessionDialog
import pl.hexmind.mindshaper.services.FlashcardsScheduler
import pl.hexmind.mindshaper.services.dto.FlashcardDTO
import pl.hexmind.mindshaper.services.dto.FlashcardSetDTO
import pl.hexmind.mindshaper.services.dto.FlashcardStatus
import kotlin.random.Random

/**
 * Preview of one set: its flashcards (front + back) in the order they were entered.
 * Delete (X, left) / edit / review (rocket), FAB = add flashcards in a series.
 * Tap on a flashcard = edit just that one, long press = delete it.
 * Each flashcard shows where it is in the Repetitions System (new / level / mastered).
 */
@AndroidEntryPoint
class FlashcardSetActivity : CoreActivity() {

    companion object {
        // Intent extra key — must match SavedStateHandle key in FlashcardSetViewModel
        private const val EXTRA_SET_ID = "setId"

        // Mastered star - bigger than the level number (15sp), in the flashcards' accent
        private const val STAR_TEXT_SIZE_SP = 22f

        // Mastered star's signal - a ring grows out of it and fades; pauses between random (see startStarSignal)
        private const val STAR_SIGNAL_SCALE = 2.6f
        private const val STAR_SIGNAL_ALPHA = 0.9f
        private const val STAR_SIGNAL_MS = 900L
        private const val STAR_SIGNAL_FIRST_MAX_MS = 3000L
        private const val STAR_SIGNAL_PAUSE_MIN_MS = 4000L
        private const val STAR_SIGNAL_PAUSE_MAX_MS = 9000L

        fun newIntent(context: Context, setId: Int): Intent =
            Intent(context, FlashcardSetActivity::class.java)
                .putExtra(EXTRA_SET_ID, setId)
    }

    private val viewModel: FlashcardSetViewModel by viewModels()

    private lateinit var llFlashcards: LinearLayout
    private lateinit var tvEmpty: TextView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var btnEdit: MaterialButton
    private lateinit var btnReview: MaterialButton

    // The set's session follows the limits counted over all the sets
    private var allSets: List<FlashcardSetDTO> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.flashcard_set_activity)

        llFlashcards = findViewById(R.id.ll_set_flashcards)
        tvEmpty = findViewById(R.id.tv_set_empty)
        fabAdd = findViewById(R.id.fab_add_flashcards)
        btnEdit = findViewById(R.id.btn_set_edit)
        btnReview = findViewById(R.id.btn_set_review)

        findViewById<MaterialButton>(R.id.btn_set_delete).setOnClickListener {
            FlashcardsDeletion.confirmSet(this) { viewModel.deleteSet() }
        }

        fabAdd.setOnClickListener {
            viewModel.set.value?.let { set -> openAddSeries(set) }
        }

        btnEdit.setOnClickListener {
            val set = viewModel.set.value ?: return@setOnClickListener
            FlashcardsEditDialog(
                context = this,
                set = set,
                onSave = { name, flashcards -> viewModel.updateSet(name, flashcards) }
            ).show()
        }

        btnReview.setOnClickListener {
            FlashcardsSessionDialog.open(this, viewModel.planReview(allSets), onRated = viewModel::rate)
        }

        viewModel.allSets.observe(this) { sets ->
            allSets = sets
            viewModel.set.value?.let { set -> render(set) }
        }

        viewModel.set.observe(this) { set ->
            // Deleted here (or restored from a backup without it) - nothing left to preview
            if (set == null) {
                finish()
                return@observe
            }
            render(set)
        }
    }

    private fun openAddSeries(set: FlashcardSetDTO) {
        FlashcardEditDialog.addSeries(this, firstNumber = set.flashcards.size + 1, onAdd = viewModel::addFlashcard).show()
    }

    private fun render(set: FlashcardSetDTO) {
        setupHeader(R.drawable.ic_activity_flashcards, R.string.flashcards_title)
        // Set name instead of the screen title - smaller than the shared header style (24sp), names run longer
        findViewById<TextView>(R.id.tv_header_title).apply {
            text = set.name
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
        }

        // Nothing to review right now - the status of each flashcard below tells why
        btnReview.visibility = if (viewModel.planReview(allSets).isEmpty) View.GONE else View.VISIBLE

        llFlashcards.removeAllViews()
        set.flashcards.forEachIndexed { index, flashcard ->
            val row = LayoutInflater.from(this).inflate(R.layout.flashcard_preview_row, llFlashcards, false)
            bindLevel(row, flashcard)
            row.findViewById<TextView>(R.id.tv_preview_front).text = flashcard.front
            row.findViewById<TextView>(R.id.tv_preview_back).text = flashcard.back
            row.setOnClickListener {
                FlashcardEditDialog.edit(this, flashcard, number = index + 1, onSave = viewModel::updateFlashcard).show()
            }
            row.setOnLongClickListener {
                FlashcardsDeletion.confirmFlashcard(this) { viewModel.deleteFlashcard(flashcard) }
                true
            }
            llFlashcards.addView(row)
        }
        tvEmpty.visibility = if (set.flashcards.isEmpty()) View.VISIBLE else View.GONE
    }

    /**
     * Level 0 (new) - 5: number on top + the bar filled from the bottom.
     * Mastered = a star instead of the number
     */
    private fun bindLevel(row: View, flashcard: FlashcardDTO) {
        val filled = flashcard.level.coerceIn(0, FlashcardsScheduler.MAX_LEVEL).toFloat() / FlashcardsScheduler.MAX_LEVEL
        val tvLevel = row.findViewById<TextView>(R.id.tv_preview_level)
        if (flashcard.status == FlashcardStatus.MASTERED) {
            tvLevel.text = getString(R.string.flashcards_level_mastered)
            tvLevel.setTextColor(getColor(R.color.flashcard_accent))
            tvLevel.setTextSize(TypedValue.COMPLEX_UNIT_SP, STAR_TEXT_SIZE_SP)
            startStarSignal(row.findViewById(R.id.v_preview_level_signal))
        }
        else tvLevel.text = flashcard.level.toString()
        row.findViewById<View>(R.id.v_preview_level_done).updateLayoutParams<LinearLayout.LayoutParams> { weight = filled }
        row.findViewById<View>(R.id.v_preview_level_left).updateLayoutParams<LinearLayout.LayoutParams> { weight = 1f - filled }
    }

    /**
     * Mastered star sends out a signal now and then - a ring grows out of it and fades, the star itself stays still.
     * Random pauses - the stars of the set do not signal in sync. Runs only while the row is attached
     * (render() rebuilds the rows); animations off in the system -> no signal
     */
    private fun startStarSignal(signal: View) {
        signal.visibility = View.VISIBLE
        val pulse = object : Runnable {
            override fun run() {
                if (ValueAnimator.areAnimatorsEnabled()) {
                    signal.scaleX = 1f
                    signal.scaleY = 1f
                    signal.alpha = STAR_SIGNAL_ALPHA
                    signal.animate()
                        .scaleX(STAR_SIGNAL_SCALE).scaleY(STAR_SIGNAL_SCALE).alpha(0f)
                        .setDuration(STAR_SIGNAL_MS)
                        .setInterpolator(DecelerateInterpolator())
                        .start()
                }
                signal.postDelayed(this, Random.nextLong(STAR_SIGNAL_PAUSE_MIN_MS, STAR_SIGNAL_PAUSE_MAX_MS))
            }
        }
        signal.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) {
                view.postDelayed(pulse, Random.nextLong(STAR_SIGNAL_FIRST_MAX_MS))
            }

            override fun onViewDetachedFromWindow(view: View) {
                view.removeCallbacks(pulse)
                view.animate().cancel()
            }
        })
    }
}
