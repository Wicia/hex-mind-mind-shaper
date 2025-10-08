package pl.hexmind.mindshaper.activities.capture.handlers


import pl.hexmind.mindshaper.activities.ThoughtValidator
import pl.hexmind.mindshaper.activities.capture.ui.RichTextCaptureView
import pl.hexmind.mindshaper.services.dto.ThoughtDTO

class RichTextHandler(
    private val richTextCaptureView: RichTextCaptureView,
    private val validator: ThoughtValidator
) {

    private var onDataChangedListener: ((ThoughtDTO) -> Unit)? = null

    fun getCurrentData(): ThoughtDTO {
        val richText = richTextCaptureView.getRichText().trim()
        return ThoughtDTO(
            richText = richText
        )
    }

    fun setOnDataChangedListener(listener: (ThoughtDTO) -> Unit) {
        onDataChangedListener = listener
    }

    private fun notifyDataChanged() {
        onDataChangedListener?.invoke(getCurrentData())
    }
}