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
import pl.hexmind.mindshaper.common.formatting.setColoredText
import pl.hexmind.mindshaper.common.onboarding.OnboardingProgressStep
import pl.hexmind.mindshaper.services.GreetingsService

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
}