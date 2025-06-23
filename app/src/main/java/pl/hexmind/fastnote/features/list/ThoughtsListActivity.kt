package pl.hexmind.fastnote.features.list

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import pl.hexmind.fastnote.R
import pl.hexmind.fastnote.databinding.ThoughtListLayoutBinding
import pl.hexmind.fastnote.features.details.DetailsActivity

class ThoughtsListActivity : AppCompatActivity() {

    private lateinit var binding: ThoughtListLayoutBinding
    private lateinit var adapter: ThoughtsListAdapter
    private lateinit var viewModel: ThoughtViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeBinding()
        initializeComponents()
    }

    private fun initializeBinding(){
        binding = ThoughtListLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    private fun initializeComponents() {
        initListView()
        initFloatingButton()
    }

    private fun initListView(){
        adapter = ThoughtsListAdapter(emptyList())
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        viewModel = ViewModelProvider(this)[ThoughtViewModel::class.java]

        // ! applying observing
        viewModel.thoughts.observe(this) { thoughts -> adapter.updateData(thoughts) }
    }

    private fun initFloatingButton(){
        val fab = findViewById<FloatingActionButton>(R.id.fab)
        fab.setOnClickListener(::onAddButtonClick)
    }

    private fun onAddButtonClick(view: View){
        val intent = Intent(this, DetailsActivity::class.java)
        startActivity(intent)
    }
}