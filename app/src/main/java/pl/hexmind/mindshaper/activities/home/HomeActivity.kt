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
import pl.hexmind.mindshaper.activities.flashcards.FlashcardsActivity
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

        // Start-screen routing: Home is the launcher, so on a cold launch it hands over to the
        // user-chosen entry screen before drawing anything (Home stays below it -> BACK returns here)
        if (openChosenStartScreenIfNeeded(savedInstanceState)) {
            return
        }

        setContentView(R.layout.home_activity)

        initViews()
        setupClickListeners()

        onboardingManager.showTooltipForStep(
            OnboardingProgressStep.HOME_TOOLTIP, this
        )
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
     * Opens the user-chosen start screen on a genuine cold launch from the launcher icon.
     * This launcher instance finishes before its first frame (no Home flash); the stack becomes
     * Home -> chosen screen, and that Home is created only when BACK reaches it.
     * @return true when rerouted (this instance is finishing)
     */
    private fun openChosenStartScreenIfNeeded(savedInstanceState: Bundle?): Boolean {
        // Only a real cold launch reroutes: nav-bar navigation to Home carries no MAIN/LAUNCHER intent,
        // and a config-change recreation (e.g. rotation) has a non-null savedInstanceState
        val isColdLaunchFromLauncher = savedInstanceState == null
                && isTaskRoot
                && intent.action == Intent.ACTION_MAIN
                && intent.hasCategory(Intent.CATEGORY_LAUNCHER)

        if (!isColdLaunchFromLauncher) {
            return false
        }

        val targetScreen = when (appSettingsStorage.getStartScreen()) {
            StartScreen.STREAM     -> StreamActivity::class.java
            // Flashcards feature off = its screen is hidden, Home stays
            StartScreen.FLASHCARDS -> if (appSettingsStorage.isFlashcardsFeatureEnabled()) FlashcardsActivity::class.java else return false
            StartScreen.WORKSHOP   -> WorkshopActivity::class.java
            StartScreen.HOME       -> return false
        }

        // Plain Home intent (no MAIN/LAUNCHER) -> the Home below does not reroute again
        startActivities(arrayOf(
            Intent(this, HomeActivity::class.java),
            Intent(this, targetScreen)
        ))
        finish()
        return true
    }
}