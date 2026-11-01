package pl.hexmind.mindshaper.activities.metadata

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.view.children
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.activities.CoreActivity
import pl.hexmind.mindshaper.activities.stream.StreamActivity
import pl.hexmind.mindshaper.common.onboarding.OnboardingTask
import pl.hexmind.mindshaper.common.onboarding.OnboardingTaskGuide
import pl.hexmind.mindshaper.common.onboarding.OnboardingTaskPoint
import pl.hexmind.mindshaper.common.ui.dialogs.ActionsDialog
import pl.hexmind.mindshaper.common.regex.HexTags
import pl.hexmind.mindshaper.common.ui.views.HexSpotlight
import pl.hexmind.mindshaper.common.ui.views.HexTabSwitch
import pl.hexmind.mindshaper.common.ui.views.IconsGridItem
import pl.hexmind.mindshaper.database.models.HexTagType
import pl.hexmind.mindshaper.services.DomainIconsService
import pl.hexmind.mindshaper.services.DomainsService
import pl.hexmind.mindshaper.services.RenameOutcome
import pl.hexmind.mindshaper.services.dto.DomainDTO
import pl.hexmind.mindshaper.services.validators.DomainValidator
import javax.inject.Inject

@AndroidEntryPoint
class MetadataActivity : CoreActivity() {

    private val viewModel: MetadataViewModel by viewModels()

    private lateinit var rvTags: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var tabsTagType: HexTabSwitch

    // Tag task: the search guide waits for the projects list the previous step switched to
    private var isSearchGuidePending = false
    private lateinit var glDomains: GridLayout

    @Inject
    lateinit var domainService : DomainsService

    @Inject
    lateinit var domainIconsService : DomainIconsService

    @Inject
    lateinit var domainValidator: DomainValidator

    private val tagsAdapter = MetadataTagRowAdapter(
        onTagTap = { tagName -> showTagEditSheet(tagName) },
        onTagLongPress = { tagName -> showTagDeleteDialog(tagName) },
        onTagSearch = { tagName -> openStreamSearchedBy(tagName) }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.metadata_activity)

        // Nav overlay + current-screen highlight are added by CoreActivity (onContentChanged / onResume)
        setupHeader(R.drawable.ic_activity_metadata, R.string.metadata_title)

        rvTags = findViewById(R.id.rv_tags)
        tvEmpty = findViewById(R.id.tv_tags_empty)
        tabsTagType = findViewById(R.id.tabs_tag_type)

        rvTags.layoutManager = LinearLayoutManager(this)
        rvTags.adapter = tagsAdapter

        glDomains = findViewById(R.id.gl_domains)

        tabsTagType.setTabs(
            listOf(
                getString(R.string.metadata_tab_persons),
                getString(R.string.metadata_tab_projects),
                getString(R.string.metadata_tab_domains)
            )
        )
        tabsTagType.onTabSelected = { tabIndex ->
            when (tabIndex) {
                TAB_PERSONS  -> selectTagType(HexTagType.PERSON)
                TAB_PROJECTS -> selectTagType(HexTagType.PROJECT)
                else         -> selectDomains()
            }
        }

