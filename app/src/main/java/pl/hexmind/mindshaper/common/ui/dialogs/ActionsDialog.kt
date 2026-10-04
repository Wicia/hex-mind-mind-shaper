package pl.hexmind.mindshaper.common.ui.dialogs

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import pl.hexmind.mindshaper.R

/**
 * Decision dialog - Cancel is always present, layout depends on the number of actions:
 * - 1 action:  [Cancel | Primary]
 * - 2 actions: [Secondary | Primary] + Cancel below
 * Primary (right) may be a caution action (serious consequences) - secondary is always standard.
 */
class ActionsDialog private constructor(
    // core
    private val context: Context,

    // header and content
    private val title: String,
    private val description: String?,

    // actions = buttons
    private val primaryText: String,
    private val primaryCaution: Boolean,
    private val primaryAction: () -> Unit,

    private val secondaryText: String?,
    private val secondaryAction: (() -> Unit)?,

    // Cancel button, tap outside and back
    private val onCancel: (() -> Unit)?
) {

    fun show() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.common_actions_dialog, null)

        val dialog = MaterialAlertDialogBuilder(context)
            .setView(dialogView)
            .setOnCancelListener { onCancel?.invoke() }
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
        // Primary (right)
        dialogView.findViewById<MaterialButton>(R.id.btn_action_primary).apply {
            text = primaryText
            if (primaryCaution) {
                setTextColor(ContextCompat.getColor(context, R.color.action_caution))
                setTypeface(typeface, Typeface.BOLD)
            }
            setOnClickListener {
                primaryAction.invoke()
                dialog.dismiss()
            }
        }

        // Secondary (left) - with it Cancel goes to its own row below
        val hasSecondary = secondaryText != null && secondaryAction != null
        dialogView.findViewById<MaterialButton>(R.id.btn_action_secondary).apply {
            visibility = if (hasSecondary) View.VISIBLE else View.GONE
            text = secondaryText
            setOnClickListener {
                secondaryAction?.invoke()
                dialog.dismiss()
            }
        }

        // Cancel button = same path as tap outside / back (cancel listener)
        val btnCancelInline = dialogView.findViewById<MaterialButton>(R.id.btn_cancel_inline)
        val btnCancelBelow = dialogView.findViewById<MaterialButton>(R.id.btn_cancel_below)
        btnCancelInline.visibility = if (hasSecondary) View.GONE else View.VISIBLE
        btnCancelBelow.visibility = if (hasSecondary) View.VISIBLE else View.GONE
        btnCancelInline.setOnClickListener { dialog.cancel() }
        btnCancelBelow.setOnClickListener { dialog.cancel() }
    }

    class Builder(private val context: Context) {
        private var title: String = ""
        private var description: String? = null
        private var primaryText: String? = null
        private var primaryCaution: Boolean = false
        private var primaryAction: (() -> Unit)? = null
        private var secondaryText: String? = null
        private var secondaryAction: (() -> Unit)? = null
        private var onCancel: (() -> Unit)? = null

        fun setTitle(title: String) = apply {
            this.title = title
        }

        fun setDescription(description : String) = apply {
            this.description = description
        }

        /**
         * Set the primary button (right) - required
         * @param text Button text - says what will happen
         * @param caution true = serious consequences (bold, dark red)
         * @param action Action to perform when clicked
         */
        fun setPrimaryAction(text: String, caution: Boolean = false, action: () -> Unit) = apply {
            this.primaryText = text
            this.primaryCaution = caution
            this.primaryAction = action
        }

        /**
         * Set the secondary button (left, always standard) - optional
         * @param text Button text - says what will happen
         * @param action Action to perform when clicked
         */
        fun setSecondaryAction(text: String, action: () -> Unit) = apply {
            this.secondaryText = text
            this.secondaryAction = action
        }

        /**
         * Set action on cancel - Cancel button, tap outside and back (default: just closes the dialog)
         */
        fun setOnCancel(action: () -> Unit) = apply {
            this.onCancel = action
        }

        /**
         * Build and show the dialog
         */
        fun show() {
            require(title.isNotEmpty()) { "Title text is required" }
            val primaryText = requireNotNull(primaryText) { "Primary action is required" }
            val primaryAction = requireNotNull(primaryAction) { "Primary action is required" }

            ActionsDialog(
                context = context,
                title = title,
                description = description,
                primaryText = primaryText,
                primaryCaution = primaryCaution,
                primaryAction = primaryAction,
                secondaryText = secondaryText,
                secondaryAction = secondaryAction,
                onCancel = onCancel
            ).show()
        }
    }
}
