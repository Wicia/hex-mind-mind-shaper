package pl.hexmind.mindshaper.activities.carousel

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.activity.viewModels
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.activities.CoreActivity
import pl.hexmind.mindshaper.activities.details.DetailsActivity
import pl.hexmind.mindshaper.activities.home.HomeActivity
import pl.hexmind.mindshaper.common.SortConfig
import pl.hexmind.mindshaper.common.regex.HexTagsUtils
import pl.hexmind.mindshaper.services.dto.ThoughtDTO
import timber.log.Timber
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
    private lateinit var btnSort: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.carousel_activity)

        initializeViews()
        setupCarousel()
        setupRealTimeSearchBar()
        setupSortButton()
        setupReactiveDataObserver()
        setupGestureDetector()
    }

    private fun initializeViews() {
        viewPager = findViewById(R.id.vp_thoughts)
        tilSearch = findViewById(R.id.til_search)
        etSearch = findViewById(R.id.et_search)
        btnSort = findViewById(R.id.btn_sort)
        setupHeader(R.drawable.ic_header_carousel, R.string.thoughts_carousel_title)
    }

    private fun setupCarousel() {
        adapter = CarouselAdapter(
            onDeleteThought = { thoughtToDelete ->
                showDeleteConfirmationDialog(thoughtToDelete)
            },
            onThoughtTap = { thoughtTap ->
                val intent = Intent(this, DetailsActivity::class.java)
                intent.putExtra(DetailsActivity.P_SELECTED_THOUGHT_ID, thoughtTap.id ?: -1)
                startActivity(intent)
            },
            onLoadAudio = { thoughtId, onReady ->
                viewModel.loadAudioForPlayback(thoughtId, onReady)
            }
        )

        viewPager.adapter = adapter
        viewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        viewPager.offscreenPageLimit = 3

        // Custom page transformer for 3D carousel effect
        viewPager.setPageTransformer { _, _ -> ThoughtCardPageTransformer() }

        // Smooth page change callback
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
        })
    }

    private fun setupRealTimeSearchBar() {
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

    private fun setupSortButton() {
        btnSort.setOnClickListener {
            showSortDialog()
        }
    }

    private fun showSortDialog() {
        val currentConfig = viewModel.sortConfig.value ?: SortConfig()

        val dialog = SortDialogFragment(currentConfig) { newConfig ->
            viewModel.updateSortConfig(newConfig)
        }

        dialog.show(supportFragmentManager, SortDialogFragment.TAG)
    }

    private fun showDeleteConfirmationDialog(thought: ThoughtDTO) {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.common_deletion_dialog_title))
            .setMessage(getString(R.string.common_deletion_dialog_message, "myśl"))
            .setPositiveButton(getString(R.string.common_deletion_dialog_yes)) { dialog, _ ->
                deleteThought(thought)
                Timber.d("Thought deleted: ${thought.id}")
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.common_deletion_dialog_no)) { dialog, _ ->
                Timber.d("Deletion cancelled")
                dialog.dismiss()
            }
            .show()
    }

    private fun deleteThought(thought: ThoughtDTO) {
        viewModel.deleteThought(thought)
        showShortToast(
            R.string.common_deletion_dialog_confirmation,
            this.getString(R.string.common_object_type_thought)
        )
    }

    /**
     * Setup reactive data observer that automatically updates UI when database changes
     */
    private fun setupReactiveDataObserver() {
        viewModel.filteredThoughts.observe(this) { thoughtsDTO ->
            // Submit to adapter - will automatically animate changes with DiffUtil
            adapter.submitList(thoughtsDTO)
        }

        viewModel.sortConfig.observe(this) { sortConfig ->
            performListRefresh()
            btnSort.text = getString(sortConfig.property.displayNameRes)
                .plus(": ")
                .plus(getString(sortConfig.direction.getLabelResByFieldType(sortConfig.property.type)))
        }
    }

    /**
     * Animate ViewPager refresh with fade and scale effect
     */
    private fun performListRefresh() {
        // Fade out and scale down
        viewPager.animate()
            .alpha(0.3f)
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(150)
            .withEndAction {
                // Reset to first item
                viewPager.setCurrentItem(0, false)

                // Fade in and scale up
                viewPager.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .start()
            }
            .start()
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