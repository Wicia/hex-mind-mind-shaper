package pl.hexmind.mindshaper.common.ui.dialogs

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import pl.hexmind.mindshaper.R

data class GuideScreen(
    val title : String? = null,
    val description: String,
    val icon: Drawable? = null
)

/**
 * Reusable dialog for showing preciousssss... knowledge, info and tooltips for user
 */
class GuideDialog private constructor(
    private val context: Context,
    private val guideScreens: List<GuideScreen>,
    private val onDismiss: (() -> Unit)?
) {

    private var currentIndex = 0
    private lateinit var dialog: AlertDialog
    private lateinit var dialogView: View

    fun show() {
        dialogView = LayoutInflater.from(context).inflate(R.layout.common_guide_dialog, null)

        dialog = MaterialAlertDialogBuilder(context)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        updateTitle()
        setupDots()
        setupButtons()
        updateContent()

        // Make dialog wider
        dialog.window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.show()
    }

    private fun updateTitle() {
        val currentGuideScreen = guideScreens[currentIndex]
        dialogView.findViewById<TextView>(R.id.tv_info_header).text = currentGuideScreen.title
    }

    private fun setupDots() {
        val dotsContainer = dialogView.findViewById<LinearLayout>(R.id.dots_indicator)
        dotsContainer.removeAllViews()

        // Hide dots if only one tooltip
        if (guideScreens.size <= 1) {
            dotsContainer.visibility = View.INVISIBLE
            return
        }

        val dotSize = (8 * context.resources.displayMetrics.density).toInt()
        val dotMargin = (6 * context.resources.displayMetrics.density).toInt()

        for (i in guideScreens.indices) {
            val dot = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(dotSize, dotSize).apply {
                    marginEnd = if (i < guideScreens.size - 1) dotMargin else 0
                }
                setImageResource(R.drawable.shape_circle)
                imageTintList = ColorStateList.valueOf(
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
            dot.imageTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    context,
                    if (i == currentIndex) R.color._orange_lvl_2
                    else R.color._gray_lvl_2
                )
            )
        }
    }

    private fun updateContent() {
        val currentGuideScreen = guideScreens[currentIndex]

        // Update description
        val tvDescription = dialogView.findViewById<TextView>(R.id.tv_description)
        tvDescription.text = currentGuideScreen.description

        // Update icon
        val ivIcon = dialogView.findViewById<ImageView>(R.id.iv_icon)
        if (currentGuideScreen.icon != null) {
            ivIcon.visibility = View.VISIBLE
            ivIcon.setImageDrawable(currentGuideScreen.icon)
        }
        else {
            ivIcon.visibility = View.GONE
        }
    }

    private fun setupButtons() {
        // Next button
        if(guideScreens.size == 1){ // Single tooltip scenario = no more to present
            val btnNext = dialogView.findViewById<MaterialButton>(R.id.btn_action_next_screen)
            btnNext.visibility = View.INVISIBLE
        }
        else{
            val btnNext = dialogView.findViewById<MaterialButton>(R.id.btn_action_next_screen)
            btnNext.setOnClickListener {
                goToNext()
            }
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
        currentIndex = (currentIndex + 1) % guideScreens.size
        updateContent()
        updateDots()
        updateTitle()
    }

    class Builder(private val context: Context) {
        private var guideScreens: MutableList<GuideScreen> = mutableListOf()
        private var onDismiss: (() -> Unit)? = null

        fun addGuideScreen(description: String, title : String? = null, icon: Drawable? = null) = apply {
            guideScreens.add(GuideScreen(title = title, description = description, icon = icon))
        }

        // For showing only one screen with title and info (simple info dialog)
        fun withSingleScreen(title : String, description: String, icon: Drawable? = null) = apply {
            guideScreens.add(GuideScreen(title = title, description = description, icon = icon))
        }

        fun setOnDismissAction(action: () -> Unit) = apply {
            this.onDismiss = action
        }

        /**
         * Build and show the dialog
         */
        fun show() {
            require(guideScreens.isNotEmpty()) { "At least one guide screen is required" }

            GuideDialog(
                context = context,
                guideScreens = guideScreens,
                onDismiss = onDismiss
            ).show()
        }
    }
}