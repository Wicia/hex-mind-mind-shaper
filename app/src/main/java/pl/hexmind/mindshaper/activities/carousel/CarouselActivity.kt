package pl.hexmind.mindshaper.activities.carousel

import android.content.Intent
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.activities.CoreActivity
import pl.hexmind.mindshaper.activities.home.HomeActivity
import pl.hexmind.mindshaper.services.ThoughtsService
import pl.hexmind.mindshaper.services.dto.ThoughtDTO
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.abs

/**
 * Activity for browsing thoughts in an elegant carousel format with 3D animations
 */
@AndroidEntryPoint
class CarouselActivity : CoreActivity(), GestureDetector.OnGestureListener {

    @Inject
    lateinit var thoughtsService : ThoughtsService

    private lateinit var viewPager: ViewPager2
    private lateinit var adapter: CarouselAdapter

    private lateinit var gestureDetector: GestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.carousel_activity)

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
    }

    /**
     * Setup carousel with custom page transformer for 3D effect
     */
    private fun setupCarousel() {
        adapter = CarouselAdapter { thoughtToDelete ->
            deleteThought(thoughtToDelete)
        }
        viewPager.adapter = adapter
        viewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        viewPager.offscreenPageLimit = 3

        // Custom page transformer for 3D carousel effect
        viewPager.setPageTransformer { page, position -> ThoughtCardPageTransformer() }

        // Smooth page change callback
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
        })
    }

    private fun deleteThought(thought: ThoughtDTO) {
        lifecycleScope.launch {
            Timber.d("Deleting thought: ${thought.id}")
            thoughtsService.deleteThought(thought)
            // Optional: Show toast confirmation
            showShortToast(R.string.common_deletion_dialog_confirmation, "Myśl")
        }
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
                val intent = Intent(this, HomeActivity::class.java)
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