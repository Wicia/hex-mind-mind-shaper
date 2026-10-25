package pl.hexmind.mindshaper.activities.flashcards

import android.content.Context
import androidx.annotation.RawRes
import pl.hexmind.mindshaper.R

/**
 * Ready-made flashcards shipped with the app: res/raw/flashcards_bank_*.txt, in the import text format - always with the dash separator.
 * Shown name comes from the file name - "flashcards_bank_stolice_europy" -> "Stolice europy".
 * ! Found by the name prefix (reflection) - kept in release by res/raw/keep.xml + proguard-rules.pro
 */
object FlashcardsBank {

    data class Entry(
        val name: String,
        @param:RawRes val resId: Int
    )

    private const val PREFIX = "flashcards_bank_"
    private const val BYTE_ORDER_MARK = "﻿"

    fun entries(): List<Entry> =
        R.raw::class.java.fields
            .filter { field -> field.name.startsWith(PREFIX) }
            .map { field -> Entry(name = displayName(field.name), resId = field.getInt(null)) }
            .sortedBy { entry -> entry.name }

    fun read(context: Context, entry: Entry): String =
        context.resources.openRawResource(entry.resId).bufferedReader().use { reader -> reader.readText() }
            .removePrefix(BYTE_ORDER_MARK)

    private fun displayName(resourceName: String): String =
        resourceName.removePrefix(PREFIX)
            .replace('_', ' ')
            .replaceFirstChar { char -> char.titlecase() }
}
