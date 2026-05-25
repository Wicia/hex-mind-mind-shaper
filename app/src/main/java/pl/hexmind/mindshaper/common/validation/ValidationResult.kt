package pl.hexmind.mindshaper.common.validation

import android.content.Context
import androidx.annotation.StringRes

sealed class ValidationResult {
    data class Valid(
        val message: String? = null
    ) : ValidationResult()

    data class Error(
        @param:StringRes val messageStringId: Int,
        val valueToInject : String? = null,
        val refProperty : ValidatedProperty? = null
    ) : ValidationResult()
}

fun ValidationResult.Error.resolveMessage(context: Context): String {
    return if (valueToInject != null)
        context.getString(messageStringId, valueToInject)
    else
        context.getString(messageStringId)
}

enum class ValidatedProperty {
    T_SUBJECT,
    T_PROJECT,
    T_SOUL_MATES,
    T_RICH_TEXT
}