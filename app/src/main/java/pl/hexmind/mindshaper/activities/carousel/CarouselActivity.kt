package pl.hexmind.mindshaper.activities.carousel

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.activity.viewModels
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.activities.CoreActivity
import pl.hexmind.mindshaper.activities.home.HomeActivity
import pl.hexmind.mindshaper.common.regex.HexTagsUtils
import pl.hexmind.mindshaper.services.dto.ThoughtDTO
import kotlin.math.abs

/**
 * Activity for browsing thoughts in an elegant carousel format with 3D animations and search
 */
@AndroidEntryPoint
class CarouselActivity : CoreActivity(), GestureDetector.OnGestureListener {

    private val viewModel: CarouselViewModel by viewModels()

    private lateinit var viewPager: ViewPager2
    private lateinit var adapter: CarouselAdapter
    private lateinit var gestureDetector: GestureDetector

    // Search UI components
    private lateinit var tilSearch: TextInputLayout
    private lateinit var etSearch: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.carousel_activity)

        initializeViews()
        setupCarousel()
        setupSearchBar()
        setupReactiveDataObserver()
        setupGestureDetector()

        viewModel.clearSearch()
    }

    private fun initializeViews() {
        viewPager = findViewById(R.id.vp_thoughts)
        tilSearch = findViewById(R.id.til_search)
        etSearch = findViewById(R.id.et_search)
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

    /**
     * Setup real-time search bar with TextWatcher
     */
    private fun setupSearchBar() {
        // ! TextWatcher for real-time search
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val hexTags = HexTagsUtils.parseInput(s?.toString() ?: "")
                // Update search query in ViewModel on every text change
                viewModel.updateSearchQuery(hexTags)
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Listener for X icon
        tilSearch.setEndIconOnClickListener {
            etSearch.text?.clear()
            viewModel.clearSearch()
        }
    }

    private fun deleteThought(thought: ThoughtDTO) {
        viewModel.deleteThought(thought)
        showShortToast(R.string.common_deletion_dialog_confirmation, "Myśl")
    }

    /**
     * Setup reactive data observer that automatically updates UI when database changes
     */
    private fun setupReactiveDataObserver() {
        viewModel.filteredThoughts.observe(this) { thoughtsDTO ->
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