package pl.hexmind.mindshaper.activities.flashcards

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.activities.CoreActivity
import pl.hexmind.mindshaper.common.formatting.withoutOrphans
import pl.hexmind.mindshaper.common.ui.TextFilePicker
import pl.hexmind.mindshaper.common.ui.dialogs.FlashcardEditDialog
import pl.hexmind.mindshaper.common.ui.dialogs.FlashcardsEditDialog
import pl.hexmind.mindshaper.common.ui.dialogs.FlashcardsImportDialog
import pl.hexmind.mindshaper.common.ui.views.SpeedDial
import pl.hexmind.mindshaper.services.dto.FlashcardDTO
import pl.hexmind.mindshaper.services.dto.FlashcardSetDTO
import pl.hexmind.mindshaper.services.dto.FlashcardsLibraryDTO

/**
 * Preview of one set: its flashcards (front + back) in the order they were entered.
 * Edit / review (rocket), FAB = speed dial: add flashcards in a series / import them from a text.
 * Tap on a flashcard = edit just that one (+ move it), long press = delete it.
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

    private val textFilePicker = TextFilePicker(this)

    private lateinit var llFlashcards: LinearLayout
    private lateinit var tvEmpty: TextView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var btnEdit: MaterialButton
    private lateinit var btnReview: MaterialButton

    // The set's session follows the limits counted over all the flashcards; the other sets are the move targets
    private var library = FlashcardsLibraryDTO(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.flashcard_set_activity)

        llFlashcards = findViewById(R.id.ll_set_flashcards)
        tvEmpty = findViewById(R.id.tv_set_empty)
        fabAdd = findViewById(R.id.fab_add_flashcards)
        btnEdit = findViewById(R.id.btn_set_edit)
        btnReview = findViewById(R.id.btn_set_review)

        setupSpeedDial()

        btnEdit.setOnClickListener {
            val set = viewModel.set.value ?: return@setOnClickListener
            FlashcardsEditDialog(
                context = this,
                set = set,
                onSave = { name, flashcards -> viewModel.updateSet(name, flashcards) }
            ).show()
        }

        btnReview.setOnClickListener {
            FlashcardsSessionActivity.open(this, viewModel.planReview(library), viewModel.setId)
        }

        viewModel.library.observe(this) { newLibrary ->
            library = newLibrary
            viewModel.set.value?.let { set -> render(set) }
        }

        viewModel.set.observe(this) { set ->
            // Deleted from the sets list or restored from a backup without it - nothing left to preview
            if (set == null) {
                finish()
                return@observe
            }
            render(set)
        }
    }

    private fun setupSpeedDial() {
        val speedDial = SpeedDial(this, fabAdd)
        speedDial.addOption(R.string.flashcards_speed_dial_import, R.drawable.ic_action_import) {
            FlashcardsImportDialog(this, textFilePicker, onImport = viewModel::importFlashcards).show()
        }
        speedDial.addOption(R.string.flashcards_speed_dial_add, R.drawable.ic_action_add_flashcards) {
            viewModel.set.value?.let { set -> openAddSeries(set) }
        }
        fabAdd.setOnClickListener { speedDial.toggle() }
    }

    private fun openAddSeries(set: FlashcardSetDTO) {
        FlashcardEditDialog.addSeries(this, onAdd = viewModel::addFlashcard).show()
    }

    private fun render(set: FlashcardSetDTO) {
        findViewById<TextView>(R.id.tv_set_name).text = set.name.withoutOrphans()

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

    // A flashcard of a set can always move - at least to the loose ones
    private fun openEdit(flashcard: FlashcardDTO, number: Int) {
        FlashcardEditDialog.edit(
            context = this,
            flashcard = flashcard,
            number = number,
            onSave = viewModel::updateFlashcard,
            onMove = {
                FlashcardMove.start(this, viewModel.setId, library.sets) { toSetId ->
                    viewModel.moveFlashcard(flashcard, toSetId)
                }
            }
        ).show()
    }
}
