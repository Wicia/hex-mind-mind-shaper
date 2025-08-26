package pl.hexmind.fastnote.activities.main

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.TextView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import pl.hexmind.fastnote.R
import pl.hexmind.fastnote.activities.capture.ThoughtsCaptureActivity
import pl.hexmind.fastnote.activities.capture.models.InitialThoughtType
import pl.hexmind.fastnote.activities.carousel.ThoughtCarouselActivity
import pl.hexmind.fastnote.services.AppSettingsStorage
import pl.hexmind.fastnote.services.GreetingsService
import pl.hexmind.fastnote.activities.settings.SettingsActivity
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

// Main activity handling FAB menu and swipe gestures for settings access
class MainActivity : CoreActivity(), GestureDetector.OnGestureListener {

    private lateinit var appSettingsStorage: AppSettingsStorage

    private lateinit var fab_new_thought: FloatingActionButton
    private lateinit var fab_note_type: FloatingActionButton
    private lateinit var fab_voice_type: FloatingActionButton
    private lateinit var fab_drawing_type: FloatingActionButton
    private lateinit var fab_photo_type: FloatingActionButton

    private lateinit var header_greetings : TextView

    private lateinit var gestureDetector: GestureDetector
    private var isMenuOpen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupClickListeners()
        setupGestureDetector()
    }

    private fun initViews() {
        appSettingsStorage = AppSettingsStorage(this)

        fab_new_thought = findViewById(R.id.fab_new_thought)
        fab_note_type = findViewById(R.id.fab_note_type)
        fab_voice_type = findViewById(R.id.fab_voice_type)
        fab_drawing_type = findViewById(R.id.fab_drawing_type)
        fab_photo_type = findViewById(R.id.fab_photo_type)

        setupGreetingsTextView()

        // Initially hide all menu buttons
        listOf(fab_note_type, fab_voice_type, fab_drawing_type, fab_photo_type).forEach { fab ->
            fab.hide()
            fab.alpha = 0f
        }
    }

    //TODO: moze to dac do klasy Service -> przekazac dodatkowy parametr currentTemplate?
    private fun setupGreetingsTextView(){
        header_greetings = findViewById(R.id.tv_greetings)
        val currentGreetingsText = header_greetings.text.toString()
        var newGreetingsText : String
        do {
            newGreetingsText = GreetingsService.getGreetingsString(this, appSettingsStorage.getYourName())
        } while (currentGreetingsText == newGreetingsText)
        header_greetings.text = newGreetingsText
    }

    private fun setupClickListeners() {
        fab_new_thought.setOnClickListener {
            toggleMenu()
        }

        fab_note_type.setOnClickListener {
            // Handle camera action
            closeMenu()
            val intent = Intent(this, ThoughtsCaptureActivity::class.java)
            intent.putExtra(ThoughtsCaptureActivity.INPUT_TYPE, InitialThoughtType.NOTE as Parcelable)
            startActivity(intent)
        }

        fab_voice_type.setOnClickListener {
            // Handle edit action
            closeMenu()
            val intent = Intent(this, ThoughtsCaptureActivity::class.java)
            intent.putExtra(ThoughtsCaptureActivity.INPUT_TYPE, InitialThoughtType.VOICE as Parcelable)
            startActivity(intent)
        }

        fab_drawing_type.setOnClickListener {
            // TODO: To be replaced with proper code !!!
            val intent = Intent(this, ThoughtCarouselActivity::class.java)
            startActivity(intent)
            closeMenu()
        }

        fab_photo_type.setOnClickListener {
            // Handle add action
            closeMenu()
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

    // Detect swipe down gesture and open settings
    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        if (e1 == null) return false

        val diffY = e2.y - e1.y
        val diffX = e2.x - e1.x

        // Check if it's a vertical swipe down with sufficient distance and velocity
        if (abs(diffY) > abs(diffX) &&
            diffY > 100 &&
            abs(velocityY) > 100) {

            // Open settings activity
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
            return true
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
        fab_new_thought.animate()
            .rotation(45f)
            .setDuration(300)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        // Show and animate menu buttons - DOKŁADNIE 90° do 180°
        val fabs = listOf(fab_note_type, fab_voice_type, fab_drawing_type, fab_photo_type)

        // NOWE kąty - równomiernie rozłożone między 90° a 180°
        val angles = listOf(
            90.0,  // czysto góra (północ)
            120.0, // północny-zachód
            150.0, // bardziej zachód
            180.0  // czysto lewo (zachód)
        )

        val radius = 300f

        fabs.forEachIndexed { index, fab ->
            fab.show()

            val angleRad = Math.toRadians(angles[index])
            val x = (radius * cos(angleRad)).toFloat()
            val y = (radius * sin(angleRad)).toFloat()

            fab.animate()
                .translationX(x)
                .translationY(-y) // Ujemne Y bo Android ma Y w dół
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
        fab_new_thought.animate()
            .rotation(0f)
            .setDuration(300)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        // Hide menu buttons
        listOf(fab_note_type, fab_voice_type, fab_drawing_type, fab_photo_type).forEach { fab ->
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