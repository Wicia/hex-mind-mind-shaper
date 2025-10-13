package pl.hexmind.mindshaper.activities.capture.handlers

import android.content.Context
import android.text.Editable
import android.util.AttributeSet
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import pl.hexmind.mindshaper.R
import pl.hexmind.mindshaper.activities.CommonTextEditDialog
import pl.hexmind.mindshaper.common.validation.ValidationResult

class RichTextCaptureView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    val etRichText: EditText

    private val tvValidationInfo : TextView

    init {
        inflate(context, R.layout.view_rich_text_capture, this)
        orientation = VERTICAL
        etRichText = findViewById(R.id.et_rich_notes)

        tvValidationInfo = findViewById(R.id.tv_validation_info)
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