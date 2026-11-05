package pl.hexmind.mindshaper.activities.flashcards

import android.animation.ValueAnimator
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.common.ui.views.HexLevelHexagon
import pl.hexmind.mindshaper.services.FlashcardsScheduler
import pl.hexmind.mindshaper.services.dto.FlashcardDTO
import pl.hexmind.mindshaper.services.dto.FlashcardStatus
import kotlin.random.Random

/**
 * Flashcards listed as preview rows (flashcard_preview_row) - the set's preview and the loose flashcards tab.
 * Each flashcard shows where it is in the Repetitions System (new / level / mastered).
 */
object FlashcardPreviewRows {

    // Mastered star - bigger than the level number (15sp), in the flashcards' accent
    private const val STAR_TEXT_SIZE_SP = 22f

    // Mastered star's signal - a ring grows out of it and fades; pauses between random (see startStarSignal)
    private const val STAR_SIGNAL_SCALE = 2.6f
    private const val STAR_SIGNAL_ALPHA = 0.9f
    private const val STAR_SIGNAL_MS = 900L
    private const val STAR_SIGNAL_FIRST_MAX_MS = 3000L
    private const val STAR_SIGNAL_PAUSE_MIN_MS = 4000L
    private const val STAR_SIGNAL_PAUSE_MAX_MS = 9000L

    /**
     * Rebuilds the whole list - tap = edit, long press = delete.
     * @param onTap number: 1-based position in the list - the edit dialog's header
     */
    fun render(
        container: LinearLayout,
        flashcards: List<FlashcardDTO>,
        onTap: (flashcard: FlashcardDTO, number: Int) -> Unit,
        onLongPress: (FlashcardDTO) -> Unit
    ) {
        container.removeAllViews()
        flashcards.forEachIndexed { index, flashcard ->
            val row = LayoutInflater.from(container.context).inflate(R.layout.flashcard_preview_row, container, false)
            bindLevel(row, flashcard)
            row.findViewById<TextView>(R.id.tv_preview_front).text = flashcard.front
            row.findViewById<TextView>(R.id.tv_preview_back).text = flashcard.back
            row.setOnClickListener { onTap(flashcard, index + 1) }
            row.setOnLongClickListener {
                onLongPress(flashcard)
                true
            }
            container.addView(row)
        }
    }

    /**
     * Level 0 (new) - 5: "lvl: N" on top + one hexagon side per level.
     * Mastered = a star instead of "lvl: N" and the last side closes the hexagon
     */
    private fun bindLevel(row: View, flashcard: FlashcardDTO) {
        val context = row.context
        val tvLevel = row.findViewById<TextView>(R.id.tv_preview_level)
        val hexagon = row.findViewById<HexLevelHexagon>(R.id.v_preview_level_hexagon)
        if (flashcard.status == FlashcardStatus.MASTERED) {
            tvLevel.text = context.getString(R.string.flashcards_level_mastered)
            tvLevel.setTextColor(context.getColor(R.color.flashcard_accent))
            tvLevel.setTextSize(TypedValue.COMPLEX_UNIT_SP, STAR_TEXT_SIZE_SP)
            hexagon.litSides = HexLevelHexagon.SIDE_COUNT
            startStarSignal(row.findViewById(R.id.v_preview_level_signal))
        }
        else {
            tvLevel.text = context.getString(R.string.flashcards_level, flashcard.level)
            hexagon.litSides = flashcard.level.coerceIn(0, FlashcardsScheduler.MAX_LEVEL)
        }
    }

    /**
     * Mastered star sends out a signal now and then - a ring grows out of it and fades, the star itself stays still.
     * Random pauses - the stars of the list do not signal in sync. Runs only while the row is attached
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
