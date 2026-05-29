package pl.hexmind.mindshaper.common.ui.dialogs

import android.content.Context
import android.graphics.Color.TRANSPARENT
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
}