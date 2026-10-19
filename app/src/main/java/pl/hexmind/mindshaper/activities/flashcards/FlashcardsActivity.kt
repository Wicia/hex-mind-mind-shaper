package pl.hexmind.mindshaper.activities.flashcards

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.activities.CoreActivity
import pl.hexmind.mindshaper.common.ui.TextFilePicker
import pl.hexmind.mindshaper.common.ui.dialogs.FlashcardEditDialog
import pl.hexmind.mindshaper.common.ui.dialogs.FlashcardsImportDialog
import pl.hexmind.mindshaper.common.ui.dialogs.FlashcardsSessionDialog
import pl.hexmind.mindshaper.common.ui.views.HexTabSwitch
import pl.hexmind.mindshaper.common.ui.views.HexWavyFrameView
import pl.hexmind.mindshaper.common.ui.views.SpeedDial
import pl.hexmind.mindshaper.services.dto.FlashcardDTO
import pl.hexmind.mindshaper.services.dto.FlashcardsLibraryDTO

/**
 * Flashcards screen - today's session widget (one session over all the flashcards) + tabs below:
 * - Luźne fiszki (default): flashcards without a set - tap = edit (+ move to a set), long press = delete
 * - Zestawy: set tap = preview, rocket = review of just this set, long press = delete
 * FAB by the shown tab: speed dial (add loose flashcards in a series / import them from a text) / a new set.
 * Opened from a reminder: the session starts as soon as the flashcards are loaded.
 * Opened from the launcher shortcut: a series of new loose flashcards right away.
 */
@AndroidEntryPoint
class FlashcardsActivity : CoreActivity() {

    private val viewModel: FlashcardsViewModel by viewModels()

    private val textFilePicker = TextFilePicker(this)

    private lateinit var tabsFlashcards: HexTabSwitch
    private lateinit var llLoose: LinearLayout
    private lateinit var tvLooseEmpty: TextView
    private lateinit var rvSets: RecyclerView
    private lateinit var tvSetsEmpty: TextView
    private lateinit var reviewToday: View
    private lateinit var tvReviewTodayStatus: TextView
    private lateinit var btnReviewToday: MaterialButton
    private lateinit var frameReviewToday: HexWavyFrameView
    private lateinit var speedDial: SpeedDial

    // Last flashcards from the database - due reviews also change with the time, so onResume renders them again
    private var library = FlashcardsLibraryDTO(emptyList())

    // Reminder's session waiting for the first flashcards from the database
    private var pendingSession = false

    // Shortcut's series waiting for the loose flashcards - their count numbers the new ones
    private var pendingNewLoose = false

    private val setsAdapter = FlashcardSetsAdapter(
        onSetTap = { set ->
            set.id?.let { setId -> startActivity(FlashcardSetActivity.newIntent(this, setId)) }
        },
        onSetLongPress = { set ->
            set.id?.let { setId ->
                FlashcardsDeletion.confirmSet(this, set) { keepFlashcards -> viewModel.deleteSet(setId, keepFlashcards) }
            }
        },
        onReviewTap = { set ->
            FlashcardsSessionDialog.open(this, viewModel.planReview(library, set.id), onRated = viewModel::rate)
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.flashcards_activity)

        // Only on the first creation - a rotation must not start the session / the series again
        if (savedInstanceState == null) {
            pendingSession = intent.getBooleanExtra(EXTRA_START_SESSION, false)
            pendingNewLoose = intent.getBooleanExtra(EXTRA_NEW_LOOSE_FLASHCARDS, false)
        }

        // Nav overlay + current-screen highlight are added by CoreActivity (onContentChanged / onResume)
        setupHeader(R.drawable.ic_activity_flashcards, R.string.flashcards_title)

        tabsFlashcards = findViewById(R.id.tabs_flashcards)
        llLoose = findViewById(R.id.ll_loose_flashcards)
        tvLooseEmpty = findViewById(R.id.tv_loose_empty)
        rvSets = findViewById(R.id.rv_sets)
        tvSetsEmpty = findViewById(R.id.tv_sets_empty)

        reviewToday = findViewById(R.id.inc_review_today)
        tvReviewTodayStatus = reviewToday.findViewById(R.id.tv_review_today_status)
        btnReviewToday = reviewToday.findViewById(R.id.btn_review_today)
        frameReviewToday = reviewToday.findViewById(R.id.frame_review_today)

        rvSets.layoutManager = LinearLayoutManager(this)
        rvSets.adapter = setsAdapter

        tabsFlashcards.setTabs(
            listOf(
                getString(R.string.flashcards_tab_loose),
                getString(R.string.flashcards_tab_sets)
            ),
            initialIndex = savedInstanceState?.getInt(STATE_TAB) ?: TAB_LOOSE
        )
        tabsFlashcards.onTabSelected = {
            speedDial.close()
            renderTabs()
        }

        btnReviewToday.setOnClickListener {
            FlashcardsSessionDialog.open(this, viewModel.planReview(library), onRated = viewModel::rate)
        }

        setupFab()

        viewModel.library.observe(this) { newLibrary ->
            library = newLibrary
            render()
            startPendingSession()
            startPendingNewLoose()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_TAB, tabsFlashcards.selectedIndex)
    }

