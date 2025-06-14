package com.example.fastnote.db

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.fastnote.db.repositories.ThoughtsRepository
import kotlinx.coroutines.launch

class ThoughtViewModel (application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repo = ThoughtsRepository(db.thoughtsDao())

    val thoughts: LiveData<List<ThoughtEntity>> = repo.getThoughts()

    fun addThought(thought: ThoughtEntity) {
        viewModelScope.launch {
            repo.addThought(thought)
        }
    }
}