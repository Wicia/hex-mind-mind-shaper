package pl.hexmind.mindshaper.common.ui.dialogs

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AlertDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import pl.hexmind.mindshaper.R

/**
 * Reusable dialog with two action buttons (caution + standard) and cancel/dismiss button
 */
class MultipleActionsDialog private constructor(
    // core
    private val context: Context,

    // header and content
    private val title: String,
    private val description: String?,

    // actions = buttons
    private val btnStandardText: String?,
    private val btnStandardAction: (() -> Unit)?,

    private val btnCautionText: String?,
    private val btnCautionAction: (() -> Unit)?,

    private val btnDismissText: String,
    private val btnDismissAction: (() -> Unit)?
) {

    fun show() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.z_multiple_actions_dialog, null)

        val dialog = MaterialAlertDialogBuilder(context)
            .setView(dialogView)
            .create()

        dialogView.findViewById<TextView>(R.id.tv_info_header).text = title

        setupButtons(dialogView, dialog)

        // Setup additional (not mandatory) dialog elements
        if(description != null){
            dialogView.findViewById<TextView>(R.id.tv_description).visibility = View.VISIBLE
            dialogView.findViewById<TextView>(R.id.tv_description).text = description
        }
        else{
            dialogView.findViewById<TextView>(R.id.tv_description).visibility = View.GONE
        }

        // Make dialog wider
        dialog.window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.show()
    }

    private fun setupButtons(dialogView : View, dialog : AlertDialog) {
        // Setup caution button
        val btnCaution = dialogView.findViewById<MaterialButton>(R.id.btn_action_caution)
        if (btnCautionText != null && btnCautionAction != null) {
            btnCaution.text = btnCautionText
            btnCaution.setOnClickListener {
                btnCautionAction.invoke()
                dialog.dismiss()
            }
        }
        else {
            btnCaution.visibility = View.GONE
        }

        // Setup standard button
        val btnStandard = dialogView.findViewById<MaterialButton>(R.id.btn_action_standard)
        if (btnStandardText != null && btnStandardAction != null) {
            btnStandard.text = btnStandardText
            btnStandard.setOnClickListener {
                btnStandardAction.invoke()
                dialog.dismiss()
            }
        }
        else {
            btnStandard.visibility = View.GONE
        }

        // Setup cancel button
        val btnDismiss = dialogView.findViewById<MaterialButton>(R.id.btn_dismiss)
        btnDismiss.apply {
            text = btnDismissText
            setOnClickListener {
                btnDismissAction?.invoke()
                dialog.dismiss()
            }
        }

        // Scenario with only 1 button / confirmation
        if(btnDismissAction == null && btnCautionAction == null){
            btnCaution.visibility = View.INVISIBLE
            btnDismiss.visibility = View.GONE
        }
        else{
            btnCaution.visibility = View.VISIBLE
            btnDismiss.visibility = View.VISIBLE
        }
    }

    class Builder(private val context: Context) {
        private var title: String = ""
        private var description: String? = null

        @DrawableRes
        private var iconResId: Int? = null
        private var cautionText: String? = null
        private var cautionAction: (() -> Unit)? = null
        private var standardText: String? = null
        private var standardAction: (() -> Unit)? = null
        private var cancelText: String = "Anuluj"
        private var onCancel: (() -> Unit)? = null

        fun setTitle(title: String) = apply {
            this.title = title
        }

        fun setIconResId(@DrawableRes iconResId: Int?) = apply {
            this.iconResId = iconResId
        }

        fun setDescription(description : String) = apply {
            this.description = description
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
            require(title.isNotEmpty()) { "Question text is required" }
            require(cautionAction != null || standardAction != null) {
                "At least one action (caution or standard) must be set"
            }

            MultipleActionsDialog(
                context = context,
                title = title,
                description = description,
                btnCautionText = cautionText,
                btnCautionAction = cautionAction,
                btnStandardText = standardText,
                btnStandardAction = standardAction,
                btnDismissText = cancelText,
                btnDismissAction = onCancel
            ).show()
        }
    }
}