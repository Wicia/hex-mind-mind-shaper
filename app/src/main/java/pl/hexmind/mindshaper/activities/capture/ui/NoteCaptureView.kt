package pl.hexmind.mindshaper.activities.capture.ui

import android.content.Context
import android.text.Editable
import android.util.AttributeSet
import android.widget.EditText
import android.widget.LinearLayout
import pl.hexmind.mindshaper.R

class NoteCaptureView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val etRichText: EditText

    init {
        inflate(context, R.layout.view_note_capture, this)
        orientation = VERTICAL
        etRichText = findViewById(R.id.et_rich_notes)
    }

    fun getRichText(): String = etRichText.text.toString()

    fun updateRichText(value : String) {
        etRichText.text = Editable.Factory.getInstance().newEditable(value)
    }
}