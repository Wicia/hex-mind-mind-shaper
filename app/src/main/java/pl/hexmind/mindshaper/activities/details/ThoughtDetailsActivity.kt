package pl.hexmind.mindshaper.activities.details

import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import pl.hexmind.mindshaper.activities.CommonTextEditDialog
import pl.hexmind.mindshaper.activities.CoreActivity
import pl.hexmind.mindshaper.databinding.ActivityThoughtDetailsBinding
import pl.hexmind.mindshaper.services.ThoughtsService
import pl.hexmind.mindshaper.services.dto.ThoughtDTO
import javax.inject.Inject

@AndroidEntryPoint
class ThoughtDetailsActivity : CoreActivity() {

    @Inject
    lateinit var service : ThoughtsService

    private lateinit var binding: ActivityThoughtDetailsBinding

    // Displayed details data
    private var dtoWithDetails : ThoughtDTO? = null


    companion object PARAMS {
        const val P_SELECTED_THOUGHT_ID = "P_SELECTED_THOUGHT_ID"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityThoughtDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        saveExtrasFromIntent()
        initializeListeners()
        fillWithDetails()
    }

    private fun initializeListeners(){
        binding.tvRichText.setOnClickListener {
            showEditTextDialog(binding.tvRichText)
        }
        binding.tvThread.setOnClickListener {
            showEditTextDialog(binding.tvThread)
        }
        binding.tvEssence.setOnClickListener {
            showEditTextDialog(binding.tvEssence)
        }
        // Save settings button
        binding.btnSave.setOnClickListener {
            saveThought()
        }
    }

    fun saveThought(){
        val dto = dtoWithDetails ?: return

        dto.thread = binding.tvThread.text.toString()
        dto.essence = binding.tvEssence.text.toString()
        dto.richText = binding.tvRichText.text.toString()

        lifecycleScope.launch {
            service.updateThought(dto)
        }
    }

    /**
     * Shows dialog for editing essence with validation
     */
    private fun showEditTextDialog(textViewToBind : TextView) {
        CommonTextEditDialog(
            context = this,
            textInput = textViewToBind.text.toString(),
            onSave = { newText ->
                // Update UI
                textViewToBind.text = newText
            }
        ).show()
    }

    private fun saveExtrasFromIntent() {
        dtoWithDetails = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(P_SELECTED_THOUGHT_ID, ThoughtDTO::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(P_SELECTED_THOUGHT_ID) as? ThoughtDTO
        }
    }

    private fun fillWithDetails(){
        binding.tvEssence.text = dtoWithDetails?.essence

        if(dtoWithDetails?.richText.isNullOrBlank()){
            binding.btnRichTextPlaceholder.visibility = View.VISIBLE
            binding.tvRichText.visibility = View.GONE
        }
        else{
            binding.btnRichTextPlaceholder.visibility = View.GONE
            binding.tvRichText.visibility = View.VISIBLE
            binding.tvRichText.text = dtoWithDetails?.richText
        }

        if(dtoWithDetails?.thread.isNullOrBlank()){
            binding.btnThreadPlaceholder.visibility = View.VISIBLE
            binding.tvThread.visibility = View.GONE
        }
        else{
            binding.btnThreadPlaceholder.visibility = View.GONE
            binding.tvThread.visibility = View.VISIBLE
            binding.tvThread.text = dtoWithDetails?.thread
        }
    }
}