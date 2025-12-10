import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.res.ColorStateList
import android.view.View
import android.widget.LinearLayout
import com.google.android.material.button.MaterialButton
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.common.ui.dpToPx
import pl.hexmind.mindshaper.services.AppSettingsStorage

class NavigationBarController(
    private val navigationBar: View,
    private val appSettings: AppSettingsStorage
) {

    private val btnToggle: MaterialButton = navigationBar.findViewById(R.id.btnToggle)
    private val navButtonsContainer: LinearLayout = navigationBar.findViewById(R.id.navButtonsContainer)

    private var isExpanded = false
    private var currentAnimator: Animator? = null

    // Navigation buttons
    private val navButtons = listOf(
        NavButton(R.id.navHome, "Home"),
        NavButton(R.id.navCarousel, "Carousel"),
        NavButton(R.id.navSettings, "Settings"),
    )

    private var selectedIndex = -1
    private var onNavigationListener: ((Int, String) -> Unit)? = null

    // Colors for selected/unselected states
    private val selectedColor: Int
    private val unselectedColor: Int

    init {
        val context = navigationBar.context
        selectedColor = context.getColor(R.color._orange_5_dark)
        unselectedColor = context.getColor(R.color._orange_3_mid)

        setupNavButtons()
        setupToggleButton()

        // Restore ONLY expanded state
        restoreExpandedState()
    }

    /**
     * Restores only expanded/collapsed state
     */
    private fun restoreExpandedState() {
        val wasExpanded = appSettings.isNavigationExpanded()
        if (wasExpanded) {
            // Show expanded without animation
            isExpanded = true
            navButtonsContainer.visibility = View.VISIBLE
            navButtonsContainer.alpha = 1f
            navButtonsContainer.scaleX = 1f
            navButtonsContainer.scaleY = 1f
            btnToggle.setIconResource(R.drawable.ic_nav_collapse)
        }
    }

    /**
     * Sets up all navigation buttons with click listeners
     */
    private fun setupNavButtons() {
        navButtons.forEachIndexed { index, navButton ->
            val button = navigationBar.findViewById<MaterialButton>(navButton.id)

            button?.setOnClickListener {
                selectButton(index, animate = true)
                onNavigationListener?.invoke(index, navButton.label)
            }
        }
    }

    /**
     * Sets up the toggle button for expanding/collapsing
     */
    private fun setupToggleButton() {
        btnToggle.setOnClickListener {
            currentAnimator?.cancel()
            isExpanded = !isExpanded
            animateNavBar()

            appSettings.setNavigationExpanded(isExpanded)
        }
    }

    /**
     * Animates the navigation bar expansion/collapse
     */
    private fun animateNavBar() {
        if (isExpanded) {
            expandNavBar()
        } else {
            collapseNavBar()
        }
    }

    /**
     * Expands the navigation bar to show all buttons
     */
    private fun expandNavBar() {
        navButtonsContainer.visibility = View.VISIBLE

        val alphaAnimator = ObjectAnimator.ofFloat(navButtonsContainer, "alpha", 0f, 1f)
        val scaleXAnimator = ObjectAnimator.ofFloat(navButtonsContainer, "scaleX", 0.7f, 1f)
        val scaleYAnimator = ObjectAnimator.ofFloat(navButtonsContainer, "scaleY", 0.7f, 1f)

        currentAnimator = AnimatorSet().apply {
            playTogether(alphaAnimator, scaleXAnimator, scaleYAnimator)
            duration = 300

            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    currentAnimator = null
                    navButtonsContainer.visibility = View.VISIBLE
                    navButtonsContainer.alpha = 1f
                    navButtonsContainer.scaleX = 1f
                    navButtonsContainer.scaleY = 1f
                }
            })

            start()
        }

        btnToggle.setIconResource(R.drawable.ic_nav_collapse)
    }

    /**
     * Collapses the navigation bar to hide all buttons
     */
    private fun collapseNavBar() {
        val alphaAnimator = ObjectAnimator.ofFloat(navButtonsContainer, "alpha", 1f, 0f)
        val scaleXAnimator = ObjectAnimator.ofFloat(navButtonsContainer, "scaleX", 1f, 0.7f)
        val scaleYAnimator = ObjectAnimator.ofFloat(navButtonsContainer, "scaleY", 1f, 0.7f)

        currentAnimator = AnimatorSet().apply {
            playTogether(alphaAnimator, scaleXAnimator, scaleYAnimator)
            duration = 300

            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    navButtonsContainer.visibility = View.GONE
                    currentAnimator = null
                }

                override fun onAnimationCancel(animation: Animator) {
                    currentAnimator = null
                }
            })

            start()
        }

        btnToggle.setIconResource(R.drawable.ic_nav_expand)
    }

    /**
     * Selects a navigation button and updates visual state
     * @param index The index of the button to select
     * @param animate Whether to animate the transition
     */
    private fun selectButton(index: Int, animate: Boolean = true) {
        selectedIndex = index

        navButtons.forEachIndexed { i, navButton ->
            val button = navigationBar.findViewById<MaterialButton>(navButton.id)

            button?.let {
                if (i == index) {
                    // Selected state
                    it.iconTint = ColorStateList.valueOf(selectedColor)

                    if (animate) {
                        ObjectAnimator.ofInt(
                            it,
                            "iconSize",
                            it.iconSize,
                            navigationBar.context.dpToPx(32)
                        ).apply {
                            duration = 200
                            start()
                        }
                    } else {
                        it.iconSize = navigationBar.context.dpToPx(32)
                    }
                }
                else {
                    // Normal state
                    it.iconTint = ColorStateList.valueOf(unselectedColor)

                    if (animate) {
                        ObjectAnimator.ofInt(
                            it,
                            "iconSize",
                            it.iconSize,
                            navigationBar.context.dpToPx(28)
                        ).apply {
                            duration = 200
                            start()
                        }
                    } else {
                        it.iconSize = navigationBar.context.dpToPx(28)
                    }
                }
            }
        }
    }

    /**
     * Manually set selected button from outside (e.g., from Activity)
     */
    fun setSelectedButton(index: Int) {
        if (index in navButtons.indices) {
            selectButton(index, animate = false)
        }
    }

    /**
     * Sets the navigation listener to handle button clicks
     */
    fun setOnNavigationListener(listener: (Int, String) -> Unit) {
        onNavigationListener = listener
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        currentAnimator?.cancel()
        currentAnimator = null
    }

    data class NavButton(val id: Int, val label: String)
}