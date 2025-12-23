package pl.hexmind.mindshaper.common.ui

import android.content.Context
import android.view.LayoutInflater
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import pl.hexmind.mindshaper.R

/**
 * Reusable dialog with two action buttons (caution + standard) and cancel button
 *
 * Example usage:
 * ```
 * CommonActionsDialog.Builder(context)
 *     .setQuestion("Co chcesz zrobić z nagraniem?")
 *     .setCautionAction("Wywalić") { deleteRecording() }
 *     .setStandardAction("Nadpisać") { overwriteRecording() }
 *     .show()
 * ```
 */
class CommonActionsDialog private constructor(
    private val context: Context,
    private val question: String,
    private val cautionText: String?,
    private val cautionAction: (() -> Unit)?,
    private val standardText: String?,
    private val standardAction: (() -> Unit)?,
    private val cancelText: String,
    private val onCancel: (() -> Unit)?
) {

    fun show() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.common_dialog_actions, null)

        val dialog = MaterialAlertDialogBuilder(context)
            .setView(dialogView)
            .create()

        // Set question text
        dialogView.findViewById<TextView>(R.id.tv_question).text = question

        // Setup caution button
        val btnCaution = dialogView.findViewById<MaterialButton>(R.id.btn_action_caution)
        if (cautionText != null && cautionAction != null) {
            btnCaution.text = cautionText
            btnCaution.setOnClickListener {
                cautionAction.invoke()
                dialog.dismiss()
            }
        }
        else {
            btnCaution.visibility = android.view.View.GONE
        }

        // Setup standard button
        val btnStandard = dialogView.findViewById<MaterialButton>(R.id.btn_action_standard)
        if (standardText != null && standardAction != null) {
            btnStandard.text = standardText
            btnStandard.setOnClickListener {
                standardAction.invoke()
                dialog.dismiss()
            }
        }
        else {
            btnStandard.visibility = android.view.View.GONE
        }

        // Setup cancel button
        dialogView.findViewById<MaterialButton>(R.id.btn_cancel).apply {
            text = cancelText
            setOnClickListener {
                onCancel?.invoke()
                dialog.dismiss()
            }
        }

        dialog.show()

        // Make dialog wider
        dialog.window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.9).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    class Builder(private val context: Context) {
        private var question: String = ""
        private var cautionText: String? = null
        private var cautionAction: (() -> Unit)? = null
        private var standardText: String? = null
        private var standardAction: (() -> Unit)? = null
        private var cancelText: String = "Anuluj"
        private var onCancel: (() -> Unit)? = null

        /**
         * Set the main question/title text
         */
        fun setQuestion(text: String) = apply {
            this.question = text
        }

        /**
         * Set the caution button (left, bold)
         * @param text Button text
         * @param action Action to perform when clicked
         */
        fun setCautionAction(text: String, action: () -> Unit) = apply {
            this.cautionText = text
            this.cautionAction = action
        }

        /**
         * Set the standard button (right)
         * @param text Button text
         * @param action Action to perform when clicked
         */
        fun setStandardAction(text: String, action: () -> Unit) = apply {
            this.standardText = text
            this.standardAction = action
        }

        /**
         * Set custom cancel button text (default: "Anuluj")
         */
        fun setCancelText(text: String) = apply {
            this.cancelText = text
        }

        /**
         * Set action to perform when cancel is clicked (optional)
         */
        fun setOnCancel(action: () -> Unit) = apply {
            this.onCancel = action
        }

        /**
         * Build and show the dialog
         */
        fun show() {
            require(question.isNotEmpty()) { "Question text is required" }
            require(cautionAction != null || standardAction != null) {
                "At least one action (caution or standard) must be set"
            }

            CommonActionsDialog(
                context = context,
                question = question,
                cautionText = cautionText,
                cautionAction = cautionAction,
                standardText = standardText,
                standardAction = standardAction,
                cancelText = cancelText,
                onCancel = onCancel
            ).show()
        }

        /**
         * Build the dialog without showing it (useful for testing)
         */
        fun build(): CommonActionsDialog {
            require(question.isNotEmpty()) { "Question text is required" }

            return CommonActionsDialog(
                context = context,
                question = question,
                cautionText = cautionText,
                cautionAction = cautionAction,
                standardText = standardText,
                standardAction = standardAction,
                cancelText = cancelText,
                onCancel = onCancel
            )
        }
    }
}