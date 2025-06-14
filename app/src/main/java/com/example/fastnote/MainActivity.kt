package com.example.fastnote

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.ViewModelProvider
import com.example.fastnote.databinding.ActivityMainBinding
import com.example.fastnote.db.ThoughtEntity
import com.example.fastnote.db.ThoughtViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: ThoughtsListAdapter
    private lateinit var viewModel: ThoughtViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = ThoughtsListAdapter(emptyList())
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        viewModel = ViewModelProvider(this)[ThoughtViewModel::class.java]

        //viewModel.addThought(ThoughtEntity(area = "test", content = "Myśl2", priority = 2))

        // ! applying observing
        viewModel.thoughts.observe(this) { thoughts -> adapter.updateData(thoughts) }
    }
}