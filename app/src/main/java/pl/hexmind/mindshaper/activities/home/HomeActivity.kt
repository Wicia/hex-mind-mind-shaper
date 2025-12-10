package pl.hexmind.mindshaper.activities.home

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.TextView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.activities.CoreActivity
import pl.hexmind.mindshaper.activities.capture.CaptureActivity
import pl.hexmind.mindshaper.activities.capture.models.ThoughtMainContentType
import pl.hexmind.mindshaper.activities.carousel.CarouselActivity
import pl.hexmind.mindshaper.activities.settings.SettingsActivity
import pl.hexmind.mindshaper.services.GreetingsService
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Main activity handling FAB menu and swipe gestures for  access
 */
@AndroidEntryPoint
class HomeActivity : CoreActivity(), GestureDetector.OnGestureListener {

    private lateinit var fabNewThought: FloatingActionButton
    private lateinit var fabNoteType: FloatingActionButton
    private lateinit var fabVoiceType: FloatingActionButton
    private lateinit var fabDrawingType: FloatingActionButton
    private lateinit var fabPhotoType: FloatingActionButton

    private lateinit var tvHeaderGreetings : TextView

    private lateinit var gestureDetector: GestureDetector
    private var isMenuOpen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home_activity)

        initViews()
        setupClickListeners()
        setupGestureDetector()
    }

    private fun initViews() {
        setupHeader(R.drawable.ic_header_home, R.string.common_foobar)
        fabNewThought = findViewById(R.id.fab_new_thought)
        fabNoteType = findViewById(R.id.fab_note_type)
        fabVoiceType = findViewById(R.id.fab_voice_type)
        fabDrawingType = findViewById(R.id.fab_drawing_type)
        fabPhotoType = findViewById(R.id.fab_photo_type)

        setupHeaderWithGreetings()

        // Initially hide all menu buttons
        listOf(fabNoteType, fabVoiceType, fabDrawingType, fabPhotoType).forEach { fab ->
            fab.hide()
            fab.alpha = 0f
        }
    }

    private fun setupHeaderWithGreetings(){
        tvHeaderGreetings = findViewById(R.id.tv_header_title)
        val currentGreetingsText = tvHeaderGreetings.text.toString()
        var newGreetingsText : String
        do {
            newGreetingsText = GreetingsService.getGreetingsString(this)
        } while (currentGreetingsText == newGreetingsText)
        tvHeaderGreetings.text = newGreetingsText

        findViewById<TextView>(R.id.tv_header_subtitle).visibility = View.VISIBLE
        findViewById<TextView>(R.id.tv_header_subtitle).text = appSettingsStorage.getYourName()
    }

    private fun setupClickListeners() {
        fabNewThought.setOnClickListener {
            toggleMenu()
        }

        // RICH NOTES
        fabNoteType.setOnClickListener {
            closeMenu()
            val intent = Intent(this, CaptureActivity::class.java)
            intent.putExtra(CaptureActivity.Params.P_INIT_THOUGHT_TYPE, ThoughtMainContentType.RICH_TEXT as Parcelable)
            startActivity(intent)
        }

        // VOICE RECORDING
        fabVoiceType.setOnClickListener {
            closeMenu()
            val intent = Intent(this, CaptureActivity::class.java)
            intent.putExtra(CaptureActivity.Params.P_INIT_THOUGHT_TYPE, ThoughtMainContentType.RECORDING as Parcelable)
            startActivity(intent)
        }

        // DRAWING
        fabDrawingType.setOnClickListener {
            closeMenu()
            val intent = Intent(this, CaptureActivity::class.java)
            intent.putExtra(CaptureActivity.Params.P_INIT_THOUGHT_TYPE, ThoughtMainContentType.DRAWING as Parcelable)
            startActivity(intent)
        }

        // PHOTO
        fabPhotoType.setOnClickListener {
            closeMenu()
            val intent = Intent(this, CaptureActivity::class.java)
            intent.putExtra(CaptureActivity.P_INIT_THOUGHT_TYPE, ThoughtMainContentType.PHOTO as Parcelable)
            startActivity(intent)
        }
    }

    // Initialize gesture detector for swipe down recognition
    private fun setupGestureDetector() {
        gestureDetector = GestureDetector(this, this)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return gestureDetector.onTouchEvent(event!!) || super.onTouchEvent(event)
    }

    override fun onDown(e: MotionEvent): Boolean {
        return true
    }

    override fun onShowPress(e: MotionEvent) {}

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        return false
    }

    override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
        return false
    }

    override fun onLongPress(e: MotionEvent) {}

    /**
     * Detect swipe gestures
     */
    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        if (e1 == null) return false

        val diffY = e2.y - e1.y
        val diffX = e2.x - e1.x

        // Vertical swipes have priority
        if (abs(diffY) > abs(diffX) && abs(velocityY) > 100) {

            if (diffY > 100) {
                // Swipe down -> open Settings
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                return true
            } else if (diffY < -100) {
                // Swipe up -> close menu if open (example action)
                val intent = Intent(this, CarouselActivity::class.java)
                startActivity(intent)
                return true
            }
        }

        return false
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
        val fabs = listOf(fabNoteType, fabVoiceType, fabDrawingType, fabPhotoType)

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
        listOf(fabNoteType, fabVoiceType, fabDrawingType, fabPhotoType).forEach { fab ->
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