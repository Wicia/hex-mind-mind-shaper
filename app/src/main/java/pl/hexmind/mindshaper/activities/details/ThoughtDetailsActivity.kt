package pl.hexmind.mindshaper.activities.details

import android.os.Build
import android.os.Bundle
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.activities.CoreActivity
import pl.hexmind.mindshaper.databinding.ActivityThoughtDetailsBinding
import pl.hexmind.mindshaper.services.dto.ThoughtDTO
import timber.log.Timber

class ThoughtDetailsActivity : CoreActivity() {

    private lateinit var binding: ActivityThoughtDetailsBinding

    // Displayed details data
    private var dtoWithDetails : ThoughtDTO? = null

    companion object PARAMS {
        const val P_SELECTED_THOUGHT_ID = "P_SELECTED_THOUGHT_ID"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityThoughtDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        saveExtrasFromIntent()
        setupViewPager()
        setupTabLayout()
    }

    private fun saveExtrasFromIntent() {
        dtoWithDetails = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(P_SELECTED_THOUGHT_ID, ThoughtDTO::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(P_SELECTED_THOUGHT_ID) as? ThoughtDTO
        }
    }

    /**
     * Initializes ViewPager2 with content pages adapter
     */
    private fun setupViewPager() {
        binding.viewPager.adapter = ContentPagerAdapter()
    }

    /**
     * Configures TabLayout with custom bubble indicator and icons
     */
    private fun setupTabLayout() {
        val tabIcons = listOf(
            R.drawable.tab_icon_rich_text_selector,
            R.drawable.tab_icon_drawing_selector,
            R.drawable.tab_icon_recording_selector,
            R.drawable.tab_icon_photo_selector
        )

        // Connect TabLayout with ViewPager2
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.setIcon(tabIcons[position])
        }.attach()

        // TODO: Apply custom bubble indicator styling
        applyBubbleIndicator()
    }

    /**
     * Applies custom bubble indicator with 10% size increase
     */
    private fun applyBubbleIndicator() {
        val tabLayout = binding.tabLayout

        for (i in 0 until tabLayout.tabCount) {
            val tab = tabLayout.getTabAt(i)
            tab?.view?.apply {
                // Scale up selected tab by 10%
                scaleX = if (i == 0) 1.1f else 1.0f
                scaleY = if (i == 0) 1.1f else 1.0f
            }
        }

        // Listen for tab selection to animate bubble
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.view?.apply {
                    animate()
                        .scaleX(1.1f)
                        .scaleY(1.1f)
                        .setDuration(200)
                        .start()
                }
                Timber.d("Tab selected: ${tab?.position}")
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                tab?.view?.apply {
                    animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(200)
                        .start()
                }
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                // Do nothing on reselect
            }
        })
    }
}