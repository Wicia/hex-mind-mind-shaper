package pl.hexmind.mindshaper.activities.metadata

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import pl.hexmind.mindshaper.database.models.HexTagType
import pl.hexmind.mindshaper.database.models.HexTagUsage
import pl.hexmind.mindshaper.services.HexTagsService
import pl.hexmind.mindshaper.services.RenameOutcome
import javax.inject.Inject

@HiltViewModel
class MetadataViewModel @Inject constructor(
    private val hexTagsService: HexTagsService
) : ViewModel() {

    private val _tagRows = MutableLiveData<List<List<HexTagUsage>>>()
    val tagRows: LiveData<List<List<HexTagUsage>>> = _tagRows

    private val _renameResult = MutableLiveData<RenameOutcome?>()
    val renameResult: LiveData<RenameOutcome?> = _renameResult

    // holds the viewed list so nav return and rename target it
    var currentType = HexTagType.PERSON
        private set

    fun loadTags(tagType: HexTagType) {
        currentType = tagType
        viewModelScope.launch {
            val tags = hexTagsService.getTagsWithUsage(tagType)
            // Max 2 per row - an odd count simply leaves the last row with one pill
            _tagRows.value = tags.chunked(ROW_MAX)
        }
    }

    fun renameTag(currentName: String, newName: String) {
        viewModelScope.launch {
            val outcome = hexTagsService.renameTag(currentType, currentName, newName)
            if (outcome == RenameOutcome.RENAMED) {
                _tagRows.value = hexTagsService.getTagsWithUsage(currentType).chunked(ROW_MAX)
            }

            _renameResult.value = outcome
        }
    }

    fun onRenameResultShown() {
        _renameResult.value = null
    }

    private companion object {
        const val ROW_MAX = 2
    }
}