        observeViewModel()
    }

    // Reloaded here, not in onCreate: the nav reuses this instance (SINGLE_TOP), so a tag added
    // elsewhere would otherwise leave a stale list on return
    override fun onResume() {
        super.onResume()
        if (viewModel.domainsSelected) {
            selectDomains()
        }
        else {
            selectTagType(viewModel.currentType)
        }
    }

    private fun observeViewModel() {
        viewModel.tagRows.observe(this) { rows ->
            tagsAdapter.submitRows(rows)

            // ! A tags load may land after a switch to Domains - it must not show over the grid
            if (viewModel.domainsSelected) return@observe

            val hasTags = rows.isNotEmpty()
            rvTags.visibility = if (hasTags) View.VISIBLE else View.GONE
            tvEmpty.visibility = if (hasTags) View.GONE else View.VISIBLE

            // ! Once the list laid out the new rows - the guide ends on a pill
            rvTags.post { showTagTaskGuide() }
            if (isSearchGuidePending && viewModel.currentType == HexTagType.PROJECT) {
                isSearchGuidePending = false
                // ! After the previous spotlight faded out - two dims at once would flash darker
                rvTags.postDelayed({ showTagSearchGuide() }, HexSpotlight.FADE_DURATION_MS)
            }
        }

        viewModel.renameResult.observe(this) { outcome ->
            if (outcome == null) {
                return@observe
            }

            // Renaming onto an existing tag is blocked - combining the two is the drag & drop merge
            // feature parked in the TODO
            if (outcome == RenameOutcome.NAME_TAKEN) {
                showShortToast(R.string.metadata_tag_edit_exists)
            }

            viewModel.onRenameResultShown()
        }
    }

    private fun selectTagType(tagType: HexTagType) {
        tabsTagType.setSelectedIndex(if (tagType == HexTagType.PERSON) TAB_PERSONS else TAB_PROJECTS)
        glDomains.visibility = View.GONE

        // Set before load so the empty state the observer may reveal already matches the type
        val emptyText = if (tagType == HexTagType.PERSON) {
            R.string.metadata_persons_empty
        }
        else {
            R.string.metadata_projects_empty
        }
        tvEmpty.setText(emptyText)

        viewModel.loadTags(tagType)
    }

    private fun showTagDeleteDialog(tagName: String) {
        ActionsDialog.Builder(this)
            .setTitle(getString(R.string.metadata_tag_delete_title))
            .setDescription(getString(R.string.metadata_tag_delete_message, tagName))
            .setPrimaryAction(getString(R.string.metadata_tag_delete_yes), caution = true) {
                viewModel.deleteTag(tagName)
                showShortToast(R.string.metadata_tag_delete_confirmation, tagName)
            }
            .show()
    }

    // ! No CLEAR_TOP, unlike the nav bar - Stream lands on top of this screen, so Back returns here
    private fun openStreamSearchedBy(tagName: String) {
        // Before Stream opens - a run done here gets congratulated there
        onboardingManager.recordTaskPoints(OnboardingTask.TAG_THOUGHT, setOf(OnboardingTaskPoint.TAG_SEARCH))

        val criteria = if (viewModel.currentType == HexTagType.PERSON) {
            HexTags(person = tagName)
        }
        else {
            HexTags(project = tagName)
        }

        val intent = Intent(this, StreamActivity::class.java)
        intent.putExtra(StreamActivity.P_SEARCH_CRITERIA, criteria)
        startActivity(intent)
    }

    private fun showTagEditSheet(tagName: String) {
        HexTagEditBottomSheet.show(supportFragmentManager, tagName) { newName ->
            viewModel.renameTag(tagName, newName)
        }
    }

    // ========== TAG THOUGHT TASK ==========

    // Three steps, once per run: the Domains tab (info), Persons and Projects tabs (on to Projects),
    // then the search part of the first project pill
    private fun showTagTaskGuide() {
        val task = OnboardingTask.TAG_THOUGHT
        if (isFinishing || isDestroyed || viewModel.domainsSelected) return
        if (!onboardingManager.shouldShowTaskGuide(task, OnboardingTaskGuide.METADATA_SEARCH)) return

        onboardingManager.markTaskGuideShown(task, OnboardingTaskGuide.METADATA_SEARCH)
        HexSpotlight.show(
            activity   = this,
            target     = tabsTagType,
            targetArea = tabsTagType.tabsBounds(TAB_DOMAINS, TAB_DOMAINS),
            text       = getString(R.string.metadata_onboarding_task_domains),
            buttonText = getString(R.string.common_spotlight_btn_next),
            step       = 1,
            stepCount  = TAG_TASK_GUIDE_STEPS,
            holeShape  = HexSpotlight.HoleShape.ROUNDED_RECT,
            // ! Next one after this faded out - two dims at once would flash darker
            onClosed   = { window.decorView.postDelayed({ showProjectsTabGuide() }, HexSpotlight.FADE_DURATION_MS) }
        ) {}
    }

    private fun showProjectsTabGuide() {
        if (isFinishing || isDestroyed || viewModel.domainsSelected) return

        HexSpotlight.show(
            activity   = this,
            target     = tabsTagType,
            targetArea = tabsTagType.tabsBounds(TAB_PERSONS, TAB_PROJECTS),
            text       = getString(R.string.metadata_onboarding_task_projects_tab),
            buttonText = getString(R.string.common_spotlight_btn_next),
            step       = 2,
            stepCount  = TAG_TASK_GUIDE_STEPS,
            holeShape  = HexSpotlight.HoleShape.ROUNDED_RECT,
            // Only the user's tap goes to Projects - in the hole or later on the tab itself (after the button or back)
            // The search guide comes with the projects list
            onClosed   = {
                if (viewModel.currentType == HexTagType.PROJECT) {
                    window.decorView.postDelayed({ showTagSearchGuide() }, HexSpotlight.FADE_DURATION_MS)
                }
                else {
                    isSearchGuidePending = true
                }
            }
        ) {
            selectTagType(HexTagType.PROJECT)
        }
    }

    // On the tag added in this run (the first one when there is none) - scrolled to when out of view
    private fun showTagSearchGuide(isScrolled: Boolean = false) {
        if (isFinishing || isDestroyed || viewModel.domainsSelected) return

        val tagName = onboardingManager.getRunTagName(OnboardingTask.TAG_THOUGHT)
        val rowIndex = viewModel.tagRows.value.orEmpty()
            .indexOfFirst { row -> row.any { tag -> tag.name.equals(tagName, ignoreCase = true) } }
            .coerceAtLeast(0)

        val rowView = rvTags.findViewHolderForAdapterPosition(rowIndex)?.itemView as? ViewGroup
        if (rowView == null) {
            if (!isScrolled) {
                rvTags.scrollToPosition(rowIndex)
                rvTags.post { showTagSearchGuide(isScrolled = true) }
            }
            return
        }

        val pill = rowView.children.firstOrNull { pill ->
            pill.findViewById<TextView>(R.id.tv_pill_name)?.text?.toString().equals(tagName, ignoreCase = true)
        } ?: rowView.getChildAt(0)
        val pillSearch = pill?.findViewById<View>(R.id.ll_pill_search) ?: return

        HexSpotlight.show(
            activity   = this,
            target     = pillSearch,
            text       = getString(R.string.metadata_onboarding_task_search),
            buttonText = getString(R.string.common_spotlight_btn_got_it),
            step       = 3,
            stepCount  = TAG_TASK_GUIDE_STEPS
        ) {
            pillSearch.performClick()
        }
    }

    // ========== DOMAINS ==========

    private fun selectDomains() {
        tabsTagType.setSelectedIndex(TAB_DOMAINS)
        viewModel.selectDomains()

        rvTags.visibility = View.GONE
        tvEmpty.visibility = View.GONE
        glDomains.visibility = View.VISIBLE

        loadDomainButtons()
    }

    private fun loadDomainButtons() {
        lifecycleScope.launch {
            val titles = domainService.getAllDomains()

            try {
                // Rebuilt on every visit - the tab is reopened on resume as well
                glDomains.removeAllViews()

                // Create buttons with loaded icons
                titles.forEachIndexed { domainIndex , domainDTO ->
                    val buttonView = layoutInflater.inflate(R.layout.metadata_domain_item, glDomains, false)

                    buttonView.findViewById<TextView>(R.id.tv_domain_name).text = domainDTO.name

                    val resourceId = domainIconsService.getIconResourceId(domainDTO.iconId)
                    buttonView.findViewById<ImageView>(R.id.iv_domain_icon).setImageResource(resourceId)

                    buttonView.setOnClickListener {
                        onDomainButtonClick(domainIndex, domainDTO)
                    }

                    glDomains.addView(buttonView)
                }
            }
            catch (e: Exception) {
                // TODO: add UI control + handle error using: R.string.settings_domains_loading_error))
            }
        }
    }

    private fun onDomainButtonClick(domainTileIndex: Int, currentDomainDTO : DomainDTO) {
        showDomainEditBottomSheet(currentDomainDTO) { updatedDTO ->
            updateDomainButton(domainTileIndex, updatedDTO)
            lifecycleScope.launch { domainService.updateDomain(dto = updatedDTO) }
        }
    }

    private fun showDomainEditBottomSheet(currentDomainDTO: DomainDTO, onDTOUpdated: (DomainDTO) -> Unit) {
        lifecycleScope.launch {
            try {
                val availableIconsIds = domainIconsService.getAvailableIconsIds()
                val iconsMap = domainIconsService.loadIconsBatch(availableIconsIds)

                // Convert to IconsGridItem list expected by IconsGridView
                val iconItems = availableIconsIds.mapNotNull { id ->
                    iconsMap[id]?.let { resId -> IconsGridItem(id = id, iconResId = resId) }
                }

                DomainEditBottomSheet.show(
                    fragmentManager = supportFragmentManager,
                    items = iconItems,
                    domainName = currentDomainDTO.name,
                    selectedIconId = currentDomainDTO.iconId,
                    validator = domainValidator
                ) { name, iconId ->
                    val updatedDTO = DomainDTO(id = currentDomainDTO.id, name = name, iconId = iconId)
                    onDTOUpdated(updatedDTO)
                }
            }
            catch (e: Exception) {
                // TODO: Handling exception is needed?
            }
        }
    }

    /**
     * Helper method to update button icon after selection
     */
    private fun updateDomainButton(buttonIndex: Int, updatedDomainDTO : DomainDTO) {
        lifecycleScope.launch {
            // Find the button in GridLayout and update its icon
            if (buttonIndex < glDomains.childCount) {
                val buttonView = glDomains.getChildAt(buttonIndex)

                val resourceId = domainIconsService.getIconResourceId(updatedDomainDTO.iconId)
                buttonView.findViewById<ImageView>(R.id.iv_domain_icon).setImageResource(resourceId)
                buttonView.findViewById<TextView>(R.id.tv_domain_name).text = updatedDomainDTO.name
            }
        }
    }

    private companion object {
        const val TAB_PERSONS  = 0
        const val TAB_PROJECTS = 1
        const val TAB_DOMAINS  = 2

        const val TAG_TASK_GUIDE_STEPS = 3
    }
}
