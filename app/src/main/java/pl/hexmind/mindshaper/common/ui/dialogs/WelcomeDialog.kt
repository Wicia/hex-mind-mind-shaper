package pl.hexmind.mindshaper.common.ui.dialogs

import android.content.Context
import android.content.res.ColorStateList
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.common.ui.views.HexInputField

/**
 * Welcome shown on the first app launch: your name, then a greeting in Home
 * and whether to get to know the app with tooltips.
 */
class WelcomeDialog(
    private val context: Context,
    private val onYourNameSaved: (yourName: String) -> Unit,
    private val onOnboardingChosen: (enabled: Boolean) -> Unit,
    // However the dialog is closed - also with back, before any answer
    private val onClosed: () -> Unit
) {

    private var currentPage = PAGE_YOUR_NAME
    private var yourName = ""
    private lateinit var dialog: AlertDialog
    private lateinit var dialogView: View

    fun show() {
        dialogView = LayoutInflater.from(context).inflate(R.layout.common_welcome_dialog, null)

        // ! Material 3 adds an invisible 80dp inset above and below - with the keyboard open the window
        // no longer fit and was cut from the bottom; the top offset is set in keepAboveKeyboard()
        dialog = MaterialAlertDialogBuilder(context)
            .setView(dialogView)
            .setBackgroundInsetTop(0)
            .setBackgroundInsetBottom(0)
            .create()

        // Back still closes it - only an accidental tap outside should not
        dialog.setCanceledOnTouchOutside(false)
        dialog.setOnDismissListener { onClosed() }

        dialogView.findViewById<MaterialButton>(R.id.btn_welcome_negative).setOnClickListener {
            if (currentPage == PAGE_YOUR_NAME) {
                goToOnboardingPage()
            }
            else {
                onOnboardingChosen(false)
                dialog.dismiss()
            }
        }

        dialogView.findViewById<MaterialButton>(R.id.btn_welcome_positive).setOnClickListener {
            if (currentPage == PAGE_YOUR_NAME) {
                // Blank = nothing to save, the default name stays
                yourName = dialogView.findViewById<HexInputField>(R.id.hif_your_name).getText()
                if (yourName.isNotBlank()) onYourNameSaved(yourName)
                goToOnboardingPage()
            }
            else {
                onOnboardingChosen(true)
                dialog.dismiss()
            }
        }

        setupDots()
        updatePage()

        // Make dialog wider
        dialog.window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        keepAboveKeyboard()
        dialog.show()
    }

    /**
     * Pinned near the top of the screen - the whole dialog fits above the keyboard.
     * ! Centered, the keyboard covered the buttons: neither adjustResize nor the IME insets moved
     * the floating dialog (MIUI)
     */
    private fun keepAboveKeyboard() {
        val window = dialog.window ?: return
        window.attributes = window.attributes.apply {
            gravity = Gravity.TOP
            y = (TOP_OFFSET_DP * context.resources.displayMetrics.density).toInt()
        }
    }

    private fun goToOnboardingPage() {
        hideKeyboard()
        currentPage = PAGE_ONBOARDING
        updatePage()
    }

    private fun hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(dialogView.windowToken, 0)
    }

    private fun updatePage() {
        val isYourNamePage = currentPage == PAGE_YOUR_NAME

        dialogView.findViewById<TextView>(R.id.tv_welcome_header).text = when {
            isYourNamePage     -> context.getString(R.string.home_welcome_name_title)
            yourName.isBlank() -> context.getString(R.string.home_welcome_home_title)
            else               -> context.getString(R.string.home_welcome_home_title_named, yourName)
        }
        dialogView.findViewById<View>(R.id.ll_page_your_name).isVisible = isYourNamePage
        dialogView.findViewById<View>(R.id.ll_page_onboarding).isVisible = !isYourNamePage

        dialogView.findViewById<MaterialButton>(R.id.btn_welcome_negative).setText(
            if (isYourNamePage) R.string.home_welcome_name_btn_skip else R.string.home_welcome_onboarding_btn_no
        )
        dialogView.findViewById<MaterialButton>(R.id.btn_welcome_positive).setText(
            if (isYourNamePage) R.string.home_welcome_name_btn_save else R.string.home_welcome_onboarding_btn_yes
        )

        updateDots()
    }

    private fun setupDots() {
        val dotsContainer = dialogView.findViewById<LinearLayout>(R.id.ll_dots_indicator)
        val dotSize = (8 * context.resources.displayMetrics.density).toInt()
        val dotMargin = (6 * context.resources.displayMetrics.density).toInt()

        for (i in 0 until PAGES_COUNT) {
            val dot = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(dotSize, dotSize).apply {
                    marginEnd = if (i < PAGES_COUNT - 1) dotMargin else 0
                }
                setImageResource(R.drawable.shape_circle)
            }
            dotsContainer.addView(dot)
        }
    }

    private fun updateDots() {
        val dotsContainer = dialogView.findViewById<LinearLayout>(R.id.ll_dots_indicator)

        for (i in 0 until dotsContainer.childCount) {
            val dot = dotsContainer.getChildAt(i) as ImageView
            dot.imageTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    context,
                    if (i == currentPage) R.color._palette_lvl_2
                    else R.color._gray_lvl_2
                )
            )
        }
    }

    companion object {
        private const val PAGE_YOUR_NAME = 0
        private const val PAGE_ONBOARDING = 1
        private const val PAGES_COUNT = 2
        private const val TOP_OFFSET_DP = 48
    }
}
