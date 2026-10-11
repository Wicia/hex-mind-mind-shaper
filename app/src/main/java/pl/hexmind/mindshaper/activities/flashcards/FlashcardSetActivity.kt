package pl.hexmind.mindshaper.activities.flashcards

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.activities.CoreActivity
import pl.hexmind.mindshaper.common.ui.dialogs.FlashcardEditDialog
import pl.hexmind.mindshaper.common.ui.dialogs.FlashcardsEditDialog
import pl.hexmind.mindshaper.common.ui.dialogs.FlashcardsSessionDialog
import pl.hexmind.mindshaper.services.dto.FlashcardDTO
import pl.hexmind.mindshaper.services.dto.FlashcardSetDTO
import pl.hexmind.mindshaper.services.dto.FlashcardsLibraryDTO

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

    // The set's session follows the limits counted over all the flashcards
    private var library = FlashcardsLibraryDTO(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.flashcard_set_activity)

        llFlashcards = findViewById(R.id.ll_set_flashcards)
        tvEmpty = findViewById(R.id.tv_set_empty)
        fabAdd = findViewById(R.id.fab_add_flashcards)
        btnEdit = findViewById(R.id.btn_set_edit)
        btnReview = findViewById(R.id.btn_set_review)

        findViewById<MaterialButton>(R.id.btn_set_delete).setOnClickListener {
            val set = viewModel.set.value ?: return@setOnClickListener
            FlashcardsDeletion.confirmSet(this, set) { keepFlashcards -> viewModel.deleteSet(keepFlashcards) }
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
            FlashcardsSessionDialog.open(this, viewModel.planReview(library), onRated = viewModel::rate)
        }

        viewModel.library.observe(this) { newLibrary ->
            library = newLibrary
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
        btnReview.visibility = if (viewModel.planReview(library).isEmpty) View.GONE else View.VISIBLE

        FlashcardPreviewRows.render(
            container   = llFlashcards,
            flashcards  = set.flashcards,
            onTap       = { flashcard, number -> openEdit(flashcard, number) },
            onLongPress = { flashcard -> FlashcardsDeletion.confirmFlashcard(this) { viewModel.deleteFlashcard(flashcard) } }
        )
        tvEmpty.visibility = if (set.flashcards.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun openEdit(flashcard: FlashcardDTO, number: Int) {
        FlashcardEditDialog.edit(this, flashcard, number = number, onSave = viewModel::updateFlashcard).show()
    }
}
