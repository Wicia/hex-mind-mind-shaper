package pl.hexmind.mindshaper.activities.details

import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.activities.CoreActivity
import pl.hexmind.mindshaper.services.dto.ThoughtDTO

class ThoughtDetailsActivity : CoreActivity() {

    private lateinit var essenceText: TextView
    private lateinit var areaAndSubjectText: TextView
    private lateinit var emotionIcon: ImageView
    private lateinit var soundButton: Button
    private lateinit var drawingButton: Button
    private lateinit var noteButton: Button
    private lateinit var photoButton: Button

    companion object PARAMS {
        const val P_SELECTED_THOUGHT_ID = "P_SELECTED_THOUGHT_ID"
    }

    private var dtoWithDetails : ThoughtDTO? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_thought_details)

        saveExtrasFromIntent()
        initializeViews()
        setupProgressBar()
        setupButtons()
    }

    private fun initializeViews() {
        // Header
        essenceText = findViewById(R.id.essenceText)
        areaAndSubjectText = findViewById(R.id.areaText)
        emotionIcon = findViewById(R.id.emotionIcon)

        // Components
        soundButton = findViewById(R.id.soundButton)
        drawingButton = findViewById(R.id.drawingButton)
        noteButton = findViewById(R.id.noteButton)
        photoButton = findViewById(R.id.photoButton)
    }

    private fun saveExtrasFromIntent() {
        dtoWithDetails = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(P_SELECTED_THOUGHT_ID, ThoughtDTO::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(P_SELECTED_THOUGHT_ID) as? ThoughtDTO
        }
    }

    private fun setupProgressBar() {
        // Load proper image and set it
    }

    private fun setupButtons() {
        soundButton.setOnClickListener {
            playVoiceRecording()
        }

        drawingButton.setOnClickListener {
            showSavedDrawing()
        }

        noteButton.setOnClickListener {
            showSavedNote()
        }

        photoButton.setOnClickListener {
            showSavedPhoto()
        }
    }

    // Empty methods for future implementation
    private fun playVoiceRecording() {
        Toast.makeText(this, "Playing voice recording...", Toast.LENGTH_SHORT).show()
        // TODO: Implement voice recording playback
    }

    private fun showSavedDrawing() {
        Toast.makeText(this, "Showing Batman drawing...", Toast.LENGTH_SHORT).show()
        // TODO: Implement drawing display
    }

    private fun showSavedNote() {
        Toast.makeText(this, "Najprościej rozbroić śmiechem u ludzi emocjonalną broń...", Toast.LENGTH_SHORT).show()
        // TODO: Implement note display
    }

    private fun showSavedPhoto() {
        Toast.makeText(this, "Showing sofa photo...", Toast.LENGTH_SHORT).show()
        // TODO: Implement photo display
    }
}