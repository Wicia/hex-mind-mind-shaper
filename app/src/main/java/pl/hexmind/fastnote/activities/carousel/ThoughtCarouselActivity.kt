package pl.hexmind.fastnote.activities.carousel

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.viewpager2.widget.ViewPager2
import pl.hexmind.fastnote.R
import pl.hexmind.fastnote.activities.main.CoreActivity
import kotlin.math.abs

/**
 * Activity for browsing thoughts in an elegant carousel format with 3D animations
 */
class ThoughtCarouselActivity : CoreActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var adapter: ThoughtCarouselAdapter

    // Additional panel of the bottom
    private lateinit var iv_phase_info_icon : ImageView
    private lateinit var tv_phase_info_header : TextView
    private lateinit var tv_phase_info_thoughts_captured : TextView
    private lateinit var tv_phase_info_next_phase : TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_thought_carousel)

        initializeViews()
        setupCarousel()
        loadThoughts()
    }

    /**
     * Initialize all UI components
     */
    private fun initializeViews() {
        viewPager = findViewById(R.id.viewPager_thoughts)
        tv_phase_info_thoughts_captured = findViewById(R.id.tv_phase_info_gathered_thoughts)
        setupPhasePanel()
    }

    private fun setupPhasePanel(){
        val currentPhase : ThoughtProcessingPhase = getCurrentPhase()

        iv_phase_info_icon = findViewById(R.id.iv_phase_info_icon)
        val resourceIconId = phaseToResourceMap[currentPhase.currentPhaseName] ?: -1
        iv_phase_info_icon.setImageResource(resourceIconId)

        tv_phase_info_header = findViewById(R.id.tv_phase_info_header)
        val phaseDisplayName : String = getString(phaseToHeaderStringMap[currentPhase.currentPhaseName] ?: -1)
        tv_phase_info_header.text = phaseDisplayName

        // Next phase launch
        tv_phase_info_next_phase = findViewById(R.id.tv_phase_info_next_phase)

        val phaseNextDisplayName : String = getString(phaseToHeaderStringMap[currentPhase.getNextPhaseName()] ?: -1)
        val resourceNextPhaseId = getString(R.string.carousel_phase_next_state_for, phaseNextDisplayName, currentPhase.minRemaining.toString())
        tv_phase_info_next_phase.text = resourceNextPhaseId
    }

    /**
     * Setup carousel with custom page transformer for 3D effect
     */
    private fun setupCarousel() {
        adapter = ThoughtCarouselAdapter()
        viewPager.adapter = adapter
        viewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        viewPager.offscreenPageLimit = 3

        // Custom page transformer for 3D carousel effect
        viewPager.setPageTransformer { page, position ->
            when {
                position < -1 -> {
                    page.alpha = 0f
                }
                position <= 1 -> {
                    page.alpha = 1f
                    page.translationX = 0f
                    page.scaleX = 1f
                    page.scaleY = 1f

                    // 3D rotation effect
                    page.rotationY = position * -30f

                    // Depth effect
                    val scaleFactor = 0.85f + (1 - abs(position)) * 0.15f
                    page.scaleX = scaleFactor
                    page.scaleY = scaleFactor

                    // Fade effect for side pages
                    page.alpha = 0.5f + (1 - abs(position)) * 0.5f
                }
                else -> {
                    page.alpha = 0f
                }
            }
        }

        // Smooth page change callback
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
            }
        })
    }

    /**
     * Load thoughts data into carousel
     */
    private fun loadThoughts() {
        // TODO: Replace with actual data loading from database
        val sampleThoughts = listOf(
            ThoughtItem(tags = "Pierwsza myśl", content = "To jest przykładowa treść pierwszej myśli"),
            ThoughtItem(tags ="Druga myśl", content = "Kolejna interesująca myśl do zapamiętania"),
            ThoughtItem(tags ="Trzecia myśl", content = "Jeszcze jedna ważna myśl")
        )

        adapter.submitList(sampleThoughts)
    }
}