package pl.hexmind.fastnote.features.main

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlin.math.cos
import kotlin.math.sin
import pl.hexmind.fastnote.R
import pl.hexmind.fastnote.features.capture.ThoughtsCaptureActivity
import pl.hexmind.fastnote.features.capture.models.InitialThoughtType

class MainActivity : AppCompatActivity() {

    private lateinit var fab_new_thought: FloatingActionButton
    private lateinit var fab_note_type: FloatingActionButton
    private lateinit var fab_voice_type: FloatingActionButton
    private lateinit var fab_drawing_type: FloatingActionButton
    private lateinit var fab_photo_type: FloatingActionButton

    private var isMenuOpen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_layout)

        initViews()
        setupClickListeners()
    }

    private fun initViews() {
        fab_new_thought = findViewById(R.id.fab_new_thought)
        fab_note_type = findViewById(R.id.fab_note_type)
        fab_voice_type = findViewById(R.id.fab_voice_type)
        fab_drawing_type = findViewById(R.id.fab_drawing_type)
        fab_photo_type = findViewById(R.id.fab_photo_type)

        // Initially hide all menu buttons
        listOf(fab_note_type, fab_voice_type, fab_drawing_type, fab_photo_type).forEach { fab ->
            fab.hide()
            fab.alpha = 0f
        }
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
            // Handle document action
            closeMenu()
        }

        fab_photo_type.setOnClickListener {
            // Handle add action
            closeMenu()
        }
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