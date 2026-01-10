package pl.hexmind.mindshaper.common.ui

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import pl.hexmind.mindshaper.R

/**
 * Data class representing a single tooltip item in the dialog's carousel
 */
data class TooltipItem(
    val description: String,
    val icon: Drawable? = null
)

/**
 * Reusable dialog for showing preciousssss... knowledge and tooltips for user
 * Now with carousel support!
 */
class DialogTooltips private constructor(
    private val context: Context,
    private val title: String,
    private val tooltips: List<TooltipItem>,
    private val onDismiss: (() -> Unit)?
) {

    private var currentIndex = 0
    private lateinit var dialog: AlertDialog
    private lateinit var dialogView: View

    fun show() {
        dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_tooltips, null)

        dialog = MaterialAlertDialogBuilder(context)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        setupTitle()
        setupDots()
        setupButtons()
        updateContent()

        // Make dialog wider
        dialog.window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.9).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.show()
    }

    private fun setupTitle() {
        dialogView.findViewById<TextView>(R.id.tv_info_header).text = title
    }

    private fun setupDots() {
        val dotsContainer = dialogView.findViewById<LinearLayout>(R.id.dots_indicator)
        dotsContainer.removeAllViews()

        // Hide dots if only one tooltip
        if (tooltips.size <= 1) {
            dotsContainer.visibility = View.INVISIBLE
            return
        }

        val dotSize = (8 * context.resources.displayMetrics.density).toInt()
        val dotMargin = (6 * context.resources.displayMetrics.density).toInt()

        for (i in tooltips.indices) {
            val dot = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(dotSize, dotSize).apply {
                    marginEnd = if (i < tooltips.size - 1) dotMargin else 0
                }
                setImageResource(R.drawable.shape_carousel_indicator)
                imageTintList = android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(
                        context,
                        if (i == currentIndex) R.color._orange_lvl_2
                        else R.color._gray_lvl_2
                    )
                )
            }
            dotsContainer.addView(dot)
        }
    }

    private fun updateDots() {
        val dotsContainer = dialogView.findViewById<LinearLayout>(R.id.dots_indicator)

        for (i in 0 until dotsContainer.childCount) {
            val dot = dotsContainer.getChildAt(i) as ImageView
            dot.imageTintList = android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(
                    context,
                    if (i == currentIndex) R.color._orange_lvl_2
                    else R.color._gray_lvl_2
                )
            )
        }
    }

    private fun updateContent() {
        val currentTooltip = tooltips[currentIndex]

        // Update description
        val tvDescription = dialogView.findViewById<TextView>(R.id.tv_description)
        tvDescription.text = currentTooltip.description

        // Update icon
        val ivIcon = dialogView.findViewById<ImageView>(R.id.iv_icon)
        if (currentTooltip.icon != null) {
            ivIcon.visibility = View.VISIBLE
            ivIcon.setImageDrawable(currentTooltip.icon)
        }
        else {
            ivIcon.visibility = View.GONE
        }
    }

    private fun setupButtons() {
        // Next button
        val btnNext = dialogView.findViewById<MaterialButton>(R.id.btn_action_next_tooltip)
        btnNext.setOnClickListener {
            goToNext()
        }

        // Dismiss button
        val btnDismiss = dialogView.findViewById<MaterialButton>(R.id.btn_dismiss)
        btnDismiss.setOnClickListener {
            onDismiss?.invoke()
            dialog.dismiss()
        }
    }

    private fun goToNext() {
        // Cycle through tooltips
        currentIndex = (currentIndex + 1) % tooltips.size
        updateContent()
        updateDots()
    }

    class Builder(private val context: Context) {
        private var title: String = ""
        private var tooltips: MutableList<TooltipItem> = mutableListOf()
        private var onDismiss: (() -> Unit)? = null

        fun setTitle(title: String) = apply {
            this.title = title
        }

        fun addTooltip(description: String, icon: Drawable? = null) = apply {
            tooltips.add(TooltipItem(description, icon))
        }

        fun setOnDismissAction(action: () -> Unit) = apply {
            this.onDismiss = action
        }

        /**
         * Build and show the dialog
         */
        fun show() {
            require(title.isNotEmpty()) { "Title is required" }
            require(tooltips.isNotEmpty()) { "At least one tooltip is required" }

            DialogTooltips(
                context = context,
                title = title,
                tooltips = tooltips,
                onDismiss = onDismiss
            ).show()
        }
    }
}