package pl.hexmind.mindshaper.common.ui.dialogs

import android.content.Context
import android.graphics.Color.TRANSPARENT
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toDrawable
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import pl.hexmind.mindshaper.R

/**
 * Reusable dialog for text editing with custom dimmed background
 */
class TextEditDialog(
    private val context: Context,
    private val title: String? = "",
    private val notesStyle: Boolean = false, // true = for making/editing notes (rich text)
    private val textInput: String = "",
    private val onSave: (String) -> Unit
) {

    private val dialogView = LayoutInflater.from(context).inflate(R.layout.common_text_edit_dialog, null)
    private val etInput: TextInputEditText = dialogView.findViewById(R.id.et_input)
    private val tvHeader : TextView = dialogView.findViewById(R.id.tv_header)
    private val toolbarMarkdown: LinearLayout = dialogView.findViewById(R.id.toolbar_markdown)
    private val btnInsertBullet: MaterialButton = dialogView.findViewById(R.id.btn_insert_bullet)
    private val dialog: AlertDialog

    init {
        setupInitialValues()
        dialog = createDialog()
    }

    private fun setupInitialValues() {
        tvHeader.text = title
        etInput.setText(textInput)
        etInput.setSelection(textInput.length)  // Cursor at end

        if (notesStyle) {
            etInput.typeface = ResourcesCompat.getFont(context, R.font.alegreya_regular)
            etInput.textSize = 18f
            toolbarMarkdown.visibility = View.VISIBLE
            btnInsertBullet.setOnClickListener { insertBulletAtCursor() }
            setupBulletContinuation()
        }
        else {
            toolbarMarkdown.visibility = View.GONE
            etInput.typeface = ResourcesCompat.getFont(context, R.font.baloo2)
            etInput.textSize = 16f
        }
    }

    private fun insertBulletAtCursor() {
        val start = etInput.selectionStart.coerceAtLeast(0)
        val text = etInput.text ?: return

        val lineStart = text.lastIndexOf('\n', start - 1) + 1
        val currentLineText = text.substring(lineStart, start)

        if (currentLineText.isBlank()) {
            text.insert(start, "* ")
        } else {
            text.insert(start, "\n* ")
        }
    }

    // Auto-continue bullet lists: Enter on a "* item" line inserts a new "* ".
    // Enter on an empty "* " line ends the list (removes the marker).
    private fun setupBulletContinuation() {
        etInput.addTextChangedListener(object : TextWatcher {
            private var isReacting = false  // guard against re-entrant edits
            private var newlineInserted = false

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            // Detect a freshly typed newline (insertion only, never on delete)
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                newlineInserted = count == 1 && before == 0 && s.getOrNull(start) == '\n'
            }

            override fun afterTextChanged(editable: Editable) {
                if (isReacting || !newlineInserted) return
                newlineInserted = false

                val cursor = etInput.selectionStart
                if (cursor <= 0 || editable.getOrNull(cursor - 1) != '\n') return

                // The line that was just broken (text before the new \n)
                val prevLineEnd = cursor - 1
                val prevLineStart = editable.lastIndexOf("\n", prevLineEnd - 1) + 1
                val prevLine = editable.substring(prevLineStart, prevLineEnd)

                if (!prevLine.trimStart().startsWith(BULLET_PREFIX)) return

                isReacting = true
                val contentAfterBullet = prevLine.trimStart().removePrefix(BULLET_PREFIX).trim()
                if (contentAfterBullet.isEmpty()) {
                    // Empty bullet -> end the list: drop the "* " line and its trailing newline
                    editable.delete(prevLineStart, cursor)
                }
                else {
                    // Continue the list with a fresh marker
                    editable.insert(cursor, BULLET_PREFIX)
                }
                isReacting = false
            }
        })
    }

    private fun createDialog(): AlertDialog {
        val builder = AlertDialog.Builder(context)
            .setView(dialogView)
            .setPositiveButton(context.getString(R.string.common_btn_save)) { _, _ ->
                handleSave()
            }
            .setNegativeButton(context.getString(R.string.common_btn_cancel)) { dialog, _ ->
                dialog.dismiss()
            }

        return builder.create()
            .apply { // Setup window parameters for custom dim
                window?.apply {
                    setBackgroundDrawable(TRANSPARENT.toDrawable())
                    setDimAmount(0.9f)
                    addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                }
            }
    }

    private fun handleSave() {
        val text = etInput.text.toString()
        onSave(text)
    }

    fun show() {
        dialog.show()
        // Request focus and show keyboard
        etInput.requestFocus()
        etInput.postDelayed({
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(etInput, InputMethodManager.SHOW_IMPLICIT)
        }, 100)
    }

    companion object {
        private const val BULLET_PREFIX = "* "
    }
}