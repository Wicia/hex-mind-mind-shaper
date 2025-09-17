package pl.hexmind.mindshaper.activities.capture.handlers


import pl.hexmind.mindshaper.activities.capture.ui.NoteCaptureView
import pl.hexmind.mindshaper.services.dto.ThoughtDTO

class NoteInputHandler(
    private val noteCaptureView: NoteCaptureView,
    private val validator: ThoughtValidator
) {

    private var onDataChangedListener: ((ThoughtDTO) -> Unit)? = null

    fun getCurrentData(): ThoughtDTO {
        val richText = noteCaptureView.getRichText().trim()
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