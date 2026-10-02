package pl.hexmind.mindshaper.activities.flashcards

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
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
import pl.hexmind.mindshaper.services.FlashcardsSession
import pl.hexmind.mindshaper.services.dto.FlashcardSetDTO

/**
 * Preview of one set: its flashcards (front + back) in the order they were entered.
 * Delete (X, left) / edit / review (rocket), FAB = add flashcards in a series.
 * Tap on a flashcard = edit just that one, long press = delete it.
 * Editing is locked while a review session is in progress.
 */
@AndroidEntryPoint
class FlashcardSetActivity : CoreActivity() {

    companion object {
        // Intent extra key — must match SavedStateHandle key in FlashcardSetViewModel
        private const val EXTRA_SET_ID = "setId"
        private const val EXTRA_START_ADDING = "startAdding"

        /** @param startAdding opens adding flashcards in a series right away - a freshly created set */
        fun newIntent(context: Context, setId: Int, startAdding: Boolean = false): Intent =
            Intent(context, FlashcardSetActivity::class.java)
                .putExtra(EXTRA_SET_ID, setId)
                .putExtra(EXTRA_START_ADDING, startAdding)
    }

    private val viewModel: FlashcardSetViewModel by viewModels()

    private lateinit var llFlashcards: LinearLayout
    private lateinit var tvEmpty: TextView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var btnEdit: MaterialButton
    private lateinit var btnReview: MaterialButton

    // Once per created set - not again after a rotation
    private var pendingStartAdding = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.flashcard_set_activity)

        llFlashcards = findViewById(R.id.ll_set_flashcards)
        tvEmpty = findViewById(R.id.tv_set_empty)
        fabAdd = findViewById(R.id.fab_add_flashcards)
        btnEdit = findViewById(R.id.btn_set_edit)
        btnReview = findViewById(R.id.btn_set_review)

        pendingStartAdding = savedInstanceState == null && intent.getBooleanExtra(EXTRA_START_ADDING, false)

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
            val set = viewModel.set.value ?: return@setOnClickListener
            FlashcardsSessionDialog.open(this, set.flashcards, onSessionChanged = viewModel::updateSession)
        }

        viewModel.set.observe(this) { set ->
            // Deleted here (or restored from a backup without it) - nothing left to preview
            if (set == null) {
                finish()
                return@observe
            }
            render(set)

            if (pendingStartAdding) {
                pendingStartAdding = false
                openAddSeries(set)
            }
        }
    }

    private fun openAddSeries(set: FlashcardSetDTO) {
        FlashcardEditDialog.addSeries(this, firstNumber = set.flashcards.size + 1, onAdd = viewModel::addFlashcard).show()
    }

    private fun render(set: FlashcardSetDTO) {
        setupHeader(R.drawable.ic_activity_flashcards, R.string.flashcards_title)
        findViewById<TextView>(R.id.tv_header_title).text = set.name

        // Changing the set mid-session would mix the queue up - edit once the session is done
        val editable = !FlashcardsSession.isInProgress(set.flashcards)
        if (editable) fabAdd.show() else fabAdd.hide()
        btnEdit.visibility = if (editable) View.VISIBLE else View.INVISIBLE
        btnReview.visibility = if (set.flashcards.isNotEmpty()) View.VISIBLE else View.INVISIBLE

        llFlashcards.removeAllViews()
        set.flashcards.forEachIndexed { index, flashcard ->
            val row = LayoutInflater.from(this).inflate(R.layout.flashcard_preview_row, llFlashcards, false)
            row.findViewById<TextView>(R.id.tv_preview_number).text = getString(R.string.flashcards_row_label, index + 1)
            row.findViewById<TextView>(R.id.tv_preview_front).text = flashcard.front
            row.findViewById<TextView>(R.id.tv_preview_back).text = flashcard.back
            if (editable) {
                row.setOnClickListener {
                    FlashcardEditDialog.edit(this, flashcard, number = index + 1, onSave = viewModel::updateFlashcard).show()
                }
                row.setOnLongClickListener {
                    FlashcardsDeletion.confirmFlashcard(this) { viewModel.deleteFlashcard(flashcard) }
                    true
                }
            }
            llFlashcards.addView(row)
        }
        tvEmpty.visibility = if (set.flashcards.isEmpty()) View.VISIBLE else View.GONE
    }
}