    private fun setupFab() {
        val fab = findViewById<FloatingActionButton>(R.id.fab_add)
        speedDial = SpeedDial(this, fab)
        speedDial.addOption(R.string.flashcards_speed_dial_import, R.drawable.ic_action_import) {
            FlashcardsImportDialog(this, textFilePicker, onImport = viewModel::importLooseFlashcards).show()
        }
        speedDial.addOption(R.string.flashcards_speed_dial_add, R.drawable.ic_action_add_flashcards) {
            openAddLoose()
        }

        fab.setOnClickListener {
            if (tabsFlashcards.selectedIndex == TAB_LOOSE) speedDial.toggle() else openNewSet()
        }
    }

    private fun openAddLoose() {
        FlashcardEditDialog.addSeries(
            this,
            firstNumber = library.loose.size + 1,
            onAdd = { flashcard -> viewModel.addFlashcard(null, flashcard) }
        ).show()
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

    // A loose flashcard moves only to a set - no set, no Move button
    private fun openLooseEdit(flashcard: FlashcardDTO, number: Int) {
        val onMove: (() -> Unit)? =
            if (FlashcardMove.canMove(null, library.sets)) {
                {
                    FlashcardMove.start(this, null, library.sets) { toSetId ->
                        viewModel.moveLooseFlashcard(flashcard, toSetId)
                    }
                }
            }
            else null
        FlashcardEditDialog.edit(
            context = this,
            flashcard = flashcard,
            number = number,
            onSave = viewModel::updateLooseFlashcard,
            onMove = onMove
        ).show()
    }

    private fun startPendingSession() {
        if (!pendingSession) return
        pendingSession = false

        FlashcardsSessionDialog.open(this, viewModel.planReview(library), onRated = viewModel::rate)
    }

    private fun startPendingNewLoose() {
        if (!pendingNewLoose) return
        pendingNewLoose = false

        tabsFlashcards.setSelectedIndex(TAB_LOOSE)
        renderTabs()
        openAddLoose()
    }

    override fun onResume() {
        super.onResume()
        // A new day may have started while away
        render()
    }

    private fun render() {
        FlashcardPreviewRows.render(
            container   = llLoose,
            flashcards  = library.loose,
            onTap       = { flashcard, number -> openLooseEdit(flashcard, number) },
            onLongPress = { flashcard -> FlashcardsDeletion.confirmFlashcard(this) { viewModel.deleteLooseFlashcard(flashcard) } }
        )

        setsAdapter.submitList(library.sets.map { set ->
            FlashcardSetItem(
                set       = set,
                stats     = viewModel.statsOf(set),
                canReview = !viewModel.planReview(library, set.id).isEmpty
            )
        })

        renderTabs()
        renderReviewToday()
    }

    /** Only the shown tab's list (or its empty state) is visible */
    private fun renderTabs() {
        val isLooseTab = tabsFlashcards.selectedIndex == TAB_LOOSE
        val hasLoose = library.loose.isNotEmpty()
        val hasSets = library.sets.isNotEmpty()

        llLoose.visibility = if (isLooseTab && hasLoose) View.VISIBLE else View.GONE
        tvLooseEmpty.visibility = if (isLooseTab && !hasLoose) View.VISIBLE else View.GONE
        rvSets.visibility = if (!isLooseTab && hasSets) View.VISIBLE else View.GONE
        tvSetsEmpty.visibility = if (!isLooseTab && !hasSets) View.VISIBLE else View.GONE
    }

    private fun renderReviewToday() {
        // No flashcards at all - nothing to say about the review
        val hasFlashcards = !library.isEmpty
        reviewToday.visibility = if (hasFlashcards) View.VISIBLE else View.GONE
        if (!hasFlashcards) return

        val plan = viewModel.planReview(library)
        // The total is the whole session - the new ones are a part of it
        val counts = resources.getQuantityString(R.plurals.flashcards_review_today_counts, plan.queue.size, plan.queue.size)
        val status = when {
            plan.isEmpty      -> getString(R.string.flashcards_review_today_done)
            plan.newCount > 0 -> counts + " " + resources.getQuantityString(R.plurals.flashcards_review_today_new, plan.newCount, plan.newCount)
            else              -> counts
        }
        tvReviewTodayStatus.text = status

        btnReviewToday.visibility = if (plan.isEmpty) View.INVISIBLE else View.VISIBLE
        // Rippling frame = "something to do" - smooths out once everything is reviewed
        frameReviewToday.isWaving = !plan.isEmpty
    }

    companion object {
        /** true = the review session starts right away (reminder) */
        const val EXTRA_START_SESSION = "extra_start_session"

        /** true = a series of new loose flashcards opens right away (launcher shortcut) */
        const val EXTRA_NEW_LOOSE_FLASHCARDS = "extra_new_loose_flashcards"

        private const val STATE_TAB = "state_tab"

        // HexTabSwitch order: 0 = loose flashcards, 1 = sets
        private const val TAB_LOOSE = 0
    }
}
