package pl.hexmind.mindshaper.activities

import NavigationBarController
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.activities.carousel.CarouselActivity
import pl.hexmind.mindshaper.activities.home.HomeActivity
import pl.hexmind.mindshaper.activities.settings.SettingsActivity
import pl.hexmind.mindshaper.services.AppSettingsStorage
import pl.hexmind.mindshaper.services.PhasesService
import javax.inject.Inject

/**
 * Core activity with bottom navigation bar overlay
 */
open class CoreActivity() : AppCompatActivity() {

    @Inject
    lateinit var phasesService: PhasesService

    @Inject
    lateinit var appSettingsStorage: AppSettingsStorage

    private var navigationController: NavigationBarController? = null
    private var navigationBarView: View? = null

    companion object {
        /**
         * Maps activity class names to navigation bar indices
         */
        private val activityToIndex = mapOf(
            HomeActivity::class.java.simpleName to 0,
            CarouselActivity::class.java.simpleName to 1,
            SettingsActivity::class.java.simpleName to 2
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onContentChanged() {
        super.onContentChanged()
        addNavigationBarOverlay()
    }

    override fun onResume() {
        super.onResume()
        highlightCurrentScreen()
    }

    /**
     * Adds navigation bar as an overlay at the bottom of the screen
     */
    private fun addNavigationBarOverlay() {
        val rootView = window.decorView.findViewById<ViewGroup>(android.R.id.content)

        if (rootView != null && navigationBarView == null) {
            navigationBarView = LayoutInflater.from(this)
                .inflate(R.layout.common_navigation_bar, null, false)

            val params = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.START
                bottomMargin = 0
            }

            ViewCompat.setElevation(navigationBarView!!, 8f)

            rootView.addView(navigationBarView, params)

            initializeNavigationController()
        }
    }

    /**
     * Initializes the navigation controller
     */
    private fun initializeNavigationController() {
        navigationBarView?.let { navBar ->
            navigationController = NavigationBarController(navBar, appSettingsStorage)

            navigationController?.setOnNavigationListener { index, label ->
                when (index) {
                    0 -> navigateToHome()
                    1 -> navigateToCarousel()
                    2 -> navigateToSettings()
                }
            }
        }
    }

    /**
     * Highlights the navigation button corresponding to current screen
     */
    private fun highlightCurrentScreen() {
        val currentActivityName = this::class.java.simpleName
        val index = activityToIndex[currentActivityName]

        if (index != null && index >= 0) {
            navigationController?.setSelectedButton(index)
        }
    }

    /**
     * Hide/show navigation bar programmatically
     */
    protected fun setNavigationBarVisible(visible: Boolean) {
        navigationBarView?.visibility = if (visible) View.VISIBLE else View.GONE
    }

    // Navigation methods
    private fun navigateToHome() {
        if (this::class.java.simpleName != HomeActivity::class.java.simpleName) {
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            overrideTransitions()
        }
    }

    private fun navigateToCarousel() {
        if (this::class.java.simpleName != CarouselActivity::class.java.simpleName) {
            val intent = Intent(this, CarouselActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            overrideTransitions()
        }
    }

    private fun navigateToSettings() {
        if (this::class.java.simpleName != SettingsActivity::class.java.simpleName) {
            val intent = Intent(this, SettingsActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            overrideTransitions()
        }
    }

    fun showShortToast(stringResourceId : Int, param : String? = ""){
        Toast.makeText(this, getString(stringResourceId, param), Toast.LENGTH_SHORT).show()
    }

    fun showErrorAndFinish(stringResourceId : Int) {
        Toast.makeText(this, getString(stringResourceId), Toast.LENGTH_SHORT).show()
        finish()
    }

    fun setupHeader(@DrawableRes iconRes: Int, @StringRes titleRes: Int) {
        findViewById<ImageView>(R.id.iv_header_icon)?.setImageResource(iconRes)
        findViewById<TextView>(R.id.tv_header_title)?.setText(titleRes)
    }

    override fun onDestroy() {
        super.onDestroy()
        navigationBarView?.let { navBar ->
            (navBar.parent as? ViewGroup)?.removeView(navBar)
        }
        navigationBarView = null
        navigationController?.cleanup()
        navigationController = null
    }

    /**
     * Disables activity transition animations
     * Uses new API for Android 14+ and deprecated API for older versions
     */
    private fun overrideTransitions() {
        // Android 14+ (API 34+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
        }
        // Android 13 & below
        else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }
}