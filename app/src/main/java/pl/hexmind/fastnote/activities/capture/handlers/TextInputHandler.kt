package pl.hexmind.fastnote.activities.capture.handlers

import android.text.TextWatcher
import pl.hexmind.fastnote.activities.capture.models.CapturedThought
import pl.hexmind.fastnote.activities.capture.ui.CaptureViewManager

// Handler for business logic related to RICH TEXT - thought type
class TextInputHandler(private val viewManager: CaptureViewManager) {
    
    private var onDataChangedListener: ((CapturedThought) -> Unit)? = null
    
    fun setupTextWatcher() {
        viewManager.editThoughtEssence.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                updateEssenceInfo()
                notifyDataChanged()
            }
        })
    }
    
    private fun updateEssenceInfo() {
        val text = viewManager.editThoughtEssence.text.toString().trim()
        viewManager.updateWordsCounterTextView(text)
    }
    
    fun getCurrentData(): CapturedThought {
        val essence = viewManager.editThoughtEssence.text.toString().trim()
        val richText = viewManager.etRichText.text.toString().trim()
        
        return CapturedThought(
            essence = essence,
            richText = richText
        )
    }
    
    fun setOnDataChangedListener(listener: (CapturedThought) -> Unit) {
        onDataChangedListener = listener
    }
    
    private fun notifyDataChanged() {
        onDataChangedListener?.invoke(getCurrentData())
    }
    
    fun setupRichTextEditor() {
        // TODO: Add formatting buttons and rich text functionality
    }
}