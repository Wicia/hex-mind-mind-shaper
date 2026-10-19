package pl.hexmind.mindshaper.common.ui

import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import pl.hexmind.mindshaper.R

/**
 * Picks a plain text file from the phone and hands over its content.
 * ! Create it as an Activity property - the result launcher has to be registered before the Activity starts
 */
class TextFilePicker(private val activity: ComponentActivity) {

    private var onRead: ((String) -> Unit)? = null

    private val launcher = activity.registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val callback = onRead
        onRead = null
        if (uri == null || callback == null) return@registerForActivityResult // Picker closed without a file

        val text = runCatching {
            activity.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader -> reader.readText() }
        }.getOrNull()

        if (text == null) {
            Toast.makeText(activity, R.string.files_text_error_reading, Toast.LENGTH_SHORT).show()
        }
        else {
            // ! Notepad saves UTF-8 with a BOM - it would stick to the first flashcard's front
            callback(text.removePrefix(BYTE_ORDER_MARK))
        }
    }

    fun pick(onRead: (String) -> Unit) {
        this.onRead = onRead
        launcher.launch(arrayOf(MIME_TEXT))
    }

    private companion object {
        const val MIME_TEXT = "text/plain"
        const val BYTE_ORDER_MARK = "﻿"
    }
}
