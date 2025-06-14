package com.example.fastnote.features.thoughtslist

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.lifecycle.ViewModelProvider
import com.example.fastnote.databinding.ThoughtListLayoutBinding
import com.example.fastnote.data.models.ThoughtEntity

class ThoughtsListActivity : AppCompatActivity() {

    private lateinit var adapter: ThoughtsListAdapter
    private lateinit var viewModel: ThoughtViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ThoughtListLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = ThoughtsListAdapter(emptyList())
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        viewModel = ViewModelProvider(this)[ThoughtViewModel::class.java]

        // ! applying observing
        viewModel.thoughts.observe(this) { thoughts -> adapter.updateData(thoughts) }

        viewModel.addThought(ThoughtEntity(area = "test", content = "Myśl3", priority = 2))
    }
}