package pl.hexmind.mindshaper.activities.carousel

import android.content.Intent
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.ImageView
import android.widget.TextView
import androidx.viewpager2.widget.ViewPager2
import dagger.hilt.android.AndroidEntryPoint
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.activities.main.CoreActivity
import pl.hexmind.mindshaper.activities.main.MainActivity
import pl.hexmind.mindshaper.services.ThoughtsService
import pl.hexmind.mindshaper.ui.carousel.ThoughtCarouselAdapter
import javax.inject.Inject
import kotlin.math.abs

/**
 * Activity for browsing thoughts in an elegant carousel format with 3D animations
 */
@AndroidEntryPoint
class ThoughtCarouselActivity : CoreActivity(), GestureDetector.OnGestureListener {

    @Inject
    lateinit var thoughtsService : ThoughtsService

    private lateinit var viewPager: ViewPager2
    private lateinit var adapter: ThoughtCarouselAdapter

    // Additional panel of the bottom
    private lateinit var ivPhaseInfoIcon : ImageView
    private lateinit var tvPhaseInfoHeader : TextView
    private lateinit var tvPhaseInfoThoughtsCaptured : TextView
    private lateinit var tvPhaseInfoNextPhase : TextView

    private lateinit var gestureDetector: GestureDetector

    internal val phaseToResourceMap = mapOf(
        ThoughtProcessingPhaseName.GATHERING to R.drawable.ic_phase_gathering,
        ThoughtProcessingPhaseName.CHOOSING to R.drawable.ic_phase_choosing,
        ThoughtProcessingPhaseName.SILENT to R.drawable.ic_phase_silent,
    )
    internal val phaseToHeaderStringMap = mapOf(
        ThoughtProcessingPhaseName.GATHERING to R.string.common_phase1_default_name,
        ThoughtProcessingPhaseName.CHOOSING to R.string.common_phase2_default_name,
        ThoughtProcessingPhaseName.SILENT to R.string.common_phase3_default_name,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_thought_carousel)

        initializeViews()
        setupCarousel()
        setupReactiveDataObserver()
        setupGestureDetector()
    }

    /**
     * Initialize all UI components
     */
    private fun initializeViews() {
        viewPager = findViewById(R.id.vp_thoughts)
        tvPhaseInfoThoughtsCaptured = findViewById(R.id.tv_phase_info_gathered_thoughts)
        setupPhasePanel()
    }

    private fun setupPhasePanel(){
        val currentPhase : ThoughtProcessingPhase = getCurrentPhase()

        ivPhaseInfoIcon = findViewById(R.id.iv_phase_info_icon)
        val resourceIconId = phaseToResourceMap[currentPhase.currentPhaseName] ?: -1
        ivPhaseInfoIcon.setImageResource(resourceIconId)

        tvPhaseInfoHeader = findViewById(R.id.tv_phase_info_header)
        val phaseDisplayName : String = getString(phaseToHeaderStringMap[currentPhase.currentPhaseName] ?: -1)
        tvPhaseInfoHeader.text = phaseDisplayName

        // Next phase launch
        tvPhaseInfoNextPhase = findViewById(R.id.tv_phase_info_next_phase)
        val phaseNextDisplayName : String = getString(phaseToHeaderStringMap[currentPhase.getNextPhaseName()] ?: -1)
        val resourceNextPhaseId = getString(R.string.carousel_phase_next_state_for, phaseNextDisplayName, currentPhase.minRemaining.toString())
        tvPhaseInfoNextPhase.text = resourceNextPhaseId
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
     * Setup reactive data observer that automatically updates UI when database changes
     */
    private fun setupReactiveDataObserver() {
        // Observe LiveData - automatically updates when database changes (add/edit/delete)
        thoughtsService.getAllThoughts().observe(this) { thoughtsDTO ->
            // Submit to adapter - will automatically animate changes with DiffUtil
            adapter.submitList(thoughtsDTO)
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
                // Swipe DOWN
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                return true
            } else if (diffY < -100) {
                // Swipe UP
                return true
            }
        }

        return false
    }
}