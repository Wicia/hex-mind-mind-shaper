package pl.hexmind.fastnote.features.carousel

import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import pl.hexmind.fastnote.R
import kotlin.math.abs

/**
 * Activity for browsing thoughts in an elegant carousel format with 3D animations
 */
class ThoughtCarouselActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var btnPrevious: MaterialButton
    private lateinit var btnNext: MaterialButton
    private lateinit var adapter: ThoughtCarouselAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_thought_carousel)

        initializeViews()
        setupCarousel()
        setupButtons()
        loadThoughts()
    }

    /**
     * Initialize all UI components
     */
    private fun initializeViews() {
        viewPager = findViewById(R.id.viewPager_thoughts)
        btnPrevious = findViewById(R.id.btn_previous)
        btnNext = findViewById(R.id.btn_next)
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
                updateNavigationButtons(position)
            }
        })
    }

    /**
     * Setup navigation buttons with smooth animations
     */
    private fun setupButtons() {
        btnPrevious.setOnClickListener {
            val currentItem = viewPager.currentItem
            if (currentItem > 0) {
                viewPager.setCurrentItem(currentItem - 1, true)
            }
        }

        btnNext.setOnClickListener {
            val currentItem = viewPager.currentItem
            if (currentItem < adapter.itemCount - 1) {
                viewPager.setCurrentItem(currentItem + 1, true)
            }
        }
    }

    /**
     * Update navigation buttons visibility based on current position
     */
    private fun updateNavigationButtons(position: Int) {
        btnPrevious.animate()
            .alpha(if (position == 0) 0.3f else 1f)
            .setDuration(300)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        btnNext.animate()
            .alpha(if (position == adapter.itemCount - 1) 0.3f else 1f)
            .setDuration(300)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
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
        updateNavigationButtons(0)
    }
}