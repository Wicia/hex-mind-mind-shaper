package pl.hexmind.mindshaper.common.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import android.view.View
import android.widget.LinearLayout
import com.google.android.material.button.MaterialButton
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.services.AppSettingsStorage

class NavigationBarController(
    private val navigationBar: View,
    private val appSettings: AppSettingsStorage
) {

    private fun animateIconTint(button: HexNavItemView, from: Int, to: Int) {
        ObjectAnimator.ofArgb(from, to).apply {
            duration = 250
            addUpdateListener {
                button.iconTint = ColorStateList.valueOf(it.animatedValue as Int)
            }
            start()
        }
    }

    private val btnToggle: MaterialButton = navigationBar.findViewById(R.id.btnToggle)
    private val navDrawer: View = navigationBar.findViewById(R.id.navDrawer)
    private val navDrawerScrim: View = navigationBar.findViewById(R.id.navDrawerScrim)
    private val navButtonsContainer: LinearLayout = navigationBar.findViewById(R.id.navButtonsContainer)

    private var isExpanded = false
    private var currentAnimator: Animator? = null

    private val navButtons = listOf(
        NavButton(R.id.navHome, "Home", R.drawable.ic_activity_home, R.drawable.ic_activity_home_filled),
        NavButton(R.id.navStream, "Stream", R.drawable.ic_activity_stream, R.drawable.ic_activity_stream_filled),
        NavButton(R.id.navWorkshop, "Workshop", R.drawable.ic_activity_workshop, R.drawable.ic_activity_workshop_filled),
        NavButton(R.id.navSettings, "Settings", R.drawable.ic_activity_settings, R.drawable.ic_activity_settings_filled)
    )

    private var selectedIndex = -1
    private var onNavigationListener: ((Int, String) -> Unit)? = null

    private val selectedColor: Int
    private val unselectedColor: Int

    init {
        val context = navigationBar.context
        selectedColor = context.getColor(R.color._orange_lvl_3)
        unselectedColor = context.getColor(R.color._orange_lvl_3)

        navDrawer.translationX = hiddenDrawerX()
        navDrawer.visibility = View.GONE
        navDrawerScrim.visibility = View.GONE

        setupNavButtons()
        setupToggleButton()
        setupScrim()
    }

    private fun hiddenDrawerX(): Float {
        val fallback = 260f * navDrawer.resources.displayMetrics.density
        return if (navDrawer.width > 0) -navDrawer.width.toFloat() else -fallback
    }

    private fun setupNavButtons() {
        navButtons.forEachIndexed { index, navButton ->
            val button = navigationBar.findViewById<HexNavItemView>(navButton.id)

            button?.setOnClickListener {
                selectButton(index, animate = true)
                closeDrawer {
                    onNavigationListener?.invoke(index, navButton.label)
                }
            }
        }
    }

    private fun setupToggleButton() {
        // MaterialButton's stateListAnimator overrides android:elevation, so the 12dp drawer
        // would cover the toggle - kill it and lift Z above the drawer
        btnToggle.stateListAnimator = null
        btnToggle.elevation = navigationBar.resources.displayMetrics.density * 24f
        btnToggle.setIconResource(R.drawable.ic_nav_menu)

        btnToggle.setOnClickListener {
            currentAnimator?.cancel()
            if (isExpanded) {
                closeDrawer()
            } else {
                openDrawer()
            }
        }
    }

    private fun setupScrim() {
        navDrawerScrim.setOnClickListener {
            closeDrawer()
        }
    }

    private fun setToggleActive(active: Boolean) {
        val context = navigationBar.context
        val backgroundColor = if (active) R.color.app_primary else R.color.button_auxiliary_background
        val iconColor = if (active) R.color.text_on_primary else R.color.button_auxiliary_icon
        btnToggle.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, backgroundColor))
        btnToggle.iconTint = ColorStateList.valueOf(ContextCompat.getColor(context, iconColor))
        btnToggle.animate().rotation(if (active) 180f else 0f).setDuration(200).start()
    }

    private fun openDrawer() {
        currentAnimator?.cancel()
        isExpanded = true
        navDrawer.visibility = View.VISIBLE
        navDrawerScrim.visibility = View.VISIBLE

        navDrawerScrim.alpha = 0f
        navDrawer.translationX = hiddenDrawerX()

        val drawerAnimator = ObjectAnimator.ofFloat(navDrawer, View.TRANSLATION_X, navDrawer.translationX, 0f)
        val scrimAnimator = ObjectAnimator.ofFloat(navDrawerScrim, View.ALPHA, 0f, 1f)

        currentAnimator = AnimatorSet().apply {
            playTogether(drawerAnimator, scrimAnimator)
            duration = 280
            interpolator = android.view.animation.DecelerateInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    currentAnimator = null
                    navDrawer.translationX = 0f
                    navDrawerScrim.alpha = 1f
                }
            })
            start()
        }

        setToggleActive(true)
    }

    private fun closeDrawer(onComplete: (() -> Unit)? = null) {
        if (!isExpanded && navDrawer.visibility != View.VISIBLE) {
            onComplete?.invoke()
            return
        }

        currentAnimator?.cancel()
        isExpanded = false

        val targetX = hiddenDrawerX()
        val drawerAnimator = ObjectAnimator.ofFloat(navDrawer, View.TRANSLATION_X, navDrawer.translationX, targetX)
        val scrimAnimator = ObjectAnimator.ofFloat(navDrawerScrim, View.ALPHA, navDrawerScrim.alpha, 0f)

        currentAnimator = AnimatorSet().apply {
            playTogether(drawerAnimator, scrimAnimator)
            duration = 220
            interpolator = android.view.animation.AccelerateInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    navDrawer.visibility = View.GONE
                    navDrawerScrim.visibility = View.GONE
                    navDrawer.translationX = targetX
                    navDrawerScrim.alpha = 0f
                    currentAnimator = null
                    onComplete?.invoke()
                }

                override fun onAnimationCancel(animation: Animator) {
                    currentAnimator = null
                }
            })
            start()
        }

        setToggleActive(false)
    }

    private fun selectButton(index: Int, animate: Boolean = true) {
        selectedIndex = index

        navButtons.forEachIndexed { i, navButton ->
            val button = navigationBar.findViewById<HexNavItemView>(navButton.id)

            button?.let {
                if (i == index) {
                    it.setIconResource(navButton.iconFilled)
                    it.setTextColor(selectedColor)
                    if (animate) {
                        animateIconTint(it, unselectedColor, selectedColor)
                    } else {
                        it.iconTint = ColorStateList.valueOf(selectedColor)
                    }
                } else {
                    it.setIconResource(navButton.icon)
                    it.setTextColor(android.graphics.Color.WHITE)
                    if (animate) {
                        animateIconTint(it, selectedColor, unselectedColor)
                    } else {
                        it.iconTint = ColorStateList.valueOf(unselectedColor)
                    }
                }
            }
        }
    }

    fun setSelectedButton(index: Int) {
        if (index in navButtons.indices) {
            selectButton(index, animate = false)
        }
    }

    fun setOnNavigationListener(listener: (Int, String) -> Unit) {
        onNavigationListener = listener
    }

    fun cleanup() {
        currentAnimator?.cancel()
        currentAnimator = null
    }

    data class NavButton(
        val id: Int,
        val label: String,
        val icon: Int,
        val iconFilled: Int
    )
}
