package pl.hexmind.mindshaper.activities.home

import android.content.Intent
import android.os.Bundle
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
        setupHeader(R.drawable.ic_header_home, R.string.common_foobar)
        fabNewThought = findViewById(R.id.fab_new_thought)
        setupHeaderWithGreetings()
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

    private fun setupClickListeners() {
        fabNewThought.setOnClickListener {
            val intent = Intent(this, CaptureActivity::class.java)
            startActivity(intent)
        }
    }
}