package pl.hexmind.mindshaper.common.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.databinding.CommonFlashcardEditRowBinding
import pl.hexmind.mindshaper.services.dto.FlashcardDTO
import pl.hexmind.mindshaper.services.dto.FlashcardStatus

/**
 * Front + back fields of one flashcard - shared by the single flashcard dialog and the whole set dialog.
 * Header (remove X + number) shows only once a remove listener is set - a list row needs it, a lone flashcard does not.
 */
class FlashcardEditRow @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding = CommonFlashcardEditRowBinding.inflate(LayoutInflater.from(context), this)

    // Flashcard being edited, null = a new one - untouched content keeps its id and answer counters
    private var original: FlashcardDTO? = null

    init {
        orientation = VERTICAL
    }

    fun bind(flashcard: FlashcardDTO?) {
        original = flashcard
        binding.hifFlashcardFront.setText(flashcard?.front)
        binding.hifFlashcardBack.setText(flashcard?.back)
    }

    /** Ready for the next new flashcard */
    fun clear() = bind(null)

    fun setNumber(number: Int) {
        binding.tvFlashcardNumber.text = context.getString(R.string.flashcards_row_label, number)
    }

    fun setOnRemoveClickListener(listener: () -> Unit) {
        binding.llFlashcardHeader.visibility = VISIBLE
        binding.btnFlashcardRemove.setOnClickListener { listener() }
    }

    fun focusFront() = binding.hifFlashcardFront.requestFocusOnField()

    fun isEmpty(): Boolean =
        binding.hifFlashcardFront.getText().isEmpty() && binding.hifFlashcardBack.getText().isEmpty()

    fun isComplete(): Boolean =
        binding.hifFlashcardFront.getText().isNotEmpty() && binding.hifFlashcardBack.getText().isNotEmpty()

    fun isChanged(): Boolean = getFlashcard() != original

    /** Content of an already reviewed flashcard changed - its progress stays, but the learning may be off */
    fun isLearnedContentChanged(): Boolean {
        val flashcard = original ?: return false
        return flashcard.status != FlashcardStatus.NEW && isChanged()
    }

    fun getFlashcard(): FlashcardDTO {
        val front = binding.hifFlashcardFront.getText()
        val back = binding.hifFlashcardBack.getText()
        val flashcard = original
        return when {
            flashcard == null                                     -> FlashcardDTO(front = front, back = back)
            // ! Editing keeps the repetition progress (Repetitions System) - the user gets a heads-up instead
            else -> flashcard.copy(front = front, back = back)
        }
    }
}
