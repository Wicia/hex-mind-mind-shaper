package pl.hexmind.mindshaper.activities.details

import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.view.View
import android.widget.TextView
import android.widget.Toast
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.activities.CoreActivity
import pl.hexmind.mindshaper.services.dto.ThoughtDTO

class ThoughtDetailsActivity : CoreActivity() {

    private lateinit var tvEssence: TextView

    private lateinit var tvThread: TextView
    private lateinit var btnThreadPlaceholder: Button

    private lateinit var btnRecordingPlaceholder: Button

    private lateinit var btnDrawingPlaceholder: Button

    private lateinit var btnRichTextPlaceholder: Button

    private lateinit var btnPhotoPlaceholder: Button

    private lateinit var tvRichText : TextView

    // Displayed details data
    private var dtoWithDetails : ThoughtDTO? = null

    companion object PARAMS {
        const val P_SELECTED_THOUGHT_ID = "P_SELECTED_THOUGHT_ID"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_thought_details)

        saveExtrasFromIntent()
        initializeViews()
        fillWithDetails()
    }

    private fun initializeViews() {
        // Header/Title
        tvEssence = findViewById(R.id.tv_essence)

        // Extras/metadata
        btnThreadPlaceholder = findViewById(R.id.btn_thread_placeholder)
        tvThread = findViewById(R.id.tv_thread)

        // "Main" fields
        btnRecordingPlaceholder = findViewById(R.id.btn_recording_placeholder)

        btnDrawingPlaceholder = findViewById(R.id.btn_drawing_placeholder)

        btnRichTextPlaceholder = findViewById(R.id.btn_rich_text_placeholder)
        tvRichText = findViewById(R.id.tv_rich_text)

        btnPhotoPlaceholder = findViewById(R.id.btn_photo_placeholder)
    }

    private fun saveExtrasFromIntent() {
        dtoWithDetails = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(P_SELECTED_THOUGHT_ID, ThoughtDTO::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(P_SELECTED_THOUGHT_ID) as? ThoughtDTO
        }
    }

    private fun fillWithDetails(){
        tvEssence.text = dtoWithDetails?.essence

        if(dtoWithDetails?.richText.isNullOrBlank()){
            btnRichTextPlaceholder.visibility = View.VISIBLE
            tvRichText.visibility = View.GONE
        }
        else{
            btnRichTextPlaceholder.visibility = View.GONE
            tvRichText.visibility = View.VISIBLE
            tvRichText.text = dtoWithDetails?.richText
        }

        if(dtoWithDetails?.thread.isNullOrBlank()){
            btnThreadPlaceholder.visibility = View.VISIBLE
            tvThread.visibility = View.GONE
        }
        else{
            btnThreadPlaceholder.visibility = View.GONE
            tvThread.visibility = View.VISIBLE
            tvThread.text = dtoWithDetails?.thread
        }
    }
}