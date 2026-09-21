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
import pl.hexmind.mindshaper.services.RenameOutcome

@AndroidEntryPoint
class MetadataActivity : CoreActivity() {

    private val viewModel: MetadataViewModel by viewModels()

    private lateinit var rvPersonTags: RecyclerView
    private lateinit var tvEmpty: TextView

    private val tagsAdapter = MetadataTagRowAdapter { tagName -> showTagEditSheet(tagName) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.metadata_activity)

        // Nav overlay + current-screen highlight are added by CoreActivity (onContentChanged / onResume)
        setupHeader(R.drawable.ic_activity_metadata, R.string.metadata_title)

        rvPersonTags = findViewById(R.id.rv_person_tags)
        tvEmpty = findViewById(R.id.tv_person_tags_empty)

        rvPersonTags.layoutManager = LinearLayoutManager(this)
        rvPersonTags.adapter = tagsAdapter

        observeViewModel()
    }

    // Reloaded here, not in onCreate: the nav reuses this instance (SINGLE_TOP), so a tag added
    // elsewhere would otherwise leave a stale list on return
    override fun onResume() {
        super.onResume()
        viewModel.loadPersonTags()
    }

    private fun observeViewModel() {
        viewModel.personTagRows.observe(this) { rows ->
            tagsAdapter.submitRows(rows)

            val hasTags = rows.isNotEmpty()
            rvPersonTags.visibility = if (hasTags) View.VISIBLE else View.GONE
            tvEmpty.visibility      = if (hasTags) View.GONE else View.VISIBLE
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

    private fun showTagEditSheet(tagName: String) {
        HexTagEditBottomSheet.show(supportFragmentManager, tagName) { newName ->
            viewModel.renameTag(tagName, newName)
        }
    }
}
