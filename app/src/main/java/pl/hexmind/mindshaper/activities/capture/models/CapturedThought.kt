package pl.hexmind.mindshaper.activities.capture.models

import java.io.File

// Central DTO for transferring data between UI laver and business layer
data class CapturedThought(
    val essence: String = "",
    val richText: String = "",
    val audioFile: File? = null,
    val photoFile: File? = null,
    val drawingData: String? = null, // JSON or base64 for future
    val initialThoughtType: InitialThoughtType = InitialThoughtType.UNKNOWN
)