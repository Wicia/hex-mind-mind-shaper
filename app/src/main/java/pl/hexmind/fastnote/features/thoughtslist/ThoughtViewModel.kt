package pl.hexmind.fastnote.features.thoughtslist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import pl.hexmind.fastnote.data.AppDatabase
import pl.hexmind.fastnote.data.models.AreaIdentifier
import pl.hexmind.fastnote.data.models.relations.ThoughtWithArea
import pl.hexmind.fastnote.data.repositories.ThoughtsRepository
import kotlinx.coroutines.launch

class ThoughtViewModel (application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repo = ThoughtsRepository(db.thoughtsDao(), db.areasDAO())

    val thoughts: LiveData<List<ThoughtWithArea>> = repo.getAllThoughtsWithArea()

    fun addThought(essence: String,
                   areaIdentifier: AreaIdentifier,
                   priority: Int = 3,
                   thread: String?) {
        viewModelScope.launch {
            repo.createThoughtWithExistingArea(essence, areaIdentifier, priority, thread)
        }
    }
}