package pl.hexmind.mindshaper.activities

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
import pl.hexmind.mindshaper.activities.capture.CaptureActivity
import pl.hexmind.mindshaper.activities.capture.models.NavigationBarController
import pl.hexmind.mindshaper.activities.details.DetailsActivity
import pl.hexmind.mindshaper.activities.home.HomeActivity
import pl.hexmind.mindshaper.activities.settings.SettingsActivity
import pl.hexmind.mindshaper.activities.stream.StreamActivity
import pl.hexmind.mindshaper.activities.workshop.WorkshopActivity
import pl.hexmind.mindshaper.common.onboarding.OnboardingManager
import pl.hexmind.mindshaper.services.AppSettingsStorage
import pl.hexmind.mindshaper.services.PermissionService
import javax.inject.Inject

/**
 * Core activity with bottom navigation bar overlay
 */
open class CoreActivity : AppCompatActivity() {

    @Inject
    lateinit var appSettingsStorage: AppSettingsStorage

    @Inject
    lateinit var permissionsService: PermissionService

    @Inject
    lateinit var onboardingManager : OnboardingManager

    private var navigationController: NavigationBarController? = null
    private var navigationBarView: View? = null

    companion object {
        /**
         * Maps activity class names to navigation bar indices
         */
        private val activityToIndex = mapOf(
            HomeActivity::class.java.simpleName     to 0,
                CaptureActivity::class.simpleName   to 0, // Highlight Home icon when in Capture
            StreamActivity::class.java.simpleName   to 1,
                DetailsActivity::class.simpleName   to 1, // Highlight Stream icon when in Details
            WorkshopActivity::class.java.simpleName to 2,
            SettingsActivity::class.java.simpleName to 3
        )
    }

// -------------------------------------------------------------------------------------------------
//      CORE
// -------------------------------------------------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdgeDisplay()
    }

    override fun onContentChanged() {
        super.onContentChanged()
        addNavigationBarOverlay()
    }

    override fun onResume() {
        super.onResume()
        highlightCurrentScreen()
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

    fun setupHeader(@DrawableRes iconRes: Int, @StringRes titleRes: Int) {
        findViewById<ImageView>(R.id.iv_header_icon)?.setImageResource(iconRes)
        findViewById<TextView>(R.id.tv_header_title)?.setText(titleRes)
    }

// -------------------------------------------------------------------------------------------------
//      NAVIGATION
// -------------------------------------------------------------------------------------------------

    /**
     * Enables edge-to-edge display, allowing the app to draw in the system bars area.
     *
     * More: System bars remain on top (Z-axis) but the app can draw behind them.
     * Use WindowInsets to add padding/margins and avoid content being obscured by system bars.
     */
    private fun enableEdgeToEdgeDisplay(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        }
        else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )
        }
    }

    /**
     * Adds navigation bar as an overlay at the bottom of the screen
     */
    private fun addNavigationBarOverlay() {
        val contentView = window.decorView.findViewById<ViewGroup>(android.R.id.content)
        val decorView = window.decorView as ViewGroup

        if (contentView != null && navigationBarView == null) {
            // Apply window insets to main content (add padding for system bars)
            // This ensures content is not hidden under system bars
            ViewCompat.setOnApplyWindowInsetsListener(contentView) { view, insets ->
                val systemBarsInsets = insets.getInsets(
                    androidx.core.view.WindowInsetsCompat.Type.systemBars()
                )

                // Apply padding to activity's content to avoid being hidden by system bars
                view.setPadding(
                    systemBarsInsets.left,
                    systemBarsInsets.top, // system status bar
                    systemBarsInsets.right,
                    systemBarsInsets.bottom // system navigation bar
                )

                // Don't consume insets - let navigation bar handle them too
                insets
            }

            navigationBarView = LayoutInflater.from(this)
                .inflate(R.layout.z_navigation_bar, null, false)

            val params = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.START
                bottomMargin = 0
            }

            ViewCompat.setElevation(navigationBarView!!, 8f)

            // Add navigation bar to decorView instead of contentView
            // This way it's not affected by the content padding
            decorView.addView(navigationBarView, params)

            // Apply window insets to navigation bar
            // This positions it directly above system navigation bar
            ViewCompat.setOnApplyWindowInsetsListener(navigationBarView!!) { view, insets ->
                val systemBarsInsets = insets.getInsets(
                    androidx.core.view.WindowInsetsCompat.Type.systemBars()
                )

                val layoutParams = view.layoutParams as FrameLayout.LayoutParams
                // No extra margin - position directly above system bar
                layoutParams.bottomMargin = systemBarsInsets.bottom
                view.layoutParams = layoutParams

                insets
            }

            initializeNavigationController()
        }
    }

    /**
     * Initializes the navigation controller
     */
    private fun initializeNavigationController() {
        navigationBarView?.let { navBar ->
            navigationController = NavigationBarController(navBar, appSettingsStorage)
            navigationController?.setOnNavigationListener { index, _ ->
                when (index) {
                    0 -> navigateToHome()
                    1 -> navigateToStream()
                    2 -> navigateToWorkshop()
                    3 -> navigateToSettings()
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

    // Navigation methods
    private fun navigateToHome() {
        if (this::class.java.simpleName != HomeActivity::class.java.simpleName) {
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            overrideTransitions()
        }
    }

    private fun navigateToStream() {
        if (this::class.java.simpleName != StreamActivity::class.java.simpleName) {
            val intent = Intent(this, StreamActivity::class.java)
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

    private fun navigateToWorkshop() {
        if (this::class.java.simpleName != WorkshopActivity::class.java.simpleName) {
            val intent = Intent(this, WorkshopActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            overrideTransitions()
        }
    }

// -----------------------------------------
//      DIALOGS & TOASTS
// -----------------------------------------

    fun showShortToast(stringResourceId : Int, param : String? = ""){
        Toast.makeText(this, getString(stringResourceId, param), Toast.LENGTH_SHORT).show()
    }

    fun showErrorAndFinish(stringResourceId : Int) {
        Toast.makeText(this, getString(stringResourceId), Toast.LENGTH_SHORT).show()
        finish()
    }

// -----------------------------------------
//      EXTRAS & EFFECTS
// -----------------------------------------

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