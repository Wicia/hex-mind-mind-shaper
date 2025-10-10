package pl.hexmind.mindshaper.activities

import android.content.Context
import android.graphics.Color.TRANSPARENT
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.textfield.TextInputEditText
import pl.hexmind.mindshaper.R
import timber.log.Timber
import androidx.core.graphics.drawable.toDrawable

/**
 * Reusable dialog for text editing with validation and custom dimmed background
 */
class CommonTextEditDialog(
    private val context: Context,
    private val title: String? = "",
    private val textInput: String = "",
    // private val validator: ((String) -> ValidationResult)? = null, //TODO: Add validation aspect + uncomment code below
    private val onSave: (String) -> Unit
) {

    private val dialogView = LayoutInflater.from(context).inflate(R.layout.common_dialog_edit, null)
    private val etInput: TextInputEditText = dialogView.findViewById(R.id.et_input)
    private val tvValidationInfo: TextView = dialogView.findViewById(R.id.tv_validation_info)
    private val dialog: AlertDialog

    init {
        setupInitialValues()
        dialog = createDialog()
        setupListeners()
    }

    /**
     * Sets initial text and hint in EditText
     */
    private fun setupInitialValues() {
        etInput.setText(textInput)
        etInput.setSelection(textInput.length)  // Cursor at end
        //tvValidationInfo.text = ""
    }

    /**
     * Creates AlertDialog with custom styling and 80% transparent black background
     */
    private fun createDialog(): AlertDialog {
        return AlertDialog.Builder(context)
            .setTitle(title)
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
     * Sets up real-time validation listener
     */
    private fun setupListeners() {
        etInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                // validateInput()
            }
        }
    }

    /**
     * Validates current input and displays validation message
     */
//    private fun validateInput(): Boolean {
//        val text = etInput.text.toString()
//
//        validator?.let { validatorFn ->
//            val result = validatorFn(text)
//
//            tvValidationInfo.text = result.message
//            tvValidationInfo.setTextColor(
//                if (result.isValid) {
//                    context.getColor(R.color.validation_success)
//                } else {
//                    context.getColor(R.color.validation_error)
//                }
//            )
//
//            return result.isValid
//        }
//
//        // No validator = always valid
//        return true
//    }

    /**
     * Handles save button click with validation
     */
    private fun handleSave() {
        val text = etInput.text.toString()

        //if (validateInput()) {
            onSave(text)
            Timber.d("Dialog saved with text: $text")
//        } else {
//            Timber.w("Validation failed, cannot save")
//            // Don't dismiss dialog - let user fix validation errors
//            show()  // Re-show dialog
//        }
    }

    /**
     * Shows the dialog
     */
    fun show() {
        dialog.show()

        // Request focus and show keyboard
        etInput.requestFocus()
        etInput.postDelayed({
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(etInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }, 100)
    }

    /**
     * Dismisses the dialog
     */
    fun dismiss() {
        dialog.dismiss()
    }

    /**
     * Data class for validation results
     */
    data class ValidationResult(
        val isValid: Boolean,
        val message: String = ""
    )
}