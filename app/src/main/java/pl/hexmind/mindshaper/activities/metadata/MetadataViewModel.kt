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

    private val _personTagRows = MutableLiveData<List<List<HexTagUsage>>>()
    val personTagRows: LiveData<List<List<HexTagUsage>>> = _personTagRows

    private val _renameResult = MutableLiveData<RenameOutcome?>()
    val renameResult: LiveData<RenameOutcome?> = _renameResult

    fun loadPersonTags() {
        viewModelScope.launch {
            val personTags = hexTagsService.getTagsWithUsage(HexTagType.PERSON)
            // Max 2 per row - an odd count simply leaves the last row with one pill
            _personTagRows.value = personTags.chunked(ROW_MAX)
        }
    }

    fun renameTag(currentName: String, newName: String) {
        viewModelScope.launch {
            val outcome = hexTagsService.renameTag(HexTagType.PERSON, currentName, newName)
            if (outcome == RenameOutcome.RENAMED) {
                _personTagRows.value = hexTagsService.getTagsWithUsage(HexTagType.PERSON).chunked(ROW_MAX)
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
