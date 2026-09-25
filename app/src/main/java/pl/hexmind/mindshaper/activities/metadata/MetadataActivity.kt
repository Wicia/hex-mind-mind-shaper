package pl.hexmind.mindshaper.activities.metadata

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.activities.CoreActivity
import pl.hexmind.mindshaper.common.ui.dialogs.ActionsDialog
import pl.hexmind.mindshaper.common.ui.views.HexTabSwitch
import pl.hexmind.mindshaper.database.models.HexTagType
import pl.hexmind.mindshaper.services.RenameOutcome

@AndroidEntryPoint
class MetadataActivity : CoreActivity() {

    private val viewModel: MetadataViewModel by viewModels()

    private lateinit var rvTags: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var tabsTagType: HexTabSwitch

    private val tagsAdapter = MetadataTagRowAdapter(
        onTagTap = { tagName -> showTagEditSheet(tagName) },
        onTagLongPress = { tagName -> showTagDeleteDialog(tagName) }
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

        tabsTagType.setTabs(
            listOf(
                getString(R.string.metadata_tab_persons),
                getString(R.string.metadata_tab_projects)
            )
        )
        tabsTagType.onTabSelected = { tabIndex ->
            selectTagType(if (tabIndex == 0) HexTagType.PERSON else HexTagType.PROJECT)
        }

        observeViewModel()
    }

    // Reloaded here, not in onCreate: the nav reuses this instance (SINGLE_TOP), so a tag added
    // elsewhere would otherwise leave a stale list on return
    override fun onResume() {
        super.onResume()
        selectTagType(viewModel.currentType)
    }

    private fun observeViewModel() {
        viewModel.tagRows.observe(this) { rows ->
            tagsAdapter.submitRows(rows)

            val hasTags = rows.isNotEmpty()
            rvTags.visibility = if (hasTags) View.VISIBLE else View.GONE
            tvEmpty.visibility = if (hasTags) View.GONE else View.VISIBLE
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
        tabsTagType.setSelectedIndex(if (tagType == HexTagType.PERSON) 0 else 1)

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
            .setTitle(getString(R.string.common_deletion_dialog_title))
            .setDescription(getString(R.string.metadata_tag_delete_message, tagName))
            .setCautionAction(getString(R.string.common_deletion_dialog_yes)) {
                viewModel.deleteTag(tagName)
                showShortToast(R.string.common_deletion_dialog_confirmation, tagName)
            }
            .setDismissText(getString(R.string.common_deletion_dialog_no))
            .show()
    }

    private fun showTagEditSheet(tagName: String) {
        HexTagEditBottomSheet.show(supportFragmentManager, tagName) { newName ->
            viewModel.renameTag(tagName, newName)
        }
    }
}
