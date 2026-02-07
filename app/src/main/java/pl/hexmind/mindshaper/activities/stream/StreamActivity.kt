package pl.hexmind.mindshaper.activities.stream

import android.content.Intent
import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.activity.viewModels
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.activities.CoreActivity
import pl.hexmind.mindshaper.activities.details.DetailsActivity
import pl.hexmind.mindshaper.common.SortConfig
import pl.hexmind.mindshaper.common.onboarding.OnboardingProgressStep
import pl.hexmind.mindshaper.common.ui.CommonIconsListItem
import pl.hexmind.mindshaper.common.ui.dialogs.IconsListDialog
import pl.hexmind.mindshaper.services.dto.ThoughtDTO
import pl.hexmind.mindshaper.services.validators.ThoughtValidator
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.cos
import kotlin.math.sin

/**
 * Activity for browsing thoughts in a vertical feed (like Instagram/TikTok stories)
 */
@AndroidEntryPoint
class StreamActivity : CoreActivity() {

    @Inject
    lateinit var thoughtValidator: ThoughtValidator

    private val viewModel: StreamViewModel by viewModels()

    private lateinit var viewPager: ViewPager2
    private lateinit var adapter: StreamAdapter

    private lateinit var btnSort: MaterialButton
    private lateinit var btnFilter: MaterialButton

    // FAB menu
    private lateinit var fabNewThought: FloatingActionButton
    private lateinit var fabNewThoughtRichText: FloatingActionButton
    private lateinit var fabNewThoughtRecording: FloatingActionButton
    private var isMenuOpen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.stream_activity)

        initializeViews()
        setupVerticalFeed()
        setupSortButton()
        setupFilterButton()
        setupFabMenu()
        setupReactiveDataObserver()

        viewModel.loadDomains()

        onboardingManager.showTooltipForStep(
            OnboardingProgressStep.STREAM_TOOLTIP, this
        )
    }

    private fun initializeViews() {
        viewPager = findViewById(R.id.vp_thoughts)
        btnSort = findViewById(R.id.btn_sort)
        btnFilter = findViewById(R.id.btn_filter)

        fabNewThought = findViewById(R.id.fab_new_thought)
        fabNewThoughtRichText = findViewById(R.id.fab_rich_text_type)
        fabNewThoughtRecording = findViewById(R.id.fab_voice_type)

        setupHeader(R.drawable.ic_header_stream, R.string.thoughts_stream_title)
    }

    private fun setupVerticalFeed() {
        adapter = StreamAdapter(
            thoughtValidator,
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
            },
            onLoadPhoto = { thoughtId, onReady ->
                viewModel.loadPhotoForDisplay(thoughtId, onReady)
            }
        )

        viewPager.adapter = adapter
        viewPager.orientation = ViewPager2.ORIENTATION_VERTICAL // VERTICAL feed!
        viewPager.offscreenPageLimit = 3

        // Smooth page change callback
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
        })
    }

    private fun setupSortButton() {
        btnSort.setOnClickListener {
            showSortDialog()
        }
    }

    private fun setupFilterButton() {
        btnFilter.setOnClickListener {
            showFilterDialog()
        }

        // Update filter button text when domain changes
        viewModel.selectedDomainId.observe(this) { domainId ->
            updateFilterButtonText(domainId)
        }
    }

    private fun setupFabMenu() {
        // Initially hide all menu buttons
        listOf(fabNewThoughtRichText, fabNewThoughtRecording).forEach { fab ->
            fab.hide()
            fab.alpha = 0f
        }

        fabNewThought.setOnClickListener {
            toggleMenu()
        }

        // For now, FABs don't open anything - just placeholder
        fabNewThoughtRichText.setOnClickListener {
            closeMenu()
            // TODO: Open CaptureActivity with RICH_TEXT type
            showShortToast(R.string.common_placeholder_accessibility_content_desc, "Rich Text")
        }

        fabNewThoughtRecording.setOnClickListener {
            closeMenu()
            // TODO: Open CaptureActivity with RECORDING type
            showShortToast(R.string.common_placeholder_accessibility_content_desc, "Recording")
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
        fabNewThought.animate()
            .rotation(45f)
            .setDuration(300)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        // Show and animate menu buttons - fan out upward and left
        val fabs = listOf(fabNewThoughtRichText, fabNewThoughtRecording)

        // Angles: 90° (straight up), 135° (up-left)
        val angles = listOf(90.0, 135.0)
        val radius = 200f

        fabs.forEachIndexed { index, fab ->
            fab.show()

            val angleRad = Math.toRadians(angles[index])
            val x = (radius * cos(angleRad)).toFloat()
            val y = (radius * sin(angleRad)).toFloat()

            fab.animate()
                .translationX(x)
                .translationY(-y) // Negative Y because Android coordinates grow downward
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
        fabNewThought.animate()
            .rotation(0f)
            .setDuration(300)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        // Hide menu buttons
        listOf(fabNewThoughtRichText, fabNewThoughtRecording).forEach { fab ->
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

    private fun showFilterDialog() {
        val domains = viewModel.domainsWithIcons.value ?: emptyList()
        if (domains.isEmpty()) return

        // Add "All domains" option at the beginning
        val allDomainsOption = CommonIconsListItem(
            iconEntityId = null,
            iconResourceId = R.drawable.ic_domain_none,
            labelText = getString(R.string.stream_filter_all_domains),
            labelEntityId = null,
            highlightItem = true
        )

        val domainsWithAll = listOf(allDomainsOption) + domains

        IconsListDialog.Builder(this)
            .setTitle(getString(R.string.stream_filter_dialog_title))
            .setIcons(domainsWithAll)
            .setOnIconSelected { selectedDomain ->
                onDomainFilterSelected(selectedDomain)
            }
            .show()
    }

    private fun onDomainFilterSelected(domain: CommonIconsListItem) {
        if (domain.labelEntityId == null) {
            // "All domains" selected - clear filter
            viewModel.clearDomainFilter()
        } else {
            // Specific domain selected
            viewModel.updateSelectedDomain(domain.labelEntityId)
        }
    }

    private fun updateFilterButtonText(domainId: Int?) {
        btnFilter.text = if (domainId == null) {
            getString(R.string.stream_filter_button_none)
        } else {
            "(1)" // TODO: to be replaced with filter counter
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
            adapter.updateSortConfig(sortConfig)
            performListRefresh()
            updateSortButtonLabel(sortConfig)
        }
    }

    private fun updateSortButtonLabel(sortConfig : SortConfig){
        val sortProperty = sortConfig.property
        val labelText = getString(sortProperty.displayNameRes)
            .plus(": ")
            .plus(getString(R.string.sort_info_prefix)).plus(" ")
            .plus(getString(sortConfig.direction.getLabelResByFieldType(sortProperty.type)))

        btnSort.text = labelText
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
}