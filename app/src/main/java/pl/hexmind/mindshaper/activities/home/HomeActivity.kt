package pl.hexmind.mindshaper.activities.home

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.activities.CoreActivity
import pl.hexmind.mindshaper.activities.capture.CaptureActivity
import pl.hexmind.mindshaper.activities.capture.models.ThoughtMainContentType
import pl.hexmind.mindshaper.common.formatting.setColoredText
import pl.hexmind.mindshaper.common.onboarding.OnboardingProgressStep
import pl.hexmind.mindshaper.services.GreetingsService
import kotlin.math.cos
import kotlin.math.sin

/**
 * Main activity handling FAB menu and swipe gestures for  access
 */
@AndroidEntryPoint
class HomeActivity : CoreActivity() {

    private lateinit var fabNewThought: FloatingActionButton
    private lateinit var fabNewThoughtRichText: FloatingActionButton
    private lateinit var fabNewThoughtRecording: FloatingActionButton

    //private lateinit var fabDrawingType: FloatingActionButton
    //private lateinit var fabPhotoType: FloatingActionButton

    private lateinit var tvHeaderGreetings : TextView
    private var isMenuOpen = false

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
        fabNewThoughtRichText = findViewById(R.id.fab_rich_text_type)
        fabNewThoughtRecording = findViewById(R.id.fab_voice_type)
        //fabDrawingType = findViewById(R.id.fab_drawing_type)
        //fabPhotoType = findViewById(R.id.fab_photo_type)

        setupHeaderWithGreetings()

        // Initially hide all menu buttons
        listOf(fabNewThoughtRichText, fabNewThoughtRecording).forEach { fab ->
            fab.hide()
            fab.alpha = 0f
        }
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
            toggleMenu()
        }

        // RICH NOTES
        fabNewThoughtRichText.setOnClickListener {
            closeMenu()
            val intent = Intent(this, CaptureActivity::class.java)
            intent.putExtra(CaptureActivity.P_INIT_THOUGHT_TYPE, ThoughtMainContentType.RICH_TEXT as Parcelable)
            startActivity(intent)
        }

        // VOICE RECORDING
        fabNewThoughtRecording.setOnClickListener {
            closeMenu()
            val intent = Intent(this, CaptureActivity::class.java)
            intent.putExtra(CaptureActivity.P_INIT_THOUGHT_TYPE, ThoughtMainContentType.RECORDING as Parcelable)
            startActivity(intent)
        }

        // TODO: DRAWING
//        fabDrawingType.setOnClickListener {
//            closeMenu()
//            val intent = Intent(this, CaptureActivity::class.java)
//            intent.putExtra(CaptureActivity.Params.P_INIT_THOUGHT_TYPE, ThoughtMainContentType.DRAWING as Parcelable)
//            startActivity(intent)
//        }

        // TODO: PHOTO
//        fabPhotoType.setOnClickListener {
//            closeMenu()
//            val intent = Intent(this, CaptureActivity::class.java)
//            intent.putExtra(CaptureActivity.P_INIT_THOUGHT_TYPE, ThoughtMainContentType.PHOTO as Parcelable)
//            startActivity(intent)
//        }
    }

    private fun toggleMenu() {
        if (isMenuOpen) {
            closeMenu()
        } else {
            openMenu()
        }
    }

    private fun openMenu() {
        isMenuOpen = true

        // Rotate main FAB
        fabNewThought.animate()
            .rotation(45f)
            .setDuration(300)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        // Show and animate menu buttons - exactly between 90° and 180°
        val fabs = listOf(fabNewThoughtRichText, fabNewThoughtRecording)

        // Angles evenly distributed between 90° and 180°
        val angles = listOf(
            90.0,  // straight up (north)
            120.0, // north-west
            150.0, // more west
            180.0  // straight left (west)
        )

        val radius = 300f

        fabs.forEachIndexed { index, fab ->
            fab.show()

            val angleRad = Math.toRadians(angles[index])
            val x = (radius * cos(angleRad)).toFloat()
            val y = (radius * sin(angleRad)).toFloat()

            fab.animate()
                .translationX(x)
                .translationY(-y) // Negative Y because Android coordinates grow downward
                .alpha(1f)
                .setDuration(300)
                .setStartDelay(index * 50L)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        }
    }

    private fun closeMenu() {
        isMenuOpen = false

        // Rotate main FAB back
        fabNewThought.animate()
            .rotation(0f)
            .setDuration(300)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        // Hide menu buttons
        listOf(fabNewThoughtRichText, fabNewThoughtRecording).forEach { fab ->
            fab.animate()
                .translationX(0f)
                .translationY(0f)
                .alpha(0f)
                .setDuration(300)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withEndAction { fab.hide() }
                .start()
        }
    }
}