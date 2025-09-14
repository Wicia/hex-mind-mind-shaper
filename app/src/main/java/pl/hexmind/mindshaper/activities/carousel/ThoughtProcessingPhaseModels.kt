package pl.hexmind.mindshaper.activities.carousel

/**
 * Storing data of current thoughts processing phase
 */
data class ThoughtProcessingPhase(
    val currentPhaseName: ThoughtProcessingPhaseName,
    var minRemaining: Int
){
    private val currentPhaseToNextPhaseMap = mapOf(
        ThoughtProcessingPhaseName.GATHERING to ThoughtProcessingPhaseName.CHOOSING,
        ThoughtProcessingPhaseName.CHOOSING to ThoughtProcessingPhaseName.SILENT,
        ThoughtProcessingPhaseName.SILENT to ThoughtProcessingPhaseName.GATHERING
    )

    fun getNextPhaseName() : ThoughtProcessingPhaseName {
        return currentPhaseToNextPhaseMap[currentPhaseName] ?: ThoughtProcessingPhaseName.ERROR
    }
}

enum class ThoughtProcessingPhaseName {
    GATHERING, // When you can gather and edit
    CHOOSING,  // When you can decide what to do with thoughts + can't edit these
    SILENT,    // When you can't add new thoughts
    ERROR      // To capture missing requirements in code
}