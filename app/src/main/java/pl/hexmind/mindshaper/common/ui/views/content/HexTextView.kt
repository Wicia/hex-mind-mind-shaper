package pl.hexmind.mindshaper.common.ui.views.content

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.core.content.withStyledAttributes
import com.google.android.material.button.MaterialButton
import org.sufficientlysecure.htmltextview.HtmlTextView
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.common.ui.dialogs.MultipleActionsDialog

/**
 * Universal rich text display view supporting HTML markups
 */
class HexTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    enum class Mode {
        EDIT_DISPLAY,  // Edit + Display
        DISPLAY_ONLY   // View only
    }

    interface TextCallback {
        fun onTextClicked()
        fun onTextDeleted()
    }

    private val htmlTextView: HtmlTextView
    val btnDelete: MaterialButton

    // State
    private var mode: Mode = Mode.EDIT_DISPLAY
    private var callback: TextCallback? = null

    var originalText: String = ""
        set(value) {
            field = value
            updateFormattedText()
        }

    /**
     * ! Controls whether click events should propagate to parent view.
     * - true (default): Click events pass through to parent (e.g., RecyclerView item)
     * - false: This view handles clicks directly via setOnClickListener
     */
    var propagateClickEventsToParent: Boolean = true
        set(value) {
            field = value
            applyClickBehavior()
        }

    init {
        inflate(context, R.layout.view_rich_text, this)
        orientation = VERTICAL

        // Initialize UI components
        htmlTextView = findViewById(R.id.html_text_view)
        btnDelete = findViewById(R.id.btn_delete)

        // Read XML attributes
        attrs?.let {
            context.withStyledAttributes(it, R.styleable.HexTextView) {
                val modeValue = getInt(R.styleable.HexTextView_richTextMode, 0)
                mode = if (modeValue == 1) Mode.DISPLAY_ONLY else Mode.EDIT_DISPLAY
                setupUIForMode()
            }
        }

        setupListeners()
    }

    private fun setupUIForMode() {
        when (mode) {
            Mode.EDIT_DISPLAY -> {
                btnDelete.visibility = VISIBLE
                propagateClickEventsToParent = false
            }
            Mode.DISPLAY_ONLY -> {
                btnDelete.visibility = GONE
                propagateClickEventsToParent = true
            }
        }
    }

    private fun setupListeners() {
        htmlTextView.setOnClickListener {
            callback?.onTextClicked()
        }

        btnDelete.setOnClickListener {
            showDeleteConfirmation()
        }
    }

    private fun updateFormattedText() {
        val html = HtmlConverter.convertToHtml(originalText)
        htmlTextView.setHtml(html)
        applyClickBehavior()
    }

    /**
     * Applies click behavior configuration after HTML rendering
     * Must be called after setHtml() as it resets these properties
     */
    private fun applyClickBehavior() {
        htmlTextView.movementMethod = null
        htmlTextView.isClickable = !propagateClickEventsToParent
        htmlTextView.isFocusable = !propagateClickEventsToParent
    }

    private fun showDeleteConfirmation() {
        MultipleActionsDialog.Builder(context)
            .setTitle(context.getString(R.string.rich_text_removing_header))
            .setDescription(context.getString(R.string.rich_text_removing_content))
            .setCautionAction(context.getString(R.string.common_deletion_dialog_yes_2)) {
                deleteText()
            }
            .show()
    }

    private fun deleteText() {
        callback?.onTextDeleted()
    }

    // ===========================================
    //      Public API Methods
    // ===========================================

    fun setCallback(callback: TextCallback) {
        this.callback = callback
    }

    fun getText(): String {
        return originalText
    }
}