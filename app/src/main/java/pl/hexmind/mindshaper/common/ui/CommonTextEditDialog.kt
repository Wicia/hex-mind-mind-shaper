package pl.hexmind.mindshaper.common.ui

import android.content.Context
import android.graphics.Color.TRANSPARENT
import android.view.LayoutInflater
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.textfield.TextInputEditText
import pl.hexmind.mindshaper.R
import timber.log.Timber
import androidx.core.graphics.drawable.toDrawable

/**
 * Reusable dialog for text editing with custom dimmed background
 */
class CommonTextEditDialog(
    private val context: Context,
    private val title: String? = "",
    private val textInput: String = "",
    private val onSave: (String) -> Unit
) {

    private val dialogView = LayoutInflater.from(context).inflate(R.layout.common_dialog_edit, null)
    private val etInput: TextInputEditText = dialogView.findViewById(R.id.et_input)

    private val tvHeader : TextView = dialogView.findViewById(R.id.tv_header)
    private val dialog: AlertDialog

    init {
        setupInitialValues()
        dialog = createDialog()
    }

    /**
     * Sets initial text and hint in EditText
     */
    private fun setupInitialValues() {
        tvHeader.text = title
        etInput.setText(textInput)
        etInput.setSelection(textInput.length)  // Cursor at end
    }

    /**
     * Creates AlertDialog with custom styling and 80% transparent black background
     */
    private fun createDialog(): AlertDialog {
        return AlertDialog.Builder(context)
            .setView(dialogView)
            .setPositiveButton(context.getString(R.string.common_btn_save)) { _, _ ->
                handleSave()
            }
            .setNegativeButton(context.getString(R.string.common_btn_cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .apply {
                // Setup window parameters for custom dim
                window?.apply {
                    setBackgroundDrawable(TRANSPARENT.toDrawable())
                    setDimAmount(0.9f)  // 90% black overlay, dim amount (0.0 = no dim, 1.0 = fully black)
                    addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                }
            }
    }

    /**
     * Handles save button click
     */
    private fun handleSave() {
        val text = etInput.text.toString()
        onSave(text)
    }

    /**
     * Shows the dialog
     */
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