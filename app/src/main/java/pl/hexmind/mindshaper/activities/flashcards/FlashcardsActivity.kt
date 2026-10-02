package pl.hexmind.mindshaper.activities.flashcards

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.activities.CoreActivity
import pl.hexmind.mindshaper.common.ui.dialogs.FlashcardSetNameDialog
import pl.hexmind.mindshaper.common.ui.dialogs.FlashcardsSessionDialog

/**
 * Utrwalanie - the flashcard sets. For now: browsing them, creating a new one, reviewing; long press = delete.
 *
 * TODO: Flashcards Repetitions System - levels, intervals, one session over all the due flashcards
 */
@AndroidEntryPoint
class FlashcardsActivity : CoreActivity() {

    private val viewModel: FlashcardsViewModel by viewModels()

    private lateinit var rvSets: RecyclerView
    private lateinit var tvEmpty: TextView

    private val setsAdapter = FlashcardSetsAdapter(
        onSetTap = { set ->
            set.id?.let { setId -> startActivity(FlashcardSetActivity.newIntent(this, setId)) }
        },
        onSetLongPress = { set ->
            set.id?.let { setId -> FlashcardsDeletion.confirmSet(this) { viewModel.deleteSet(setId) } }
        },
        onReviewTap = { set ->
            FlashcardsSessionDialog.open(this, set.flashcards, onSessionChanged = viewModel::updateSession)
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.flashcards_activity)

        // Nav overlay + current-screen highlight are added by CoreActivity (onContentChanged / onResume)
        setupHeader(R.drawable.ic_activity_flashcards, R.string.flashcards_title)

        rvSets = findViewById(R.id.rv_sets)
        tvEmpty = findViewById(R.id.tv_sets_empty)

        rvSets.layoutManager = LinearLayoutManager(this)
        rvSets.adapter = setsAdapter

        findViewById<FloatingActionButton>(R.id.fab_add_set).setOnClickListener {
            // Name first, then straight into the new set - adding its flashcards in a series
            FlashcardSetNameDialog(this) { name ->
                viewModel.addSet(name) { setId ->
                    startActivity(FlashcardSetActivity.newIntent(this, setId, startAdding = true))
                }
            }.show()
        }

        viewModel.sets.observe(this) { sets ->
            setsAdapter.submitList(sets)

            val hasSets = sets.isNotEmpty()
            rvSets.visibility = if (hasSets) View.VISIBLE else View.GONE
            tvEmpty.visibility = if (hasSets) View.GONE else View.VISIBLE
        }
    }
}
