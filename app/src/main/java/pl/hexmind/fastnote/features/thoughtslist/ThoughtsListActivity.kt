package pl.hexmind.fastnote.features.thoughtslist

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.lifecycle.ViewModelProvider
import pl.hexmind.fastnote.data.models.AreaIdentifier
import pl.hexmind.fastnote.databinding.ThoughtListLayoutBinding

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

        viewModel.addThought(essence = "Myśl3", areaIdentifier = AreaIdentifier.AREA_1, thread = null, priority = 2)
    }
}