package pl.hexmind.fastnote.features.capture.models

import java.io.File

// Central DTO for transferring data between UI laver and business layer
data class CaptureData(
    val essence: String = "",
    val richText: String = "",
    val audioFile: File? = null,
    val photoFile: File? = null,
    val drawingData: String? = null, // JSON or base64 for future
    val inputType: String = ""
) {
    fun isValid(): Boolean {
        return essence.isNotEmpty() && essence.split("\\s+".toRegex()).size <= 10
    }
    
    fun getWordCount(): Int {
        return if (essence.isEmpty()) 0 else essence.split("\\s+".toRegex()).size
    }
}