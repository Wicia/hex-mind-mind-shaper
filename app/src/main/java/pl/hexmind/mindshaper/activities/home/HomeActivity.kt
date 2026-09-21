package pl.hexmind.mindshaper.activities.home

import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.activities.CoreActivity
import pl.hexmind.mindshaper.activities.capture.CaptureActivity
import pl.hexmind.mindshaper.activities.stream.StreamActivity
import pl.hexmind.mindshaper.activities.workshop.WorkshopActivity
import pl.hexmind.mindshaper.common.formatting.setColoredText
import pl.hexmind.mindshaper.common.onboarding.OnboardingProgressStep
import pl.hexmind.mindshaper.services.GreetingsService
import pl.hexmind.mindshaper.services.dto.StartScreen

/**
 * Main activity handling FAB menu and swipe gestures for  access
 */
@AndroidEntryPoint
class HomeActivity : CoreActivity() {

    private lateinit var fabNewThought: FloatingActionButton

    private lateinit var tvHeaderGreetings : TextView

    private lateinit var tvBuildVersion : TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home_activity)

        initViews()
        setupClickListeners()

        onboardingManager.showTooltipForStep(
            OnboardingProgressStep.HOME_TOOLTIP, this
        )

        // Start-screen routing: Home is the launcher, so on a cold launch push the user-chosen
        // entry screen on top (Home stays as root -> BACK returns here)
        openChosenStartScreenIfNeeded(savedInstanceState)
    }

    private fun initViews() {
        setupHeader(R.drawable.ic_activity_home, R.string.common_foobar)
        fabNewThought = findViewById(R.id.fab_new_thought)
        tvBuildVersion = findViewById(R.id.tv_build_version)
        setupHeaderWithGreetings()
        setupBuildVersion()
    }

    private fun setupHeaderWithGreetings(){
        tvHeaderGreetings = findViewById(R.id.tv_header_title)
        val currentGreetingsText = tvHeaderGreetings.text.toString()
        var newGreetingsText : String
        do {
            newGreetingsText = GreetingsService.getGreetingsString(this, appSettingsStorage.getYourName())
        } while (currentGreetingsText == newGreetingsText)

        tvHeaderGreetings.setColoredText(newGreetingsText, appSettingsStorage.getYourName(),
            ContextCompat.getColor(this, R.color._orange_lvl_3))
    }

    private fun setupBuildVersion() {
        // ! not compile time (a real build date needs a buildConfigField that changes every build and invalidates the Gradle cache)
        val installedAt = packageManager.getPackageInfo(packageName, 0).lastUpdateTime
        val formattedDate = DateFormat.format("yyyy_MM_dd", installedAt).toString()
        tvBuildVersion.text = getString(R.string.home_build_version, formattedDate)
    }

    private fun setupClickListeners() {
        fabNewThought.setOnClickListener {
            val intent = Intent(this, CaptureActivity::class.java)
            startActivity(intent)
        }
    }

    /**
     * Pushes the user-chosen start screen on top of Home on a genuine cold launch from the launcher icon.
     * Home is left in the stack (no finish), so BACK from Stream/Workshop lands on a fully rendered Home.
     */
    private fun openChosenStartScreenIfNeeded(savedInstanceState: Bundle?) {
        // Only a real cold launch reroutes: nav-bar navigation to Home carries no MAIN/LAUNCHER intent,
        // and a config-change recreation (e.g. rotation) has a non-null savedInstanceState
        val isColdLaunchFromLauncher = savedInstanceState == null
                && isTaskRoot
                && intent.action == Intent.ACTION_MAIN
                && intent.hasCategory(Intent.CATEGORY_LAUNCHER)

        if (!isColdLaunchFromLauncher) {
            return
        }

        val targetScreen = when (appSettingsStorage.getStartScreen()) {
            StartScreen.STREAM   -> StreamActivity::class.java
            StartScreen.WORKSHOP -> WorkshopActivity::class.java
            StartScreen.HOME     -> return
        }

        startActivity(Intent(this, targetScreen))
    }
}