package pl.hexmind.mindshaper.activities.capture.handlers

import android.content.Context
import android.text.method.ScrollingMovementMethod
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.common.validation.ValidationResult

class RichTextCaptureView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    val etRichText: EditText

    private val tvValidationInfo : TextView

    init {
        inflate(context, R.layout.capture_view_rich_text, this)
        orientation = VERTICAL
        etRichText = findViewById(R.id.et_rich_notes)
        etRichText.movementMethod = ScrollingMovementMethod.getInstance()

        // ! Block swiping on parent view/activity = enables scrolling on this view
        etRichText.setOnTouchListener { view, event ->
            view.parent.requestDisallowInterceptTouchEvent(true)

            when (event.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_UP -> {
                    view.parent.requestDisallowInterceptTouchEvent(false)
                }
            }
            false
        }

        tvValidationInfo = findViewById(R.id.tv_hex_tags_validation_info)
    }

    fun updateValidationInfo(result : ValidationResult){
        when(result){
            is ValidationResult.Error -> {
                tvValidationInfo.text = result.message
            }
            is ValidationResult.Valid -> {
                tvValidationInfo.text = context.getString(R.string.common_validation_info_initial)
            }
        }
    }
}